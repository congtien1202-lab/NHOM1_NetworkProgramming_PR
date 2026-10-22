package com.nhom01.imagerec.client.history;

import com.nhom01.imagerec.common.DetectionResult;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Một lần nhận dạng đã lưu trong lịch sử.
 *
 * @param annotatedImage file PNG đã vẽ bounding box
 */
public record HistoryEntry(Instant time, String fileName, Path annotatedImage, DetectionResult result) {
}
