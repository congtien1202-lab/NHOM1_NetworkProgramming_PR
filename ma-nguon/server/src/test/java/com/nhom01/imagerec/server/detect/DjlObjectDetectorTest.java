package com.nhom01.imagerec.server.detect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhom01.imagerec.common.Detection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class DjlObjectDetectorTest {

    private static final int THREADS = 4;
    private static final int CALLS_PER_THREAD = 5;
    private static final long TIMEOUT_SECONDS = 120;

    private static DjlObjectDetector detector;
    private static BufferedImage sample;

    @BeforeAll
    static void setUp() throws IOException {
        try (InputStream in = DjlObjectDetectorTest.class.getResourceAsStream("/djl/dog_bike_car.jpg")) {
            sample = ImageIO.read(in);
        }
        detector = new DjlObjectDetector();
    }

    @AfterAll
    static void tearDown() {
        detector.close();
    }

    @Test
    void detectsDogAndBicycleWithNormalizedBoxes() throws DetectionException {
        List<Detection> detections = detector.detect(sample);

        assertThat(labels(detections)).contains("dog", "bicycle");
        // xe phía sau là "truck" với YOLOv8s, nhưng chấp nhận "car" nếu đổi trọng số
        assertThat(labels(detections)).containsAnyOf("truck", "car");
        for (Detection d : detections) {
            assertThat(d.confidence()).isBetween(DjlObjectDetector.MIN_CONFIDENCE, 1.0);
            assertThat(d.x()).isBetween(0.0, 1.0);
            assertThat(d.y()).isBetween(0.0, 1.0);
            assertThat(d.x() + d.width()).isLessThanOrEqualTo(1.0);
            assertThat(d.y() + d.height()).isLessThanOrEqualTo(1.0);
            assertThat(d.width()).isPositive();
            assertThat(d.height()).isPositive();
        }
    }

    @Test
    void detectsMonkeyAndNotPerson() throws Exception {
        BufferedImage monkey;
        try (InputStream in = DjlObjectDetectorTest.class.getResourceAsStream("/djl/monkey.jpg")) {
            monkey = ImageIO.read(in);
        }

        List<Detection> detections = detector.detect(monkey);

        assertThat(labels(detections)).contains("monkey").doesNotContain("person");
    }

    @Test
    void concurrentCallsReturnSameLabels() throws Exception {
        Set<String> expected = labels(detector.detect(sample));
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Set<String>>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS * CALLS_PER_THREAD; i++) {
                futures.add(pool.submit(() -> labels(detector.detect(sample))));
            }
            for (Future<Set<String>> future : futures) {
                assertThat(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(expected);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void detectAfterCloseThrows() {
        DjlObjectDetector closed = new DjlObjectDetector();
        closed.close();

        assertThatThrownBy(() -> closed.detect(sample)).isInstanceOf(DetectionException.class);
    }

    private static Set<String> labels(List<Detection> detections) {
        return detections.stream().map(Detection::label).collect(Collectors.toSet());
    }
}
