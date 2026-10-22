package com.nhom01.imagerec.client.render;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Vẽ bounding box và nhãn lên một bản sao của ảnh. Dùng chung cho hiển thị (PLAN-C),
 * xuất file và lịch sử (PLAN-D). Ảnh gốc không bị thay đổi.
 */
public final class BoxRenderer {

    private static final Color[] PALETTE = {
        new Color(0xE6194B), new Color(0x3CB44B), new Color(0x4363D8), new Color(0xF58231),
        new Color(0x911EB4), new Color(0x42D4F4), new Color(0xF032E6), new Color(0x9A6324),
    };

    private BoxRenderer() {
    }

    public static BufferedImage render(BufferedImage source, DetectionResult result) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stroke = Math.max(2, Math.min(width, height) / 200);
            g.setStroke(new BasicStroke(stroke));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, Math.min(width, height) / 30)));
            for (Detection d : result.detections()) {
                drawDetection(g, d, width, height);
            }
        } finally {
            g.dispose();
        }
        return copy;
    }

    private static void drawDetection(Graphics2D g, Detection d, int width, int height) {
        int x = (int) Math.round(d.x() * width);
        int y = (int) Math.round(d.y() * height);
        int w = (int) Math.round(d.width() * width);
        int h = (int) Math.round(d.height() * height);
        Color color = PALETTE[Math.floorMod(d.label().hashCode(), PALETTE.length)];

        g.setColor(color);
        g.drawRect(x, y, w, h);

        String text = d.label() + " " + Math.round(d.confidence() * 100) + "%";
        FontMetrics metrics = g.getFontMetrics();
        int textHeight = metrics.getHeight();
        int labelY = y - textHeight >= 0 ? y - textHeight : y;
        g.fillRect(x, labelY, metrics.stringWidth(text) + 6, textHeight);
        g.setColor(Color.WHITE);
        g.drawString(text, x + 3, labelY + metrics.getAscent());
    }
}
