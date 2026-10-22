package com.nhom01.imagerec.client.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoryStoreTest {

    private static DetectionResult resultWith(int objects) {
        Detection[] detections = new Detection[objects];
        for (int i = 0; i < objects; i++) {
            detections[i] = new Detection("obj" + i, 0.9, 0.1, 0.1, 0.2, 0.2);
        }
        return new DetectionResult(10, List.of(detections));
    }

    private static BufferedImage image() {
        return new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void emptyOrMissingDirectoryListsNothing(@TempDir Path dir) throws IOException {
        assertThat(new HistoryStore(dir).list()).isEmpty();
        assertThat(new HistoryStore(dir.resolve("chua-co")).list()).isEmpty();
    }

    @Test
    void savedEntriesListNewestFirstAndSurviveReopen(@TempDir Path dir) throws IOException {
        HistoryStore store = new HistoryStore(dir);
        store.save("a.jpg", image(), resultWith(1));
        store.save("b.jpg", image(), resultWith(2));
        store.save("c.jpg", image(), resultWith(3));

        for (HistoryStore opened : List.of(store, new HistoryStore(dir))) {
            List<HistoryEntry> entries = opened.list();
            assertThat(entries).extracting(HistoryEntry::fileName).containsExactly("c.jpg", "b.jpg", "a.jpg");
            assertThat(entries).extracting(e -> e.result().detections().size()).containsExactly(3, 2, 1);
            assertThat(entries).allSatisfy(e -> assertThat(e.annotatedImage()).exists());
            assertThat(entries.get(0).result().processingMillis()).isEqualTo(10);
        }
    }

    @Test
    void garbageLineInIndexIsSkipped(@TempDir Path dir) throws IOException {
        HistoryStore store = new HistoryStore(dir);
        store.save("a.jpg", image(), resultWith(1));
        Files.writeString(dir.resolve("index.tsv"), "rac rac khong phai dong hop le\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        store.save("b.jpg", image(), resultWith(1));

        assertThat(new HistoryStore(dir).list()).extracting(HistoryEntry::fileName).containsExactly("b.jpg", "a.jpg");
    }

    @Test
    void entryWithMissingFilesIsSkipped(@TempDir Path dir) throws IOException {
        HistoryStore store = new HistoryStore(dir);
        HistoryEntry gone = store.save("gone.jpg", image(), resultWith(1));
        store.save("kept.jpg", image(), resultWith(1));
        Files.delete(gone.annotatedImage());

        assertThat(store.list()).extracting(HistoryEntry::fileName).containsExactly("kept.jpg");
    }

    @Test
    void fileNameWithTabAndNewlineIsSanitized(@TempDir Path dir) throws IOException {
        HistoryStore store = new HistoryStore(dir);
        store.save("x\ty\nz.jpg", image(), resultWith(1));

        assertThat(store.list()).extracting(HistoryEntry::fileName).containsExactly("x y z.jpg");
    }

    @Test
    void twoSavesInSameMillisecondGetDistinctIds(@TempDir Path dir) throws IOException {
        HistoryStore store = new HistoryStore(dir);
        HistoryEntry first = store.save("a.jpg", image(), resultWith(1));
        HistoryEntry second = store.save("a.jpg", image(), resultWith(1));
        HistoryEntry third = store.save("a.jpg", image(), resultWith(1));

        assertThat(List.of(first.annotatedImage(), second.annotatedImage(), third.annotatedImage())).doesNotHaveDuplicates();
        assertThat(store.list()).hasSize(3);
    }
}
