package com.nhom01.imagerec.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nhom01.imagerec.common.Protocol;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageFilesTest {

    @TempDir
    Path dir;

    @Test
    void acceptsValidPng() throws IOException {
        File png = writePng("anh.png");

        assertEquals(Optional.empty(), ImageFiles.validate(png));
    }

    @Test
    void extensionIsCaseInsensitive() throws IOException {
        File png = writePng("ANH.PNG");

        assertEquals(Optional.empty(), ImageFiles.validate(png));
    }

    @Test
    void rejectsGif() throws IOException {
        File gif = Files.write(dir.resolve("a.gif"), new byte[] {1, 2, 3}).toFile();

        assertTrue(ImageFiles.validate(gif).isPresent());
    }

    @Test
    void rejectsFileOverMaxSize() throws IOException {
        File big = dir.resolve("big.jpg").toFile();
        try (RandomAccessFile file = new RandomAccessFile(big, "rw")) {
            file.setLength(Protocol.MAX_IMAGE_BYTES + 1L);
        }

        assertTrue(ImageFiles.validate(big).isPresent());
    }

    @Test
    void rejectsGarbageWithImageExtension() throws IOException {
        File fake = Files.write(dir.resolve("fake.jpg"), "khong phai anh".getBytes()).toFile();

        assertTrue(ImageFiles.validate(fake).isPresent());
    }

    @Test
    void rejectsMissingFile() {
        assertTrue(ImageFiles.validate(dir.resolve("none.png").toFile()).isPresent());
    }

    private File writePng(String name) throws IOException {
        File file = dir.resolve(name).toFile();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", file);
        return file;
    }
}
