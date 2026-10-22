package com.nhom01.imagerec.server.detect;

import com.nhom01.imagerec.common.Detection;
import java.awt.image.BufferedImage;
import java.util.List;

/** Detector giả trả về 2 đối tượng cố định, dùng cho test và khi chưa có mô hình thật. */
public final class FakeObjectDetector implements ObjectDetector {

    public static final List<Detection> FIXED_DETECTIONS = List.of(
            new Detection("person", 0.90, 0.10, 0.10, 0.30, 0.60),
            new Detection("dog", 0.75, 0.55, 0.50, 0.35, 0.40));

    @Override
    public List<Detection> detect(BufferedImage image) {
        return FIXED_DETECTIONS;
    }

    @Override
    public void close() {
    }
}
