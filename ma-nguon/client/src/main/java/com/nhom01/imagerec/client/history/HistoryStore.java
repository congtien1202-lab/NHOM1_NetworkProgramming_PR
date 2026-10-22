package com.nhom01.imagerec.client.history;

import com.nhom01.imagerec.common.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Stub kho lưu trữ lịch sử nhận dạng (sẽ được cài đặt trong PLAN-D / Issue 9).
 */
public class HistoryStore {

    private final Path root;

    public HistoryStore(Path root) {
        this.root = root;
    }

    public static Path defaultRoot() {
        return Paths.get(System.getProperty("user.home"), ".image-recognition", "history");
    }

    public synchronized HistoryEntry save(String originalFileName, BufferedImage annotatedImage, DetectionResult result)
            throws IOException {
        throw new UnsupportedOperationException("PLAN-D");
    }

    public synchronized List<HistoryEntry> list() throws IOException {
        throw new UnsupportedOperationException("PLAN-D");
    }

    public Path root() {
        return root;
    }
}
