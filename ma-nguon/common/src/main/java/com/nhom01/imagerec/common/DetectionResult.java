package com.nhom01.imagerec.common;

import java.util.List;

/**
 * Kết quả nhận dạng một ảnh.
 *
 * @param processingMillis thời gian Server xử lý ảnh (ms)
 * @param detections       các đối tượng phát hiện được, có thể rỗng
 */
public record DetectionResult(long processingMillis, List<Detection> detections) {

    public DetectionResult {
        detections = List.copyOf(detections);
    }
}
