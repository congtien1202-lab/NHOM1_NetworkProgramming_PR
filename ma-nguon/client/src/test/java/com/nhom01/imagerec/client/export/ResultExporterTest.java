package com.nhom01.imagerec.client.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResultExporterTest {

    private static final DetectionResult TWO_OBJECTS = new DetectionResult(42, List.of(
            new Detection("person", 0.92, 0.1, 0.2, 0.3, 0.4),
            new Detection("car", 0.5, 0.55, 0.6, 0.25, 0.3)));

    private Locale originalLocale;

    @BeforeEach
    void useVietnameseLocale() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(new Locale("vi", "VN"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void csvHasBomHeaderAndDotDecimalsEvenInVietnameseLocale(@TempDir Path dir) throws IOException {
        File out = dir.resolve("r.csv").toFile();

        ResultExporter.exportCsv(TWO_OBJECTS, out);

        byte[] bytes = Files.readAllBytes(out.toPath());
        assertThat(bytes).startsWith(0xEF, 0xBB, 0xBF);
        List<String> lines = Files.readAllLines(out.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).isEqualTo("﻿label,confidence,x,y,width,height");
        assertThat(lines.get(1)).isEqualTo("person,0.92,0.1,0.2,0.3,0.4");
    }

    @Test
    void labelWithCommaAndQuoteIsEscaped(@TempDir Path dir) throws IOException {
        DetectionResult tricky = new DetectionResult(1,
                List.of(new Detection("a,\"b\"", 0.5, 0.1, 0.1, 0.1, 0.1)));
        File out = dir.resolve("t.csv").toFile();

        ResultExporter.exportCsv(tricky, out);

        assertThat(ResultExporter.readCsv(out.toPath(), 1)).isEqualTo(tricky);
    }

    @Test
    void exportImageWritesReadablePngWithSameSize(@TempDir Path dir) throws IOException {
        File out = dir.resolve("a.png").toFile();

        ResultExporter.exportImage(new BufferedImage(30, 20, BufferedImage.TYPE_INT_RGB), out);

        BufferedImage back = ImageIO.read(out);
        assertThat(back.getWidth()).isEqualTo(30);
        assertThat(back.getHeight()).isEqualTo(20);
    }

    @Test
    void readCsvRoundTripsExportedResult(@TempDir Path dir) throws IOException {
        File out = dir.resolve("r.csv").toFile();
        ResultExporter.exportCsv(TWO_OBJECTS, out);

        assertThat(ResultExporter.readCsv(out.toPath(), 42)).isEqualTo(TWO_OBJECTS);
    }

    @Test
    void emptyResultRoundTrips(@TempDir Path dir) throws IOException {
        File out = dir.resolve("e.csv").toFile();
        DetectionResult empty = new DetectionResult(7, List.of());
        ResultExporter.exportCsv(empty, out);

        assertThat(ResultExporter.readCsv(out.toPath(), 7)).isEqualTo(empty);
    }
}
