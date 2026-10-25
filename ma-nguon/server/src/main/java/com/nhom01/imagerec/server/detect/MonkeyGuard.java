package com.nhom01.imagerec.server.detect;

import com.nhom01.imagerec.common.Detection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lớp bảo vệ chống nhận người thành khỉ, chạy sau NMS và chỉ được phép bỏ box {@code monkey}.
 *
 * <p>Luật: (1) bỏ box {@code monkey} có độ tin cậy dưới {@link #MIN_MONKEY_CONFIDENCE}; (2) nếu còn box
 * {@code monkey}, lấy kết quả của YOLOv8s gốc 80 lớp và bỏ box {@code monkey} nào có box {@code person}
 * (độ tin cậy từ {@link #GUARD_MIN_CONFIDENCE}) với IoU từ {@link #VETO_IOU}. Bước 2 chỉ chạy khi còn khỉ,
 * nên ảnh không có khỉ không tốn thêm một lần suy luận. Tọa độ là tỉ lệ 0..1 theo ảnh gốc.
 */
final class MonkeyGuard {

    static final String MONKEY_LABEL = "monkey";
    static final String PERSON_LABEL = "person";
    static final double MIN_MONKEY_CONFIDENCE = 0.5;
    static final double GUARD_MIN_CONFIDENCE = 0.2;
    static final double VETO_IOU = 0.5;

    private MonkeyGuard() {
    }

    /**
     * @param detections kết quả của mô hình chính (đã qua ngưỡng và NMS)
     * @param guardModel gọi mô hình gốc 80 lớp (ngưỡng {@link #GUARD_MIN_CONFIDENCE}); chỉ được gọi khi cần,
     *                   {@code null} nghĩa là chỉ áp dụng bước 1
     */
    static List<Detection> apply(List<Detection> detections, Supplier<List<Detection>> guardModel) {
        List<Detection> kept = new ArrayList<>(detections.size());
        List<Detection> monkeys = new ArrayList<>();
        for (Detection d : detections) {
            if (!MONKEY_LABEL.equals(d.label())) {
                kept.add(d);
            } else if (d.confidence() >= MIN_MONKEY_CONFIDENCE) {
                monkeys.add(d);
            }
        }
        if (monkeys.isEmpty()) {
            return kept;
        }
        List<Detection> people = guardModel == null ? List.of() : guardModel.get().stream()
                .filter(p -> PERSON_LABEL.equals(p.label()) && p.confidence() >= GUARD_MIN_CONFIDENCE)
                .toList();
        for (Detection monkey : monkeys) {
            if (people.stream().noneMatch(p -> iou(monkey, p) >= VETO_IOU)) {
                kept.add(monkey);
            }
        }
        kept.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return kept;
    }

    static double iou(Detection a, Detection b) {
        double ix = Math.min(a.x() + a.width(), b.x() + b.width()) - Math.max(a.x(), b.x());
        double iy = Math.min(a.y() + a.height(), b.y() + b.height()) - Math.max(a.y(), b.y());
        if (ix <= 0 || iy <= 0) {
            return 0;
        }
        double inter = ix * iy;
        return inter / (a.width() * a.height() + b.width() * b.height() - inter);
    }
}
