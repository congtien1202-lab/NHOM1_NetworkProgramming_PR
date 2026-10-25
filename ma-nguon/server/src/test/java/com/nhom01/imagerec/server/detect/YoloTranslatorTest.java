package com.nhom01.imagerec.server.detect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.nhom01.imagerec.common.Detection;
import java.util.List;
import org.junit.jupiter.api.Test;

class YoloTranslatorTest {

    private static final int ANCHORS = 3;
    private static final int CLASSES = 2;
    private static final double EPS = 1e-6;

    private final YoloTranslator translator = new YoloTranslator(List.of("cat", "dog"), 0.3);

    /** Ảnh rộng 1280x640: letterbox thu 0.5 và chèn viền trên/dưới 160 px. */
    private final YoloTranslator.Letterbox wide = YoloTranslator.Letterbox.of(1280, 640);

    /** Dữ liệu phẳng [4 + CLASSES][ANCHORS]: mỗi hàng là một thuộc tính của cả 3 anchor. */
    private static float[] data(float[] cx, float[] cy, float[] w, float[] h, float[] cat, float[] dog) {
        float[] out = new float[(4 + CLASSES) * ANCHORS];
        float[][] rows = {cx, cy, w, h, cat, dog};
        for (int r = 0; r < rows.length; r++) {
            System.arraycopy(rows[r], 0, out, r * ANCHORS, ANCHORS);
        }
        return out;
    }

    @Test
    void letterboxHasExpectedScaleAndPadding() {
        assertThat(wide.scale()).isEqualTo(0.5);
        assertThat(wide.padX()).isZero();
        assertThat(wide.padY()).isEqualTo(160.0);
    }

    @Test
    void boxIsMappedBackToOriginalImageRatios() {
        // hộp tâm (320,320), rộng 320, cao 160 trong ảnh 640x640 -> gốc: x 320..960, y 160..480
        float[] data = data(
                new float[] {320, 0, 0}, new float[] {320, 0, 0},
                new float[] {320, 0, 0}, new float[] {160, 0, 0},
                new float[] {0.9f, 0, 0}, new float[] {0.1f, 0, 0});

        List<Detection> result = translator.decode(data, ANCHORS, CLASSES, wide);

        assertThat(result).hasSize(1);
        Detection d = result.get(0);
        assertThat(d.label()).isEqualTo("cat");
        assertThat(d.confidence()).isCloseTo(0.9, within(EPS));
        assertThat(d.x()).isCloseTo(0.25, within(EPS));
        assertThat(d.y()).isCloseTo(0.25, within(EPS));
        assertThat(d.width()).isCloseTo(0.5, within(EPS));
        assertThat(d.height()).isCloseTo(0.5, within(EPS));
    }

    @Test
    void boxPartlyInPaddingIsClampedInsideImage() {
        // hộp trải từ y=100 đến 300 trong ảnh letterbox, trong khi vùng ảnh thật chỉ từ y=160
        float[] data = data(
                new float[] {320, 0, 0}, new float[] {200, 0, 0},
                new float[] {100, 0, 0}, new float[] {200, 0, 0},
                new float[] {0, 0, 0}, new float[] {0.8f, 0, 0});

        Detection d = translator.decode(data, ANCHORS, CLASSES, wide).get(0);

        assertThat(d.y()).isZero();
        assertThat(d.y() + d.height()).isLessThanOrEqualTo(1.0);
        assertThat(d.height()).isCloseTo(0.4375, within(EPS));
    }

    @Test
    void lowConfidenceIsDropped() {
        float[] data = data(
                new float[] {320, 0, 0}, new float[] {320, 0, 0},
                new float[] {100, 0, 0}, new float[] {100, 0, 0},
                new float[] {0.29f, 0, 0}, new float[] {0.1f, 0, 0});

        assertThat(translator.decode(data, ANCHORS, CLASSES, wide)).isEmpty();
    }

    @Test
    void nmsKeepsHigherScoreOfOverlappingSameClassButNotOtherClass() {
        // anchor 0 và 1 gần như trùng nhau; 0 và 1 cùng lớp "cat", anchor 2 trùng vị trí nhưng lớp "dog"
        float[] data = data(
                new float[] {320, 322, 320}, new float[] {320, 320, 320},
                new float[] {200, 200, 200}, new float[] {200, 200, 200},
                new float[] {0.9f, 0.6f, 0}, new float[] {0, 0, 0.7f});

        List<Detection> result = translator.decode(data, ANCHORS, CLASSES, wide);

        assertThat(result).extracting(Detection::label).containsExactly("cat", "dog");
        assertThat(result.get(0).confidence()).isCloseTo(0.9, within(EPS));
    }
}
