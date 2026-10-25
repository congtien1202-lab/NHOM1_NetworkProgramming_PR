package com.nhom01.imagerec.server.detect;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.nhom01.imagerec.common.Detection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Nhận dạng đối tượng bằng DJL + YOLOv8 (ONNX, engine OnnxRuntime, chạy CPU).
 *
 * <p>Mặc định nạp mô hình COCO 80 lớp + monkey (81 lớp) đóng sẵn trong jar ({@code /models/yolov8s-coco81.onnx} và
 * {@code /models/synset.txt}); có thể trỏ tới một file ONNX khác bằng {@link #DjlObjectDetector(Path)}
 * (nhãn lấy từ {@code synset.txt} nằm cạnh file đó).
 *
 * <p>Khi mô hình có nhãn {@code monkey}, kết quả đi qua {@link MonkeyGuard}: ngoài ngưỡng riêng cho khỉ còn có
 * mô hình YOLOv8s gốc 80 lớp ({@code /models/guard/}) nạp một lần và chỉ chạy khi còn box khỉ.
 *
 * <p>Thread-safe: mô hình dùng chung, mỗi lời gọi {@link #detect} tạo một Predictor riêng.
 */
public final class DjlObjectDetector implements ObjectDetector {

    /** Ngưỡng tin cậy giữ 0.3 như bản SSD cũ để so sánh công bằng. */
    static final double MIN_CONFIDENCE = 0.3;
    private static final String RESOURCE_DIR = "/models/";
    private static final String MODEL_FILE = "yolov8s-coco81.onnx";
    private static final String SYNSET_FILE = "synset.txt";
    private static final String GUARD_DIR = "/models/guard/";
    private static final String GUARD_MODEL_FILE = "yolov8s-coco80.onnx";
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final ZooModel<Image, List<Detection>> model;
    /** Mô hình gốc 80 lớp cho lớp bảo vệ khỉ, {@code null} nếu mô hình chính không có nhãn khỉ. */
    private final ZooModel<Image, List<Detection>> guardModel;
    private volatile boolean closed;

    /** Dùng mô hình đóng sẵn trong jar. */
    public DjlObjectDetector() {
        this(null);
    }

    /** @param onnx đường dẫn file ONNX, hoặc {@code null} để dùng mô hình trong jar */
    public DjlObjectDetector(Path onnx) {
        this(onnx, null);
    }

    /**
     * @param onnx       đường dẫn file ONNX, hoặc {@code null} để dùng mô hình trong jar
     * @param ortThreads số luồng intra-op của ONNX Runtime cho mỗi lần suy luận, {@code null} = mặc định
     */
    public DjlObjectDetector(Path onnx, Integer ortThreads) {
        long start = System.nanoTime();
        Path tempDir = null;
        try {
            Path modelFile;
            List<String> labels;
            if (onnx == null) {
                tempDir = Files.createTempDirectory("imagerec-model");
                modelFile = tempDir.resolve(MODEL_FILE);
                copyResource(RESOURCE_DIR + MODEL_FILE, modelFile);
                labels = readLabels(openResource(RESOURCE_DIR + SYNSET_FILE));
            } else {
                modelFile = onnx.toAbsolutePath();
                if (!Files.isRegularFile(modelFile)) {
                    throw new IllegalStateException("Không thấy file mô hình: " + modelFile);
                }
                Path synset = modelFile.resolveSibling(SYNSET_FILE);
                if (!Files.isRegularFile(synset)) {
                    throw new IllegalStateException("Thiếu " + SYNSET_FILE + " cạnh mô hình: " + synset);
                }
                labels = readLabels(Files.newInputStream(synset));
            }
            model = load(modelFile, new YoloTranslator(labels, MIN_CONFIDENCE), ortThreads);
            guardModel = labels.contains(MonkeyGuard.MONKEY_LABEL) ? loadGuard(ortThreads) : null;
            System.out.printf("Model %s (%d nhãn) loaded in %.1f s%n", modelFile.getFileName(), labels.size(),
                    (System.nanoTime() - start) / NANOS_PER_SECOND);
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new IllegalStateException("Không nạp được mô hình YOLO: " + e.getMessage(), e);
        } finally {
            deleteQuietly(tempDir);
        }
    }

    private static ZooModel<Image, List<Detection>> loadGuard(Integer ortThreads) throws IOException,
            ModelNotFoundException, MalformedModelException {
        Path dir = Files.createTempDirectory("imagerec-guard");
        try {
            Path file = dir.resolve(GUARD_MODEL_FILE);
            copyResource(GUARD_DIR + GUARD_MODEL_FILE, file);
            List<String> labels = readLabels(openResource(GUARD_DIR + SYNSET_FILE));
            return load(file, new YoloTranslator(labels, MonkeyGuard.GUARD_MIN_CONFIDENCE), ortThreads);
        } finally {
            deleteQuietly(dir);
        }
    }

    @SuppressWarnings("unchecked")
    private static ZooModel<Image, List<Detection>> load(Path modelFile, YoloTranslator translator,
            Integer ortThreads)
            throws IOException, ModelNotFoundException, MalformedModelException {
        String fileName = modelFile.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - ".onnx".length());
        Criteria.Builder<Image, List<Detection>> builder = Criteria.builder()
                .setTypes(Image.class, (Class<List<Detection>>) (Class<?>) List.class)
                .optModelPath(modelFile.getParent())
                .optModelName(name)
                .optEngine("OnnxRuntime")
                .optTranslator(translator);
        if (ortThreads != null) {
            builder.optOption("intraOpNumThreads", String.valueOf(ortThreads));
        }
        return builder.build().loadModel();
    }

    private static InputStream openResource(String path) throws IOException {
        InputStream in = DjlObjectDetector.class.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("Thiếu tài nguyên trong jar: " + path);
        }
        return in;
    }

    private static void copyResource(String path, Path target) throws IOException {
        try (InputStream in = openResource(path)) {
            Files.copy(in, target);
        }
    }

    private static List<String> readLabels(InputStream in) throws IOException {
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .toList();
        }
    }

    /** ONNX Runtime đã nạp xong mô hình vào bộ nhớ nên file tạm không còn cần nữa. */
    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var files = Files.walk(dir)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // file tạm sẽ được hệ điều hành dọn sau
        }
    }

    @Override
    public List<Detection> detect(BufferedImage image) throws DetectionException {
        if (closed) {
            throw new DetectionException("Detector đã đóng", new IllegalStateException("closed"));
        }
        Image input = ImageFactory.getInstance().fromImage(image);
        try (Predictor<Image, List<Detection>> predictor = model.newPredictor()) {
            List<Detection> found = predictor.predict(input);
            return guardModel == null ? found : MonkeyGuard.apply(found, () -> guardPredict(input));
        } catch (TranslateException | RuntimeException e) {
            throw new DetectionException("Suy luận thất bại: " + e.getMessage(), e);
        }
    }

    private List<Detection> guardPredict(Image input) {
        try (Predictor<Image, List<Detection>> predictor = guardModel.newPredictor()) {
            return predictor.predict(input);
        } catch (TranslateException e) {
            throw new IllegalStateException("Suy luận lớp bảo vệ thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        closed = true;
        model.close();
        if (guardModel != null) {
            guardModel.close();
        }
    }

    /** Chạy thử: {@code DjlObjectDetector <ảnh> [file.onnx]}. */
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Cách dùng: DjlObjectDetector <ảnh> [file.onnx]");
            System.exit(2);
        }
        BufferedImage image = ImageIO.read(Path.of(args[0]).toFile());
        if (image == null) {
            System.err.println("Không đọc được ảnh: " + args[0]);
            System.exit(2);
        }
        Path onnx = args.length == 2 ? Path.of(args[1]) : null;
        try (DjlObjectDetector detector = new DjlObjectDetector(onnx)) {
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
