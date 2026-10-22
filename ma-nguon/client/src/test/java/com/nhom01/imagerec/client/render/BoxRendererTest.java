package com.nhom01.imagerec.client.render;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoxRendererTest {

    private static BufferedImage whiteImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, Color.WHITE.getRGB());
            }
        }
        return image;
    }

    @Test
    void drawsBoxEdgeAtScaledCoordinatesWithoutTouchingOriginal() {
        BufferedImage source = whiteImage(200, 100);
        DetectionResult result = new DetectionResult(1, List.of(new Detection("cat", 0.9, 0.25, 0.5, 0.5, 0.4)));

        BufferedImage annotated = BoxRenderer.render(source, result);

        assertThat(annotated.getWidth()).isEqualTo(200);
        assertThat(annotated.getHeight()).isEqualTo(100);
        // Cạnh trái của box tại x = 0.25 * 200 = 50, giữa chiều cao box (y = 50 + 20 = 70).
        assertThat(annotated.getRGB(50, 70)).isNotEqualTo(Color.WHITE.getRGB());
        // Giữa box không bị tô.
        assertThat(annotated.getRGB(100, 70)).isEqualTo(Color.WHITE.getRGB());
        assertThat(source.getRGB(50, 70)).isEqualTo(Color.WHITE.getRGB());
    }

    @Test
    void noDetectionsGivesIdenticalCopy() {
        BufferedImage source = whiteImage(10, 10);

        BufferedImage annotated = BoxRenderer.render(source, new DetectionResult(0, List.of()));

        assertThat(annotated).isNotSameAs(source);
        assertThat(annotated.getRGB(5, 5)).isEqualTo(Color.WHITE.getRGB());
    }
}
