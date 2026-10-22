package com.nhom01.imagerec.client.history;

import com.nhom01.imagerec.common.DetectionResult;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Một mục trong lịch sử nhận dạng.
 */
public record HistoryEntry(
        String id,
        Instant timestamp,
        String originalFileName,
        DetectionResult result,
        Path annotatedImagePath) {
}
