package com.nhom01.imagerec.client.history;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Chạy tay (Run File trong NetBeans): tạo vài mục lịch sử trong thư mục tạm rồi mở HistoryPanel. */
public final class HistoryPanelDemo {

    private HistoryPanelDemo() {
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("history-demo");
        HistoryStore store = new HistoryStore(dir);
        store.save("cat.jpg", sampleImage(Color.ORANGE),
                new DetectionResult(35, List.of(new Detection("cat", 0.92, 0.2, 0.2, 0.5, 0.5))));
        store.save("street.png", sampleImage(Color.CYAN), new DetectionResult(80, List.of(
                new Detection("person", 0.88, 0.1, 0.1, 0.2, 0.6),
                new Detection("car", 0.75, 0.5, 0.4, 0.4, 0.3),
                new Detection("dog", 0.51, 0.3, 0.6, 0.1, 0.2))));
        store.save("empty.jpg", sampleImage(Color.LIGHT_GRAY), new DetectionResult(12, List.of()));

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("HistoryPanel demo - " + dir);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new HistoryPanel(store));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static BufferedImage sampleImage(Color color) {
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 640, 400);
        g.setColor(Color.RED);
        g.drawRect(128, 80, 320, 200);
        g.dispose();
        return image;
    }
}
