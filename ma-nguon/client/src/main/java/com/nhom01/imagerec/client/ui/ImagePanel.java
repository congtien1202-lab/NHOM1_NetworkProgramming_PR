package com.nhom01.imagerec.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/** Hiển thị một ảnh co giãn theo kích thước panel, giữ tỉ lệ và căn giữa. */
final class ImagePanel extends JPanel {

    private static final Dimension PREFERRED_SIZE = new Dimension(640, 480);
    private static final String EMPTY_HINT = "Chưa có ảnh - kéo thả file JPG/PNG vào đây";

    private BufferedImage image;

    ImagePanel() {
        setBackground(new Color(0x2B2B2B));
        setPreferredSize(PREFERRED_SIZE);
    }

    void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (image == null) {
                g.setColor(Color.LIGHT_GRAY);
                int textWidth = g.getFontMetrics().stringWidth(EMPTY_HINT);
                g.drawString(EMPTY_HINT, (getWidth() - textWidth) / 2, getHeight() / 2);
                return;
            }
            double scale = Math.min((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            g.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
        } finally {
            g.dispose();
        }
    }
}
