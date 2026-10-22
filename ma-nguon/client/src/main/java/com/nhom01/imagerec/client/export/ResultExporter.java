package com.nhom01.imagerec.client.export;

import com.nhom01.imagerec.common.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Stub xuất kết quả ảnh và dữ liệu (sẽ được cài đặt trong PLAN-D / Issue 8).
 */
public final class ResultExporter {

    private ResultExporter() {
    }

    public static void exportImage(BufferedImage image, File targetFile) throws IOException {
        throw new UnsupportedOperationException("PLAN-D");
    }

    public static void exportCsv(DetectionResult result, File targetFile) throws IOException {
        throw new UnsupportedOperationException("PLAN-D");
    }
}
