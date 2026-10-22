package com.nhom01.imagerec.server.detect;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.nhom01.imagerec.common.Detection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Nhận dạng đối tượng bằng DJL + SSD-MobileNet (MXNet model zoo, VOC 20 lớp).
 *
 * <p>Thread-safe: mô hình dùng chung, mỗi lời gọi {@link #detect} tạo một Predictor riêng.
 */
public final class DjlObjectDetector implements ObjectDetector {

    /** Ngưỡng 0.3 (thay vì 0.5): SSD-MobileNet VOC chấm "bicycle" trong ảnh mẫu chỉ ~0.34. */
    static final double MIN_CONFIDENCE = 0.3;
    /** Mô hình SSD trả tọa độ theo ảnh đã resize về 512x512, không phải pixel ảnh gốc. */
    private static final double MODEL_INPUT_SIZE = 512.0;
    private static final String ARTIFACT_ID = "ssd";
    private static final String BACKBONE = "mobilenet1.0";
    private static final String DATASET = "voc";
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final ZooModel<Image, DetectedObjects> model;
    private volatile boolean closed;

    public DjlObjectDetector() {
        long start = System.nanoTime();
        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .optApplication(ai.djl.Application.CV.OBJECT_DETECTION)
                .setTypes(Image.class, DetectedObjects.class)
                .optEngine("MXNet")
                .optGroupId("ai.djl.mxnet")
                .optArtifactId(ARTIFACT_ID)
                .optFilter("backbone", BACKBONE)
                .optFilter("dataset", DATASET)
                .optProgress(new ProgressBar())
                .build();
        try {
            model = criteria.loadModel();
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new IllegalStateException(
                    "Không nạp được mô hình DJL " + ARTIFACT_ID + "/" + BACKBONE + "/" + DATASET
                            + ": " + e.getMessage(), e);
        }
        System.out.printf("Model %s loaded in %.1f s%n", model.getName(),
                (System.nanoTime() - start) / NANOS_PER_SECOND);
    }

    @Override
    public List<Detection> detect(BufferedImage image) throws DetectionException {
        if (closed) {
            throw new DetectionException("Detector đã đóng", new IllegalStateException("closed"));
        }
        Image input = ImageFactory.getInstance().fromImage(image);
        try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects found = predictor.predict(input);
            List<Detection> result = new ArrayList<>();
            for (DetectedObjects.DetectedObject object : found.<DetectedObjects.DetectedObject>items()) {
                if (object.getProbability() < MIN_CONFIDENCE) {
                    continue;
                }
                result.add(toDetection(object));
            }
            return result;
        } catch (TranslateException | RuntimeException e) {
            throw new DetectionException("Suy luận thất bại: " + e.getMessage(), e);
        }
    }

    private static Detection toDetection(DetectedObjects.DetectedObject object) {
        BoundingBox box = object.getBoundingBox();
        Rectangle bounds = box.getBounds();
        double x = clamp(bounds.getX() / MODEL_INPUT_SIZE);
        double y = clamp(bounds.getY() / MODEL_INPUT_SIZE);
        double w = Math.min(clamp(bounds.getWidth() / MODEL_INPUT_SIZE), 1.0 - x);
        double h = Math.min(clamp(bounds.getHeight() / MODEL_INPUT_SIZE), 1.0 - y);
        return new Detection(object.getClassName(), clamp(object.getProbability()), x, y, w, h);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    @Override
    public void close() {
        closed = true;
        model.close();
    }

    /** Chạy thử: {@code DjlObjectDetector <đường dẫn ảnh>}. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Cách dùng: DjlObjectDetector <ảnh>");
            System.exit(2);
        }
        BufferedImage image = ImageIO.read(Path.of(args[0]).toFile());
        if (image == null) {
            System.err.println("Không đọc được ảnh: " + args[0]);
            System.exit(2);
        }
        try (DjlObjectDetector detector = new DjlObjectDetector()) {
            long start = System.nanoTime();
            List<Detection> detections = detector.detect(image);
            System.out.printf("Detected %d object(s) in %.2f s%n", detections.size(),
                    (System.nanoTime() - start) / NANOS_PER_SECOND);
            for (Detection d : detections) {
                System.out.printf("%s %.2f [%.3f, %.3f, %.3f, %.3f]%n",
                        d.label(), d.confidence(), d.x(), d.y(), d.width(), d.height());
            }
        }
    }
}
