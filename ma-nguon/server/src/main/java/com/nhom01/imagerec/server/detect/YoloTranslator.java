package com.nhom01.imagerec.server.detect;

import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.TranslateException;
import com.nhom01.imagerec.common.Detection;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tiền xử lý (letterbox 640x640) và hậu xử lý (lọc ngưỡng, NMS, quy đổi tọa độ) cho YOLOv8 xuất ONNX.
 *
 * <p>Đầu ra của mô hình có dạng {@code [1, 4 + số_lớp, 8400]}: bốn hàng đầu là tâm x, tâm y, rộng, cao
 * (theo pixel của ảnh 640x640 đã letterbox), các hàng sau là điểm của từng lớp (không có objectness).
 *
 * <p>Đối tượng này dùng chung giữa các Predictor nên không giữ trạng thái; kích thước ảnh gốc được
 * truyền qua {@link TranslatorContext}.
 */
final class YoloTranslator implements Translator<Image, List<Detection>> {

    static final int INPUT_SIZE = 640;
    static final double IOU_THRESHOLD = 0.45;
    private static final int BOX_VALUES = 4;
    private static final int MAX_DETECTIONS = 100;
    private static final int PAD_GRAY = 114;
    private static final float PIXEL_MAX = 255f;
    private static final String CTX_LETTERBOX = "letterbox";

    private final List<String> labels;
    private final double minConfidence;

    YoloTranslator(List<String> labels, double minConfidence) {
        this.labels = List.copyOf(labels);
        this.minConfidence = minConfidence;
    }

    /** Phép quy đổi từ ảnh gốc sang ảnh letterbox: {@code tọa_độ_letterbox = tọa_độ_gốc * scale + pad}. */
    record Letterbox(int width, int height, double scale, double padX, double padY) {
        static Letterbox of(int width, int height) {
            double scale = Math.min((double) INPUT_SIZE / width, (double) INPUT_SIZE / height);
            double padX = (INPUT_SIZE - width * scale) / 2.0;
            double padY = (INPUT_SIZE - height * scale) / 2.0;
            return new Letterbox(width, height, scale, padX, padY);
        }
    }

    /** Đầu vào đã có chiều batch (1) và engine ONNX không hỗ trợ stack, nên tắt batchifier. */
    @Override
    public Batchifier getBatchifier() {
        return null;
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        BufferedImage source = (BufferedImage) input.getWrappedImage();
        Letterbox box = Letterbox.of(source.getWidth(), source.getHeight());
        ctx.setAttachment(CTX_LETTERBOX, box);

        float[] data = toChw(letterbox(source, box));
        NDManager manager = ctx.getNDManager();
        NDArray array = manager.create(data, new Shape(1, 3, INPUT_SIZE, INPUT_SIZE));
        return new NDList(array);
    }

