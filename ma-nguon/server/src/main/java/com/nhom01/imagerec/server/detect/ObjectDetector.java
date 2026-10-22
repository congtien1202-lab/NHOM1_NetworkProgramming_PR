package com.nhom01.imagerec.server.detect;

import com.nhom01.imagerec.common.Detection;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Seam giữa phần mạng của Server (PLAN-A) và mô hình nhận dạng (PLAN-B).
 *
 * <p>Hợp đồng:
 * <ul>
 *   <li>Implementation PHẢI thread-safe: nhiều {@code ClientHandler} gọi {@link #detect} đồng thời.</li>
 *   <li>Tọa độ trong {@link Detection} là tỉ lệ 0..1 so với kích thước ảnh đầu vào.</li>
 *   <li>Lỗi mô hình ném {@link DetectionException}; Server chuyển thành ERROR DETECTION_FAILED.</li>
 *   <li>Mô hình nạp một lần khi khởi tạo; {@link #close} giải phóng tài nguyên.</li>
 * </ul>
 */
public interface ObjectDetector extends AutoCloseable {

    List<Detection> detect(BufferedImage image) throws DetectionException;

    @Override
    void close();
}
