package com.nhom01.imagerec.client.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Điểm khởi động của Client. */
public final class ClientMain {

    private ClientMain() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            System.err.println("Không đặt được giao diện hệ thống, dùng giao diện mặc định: " + e);
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