    private static BufferedImage letterbox(BufferedImage source, Letterbox box) {
        BufferedImage canvas = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(new Color(PAD_GRAY, PAD_GRAY, PAD_GRAY));
            g.fillRect(0, 0, INPUT_SIZE, INPUT_SIZE);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = Math.max(1, (int) Math.round(box.width() * box.scale()));
            int h = Math.max(1, (int) Math.round(box.height() * box.scale()));
            g.drawImage(source, (int) Math.round(box.padX()), (int) Math.round(box.padY()), w, h, null);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private static float[] toChw(BufferedImage image) {
        int plane = INPUT_SIZE * INPUT_SIZE;
        int[] argb = image.getRGB(0, 0, INPUT_SIZE, INPUT_SIZE, null, 0, INPUT_SIZE);
        float[] data = new float[3 * plane];
        for (int i = 0; i < plane; i++) {
            int p = argb[i];
            data[i] = ((p >> 16) & 0xFF) / PIXEL_MAX;
            data[plane + i] = ((p >> 8) & 0xFF) / PIXEL_MAX;
            data[2 * plane + i] = (p & 0xFF) / PIXEL_MAX;
        }
        return data;
    }

    @Override
    public List<Detection> processOutput(TranslatorContext ctx, NDList list) throws TranslateException {
        Letterbox box = (Letterbox) ctx.getAttachment(CTX_LETTERBOX);
        NDArray output = list.get(0);
        Shape shape = output.getShape();
        if (shape.dimension() != 3 || shape.get(0) != 1) {
            throw new TranslateException("Đầu ra YOLO có dạng lạ: " + shape);
        }
        int attributes = (int) shape.get(1);
        int anchors = (int) shape.get(2);
        int classes = attributes - BOX_VALUES;
        if (classes != labels.size()) {
            throw new TranslateException("Mô hình có " + classes + " lớp nhưng synset.txt có "
                    + labels.size() + " nhãn");
        }
        return decode(output.toFloatArray(), anchors, classes, box);
    }

    /** Tách riêng để kiểm thử: đầu vào là mảng phẳng {@code [4 + classes][anchors]}. */
    List<Detection> decode(float[] data, int anchors, int classes, Letterbox box) {
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < anchors; i++) {
            int best = -1;
            float bestScore = (float) minConfidence;
            for (int c = 0; c < classes; c++) {
                float score = data[(BOX_VALUES + c) * anchors + i];
                if (score >= bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
            if (best < 0) {
                continue;
            }
            double cx = data[i];
            double cy = data[anchors + i];
            double w = data[2 * anchors + i];
            double h = data[3 * anchors + i];
            double x1 = (cx - w / 2 - box.padX()) / box.scale();
            double y1 = (cy - h / 2 - box.padY()) / box.scale();
            double x2 = (cx + w / 2 - box.padX()) / box.scale();
            double y2 = (cy + h / 2 - box.padY()) / box.scale();
            candidates.add(new Candidate(best, bestScore,
                    clamp(x1, box.width()), clamp(y1, box.height()),
                    clamp(x2, box.width()), clamp(y2, box.height())));
        }
        List<Candidate> kept = nonMaxSuppression(candidates);
        List<Detection> result = new ArrayList<>(kept.size());
        for (Candidate c : kept) {
            double width = (c.x2 - c.x1) / box.width();
            double height = (c.y2 - c.y1) / box.height();
            if (width <= 0 || height <= 0) {
                continue;
            }
            result.add(new Detection(labels.get(c.classId), c.score,
                    c.x1 / box.width(), c.y1 / box.height(), width, height));
        }
        return result;
    }

    private static double clamp(double value, double max) {
        return Math.max(0.0, Math.min(max, value));
    }

    /** NMS theo từng lớp: chỉ loại hộp trùng nhau khi cùng lớp. */
    private static List<Candidate> nonMaxSuppression(List<Candidate> candidates) {
        candidates.sort(Comparator.comparingDouble((Candidate c) -> c.score).reversed());
        List<Candidate> kept = new ArrayList<>();
        for (Candidate candidate : candidates) {
            boolean overlapped = false;
            for (Candidate other : kept) {
                if (other.classId == candidate.classId && iou(other, candidate) > IOU_THRESHOLD) {
                    overlapped = true;
                    break;
                }
            }
            if (!overlapped) {
                kept.add(candidate);
                if (kept.size() == MAX_DETECTIONS) {
                    break;
                }
            }
        }
        return kept;
    }

    private static double iou(Candidate a, Candidate b) {
        double ix = Math.max(0.0, Math.min(a.x2, b.x2) - Math.max(a.x1, b.x1));
        double iy = Math.max(0.0, Math.min(a.y2, b.y2) - Math.max(a.y1, b.y1));
        double inter = ix * iy;
        double union = a.area() + b.area() - inter;
        return union <= 0 ? 0.0 : inter / union;
    }

    private record Candidate(int classId, double score, double x1, double y1, double x2, double y2) {
        double area() {
            return (x2 - x1) * (y2 - y1);
        }
    }
}
