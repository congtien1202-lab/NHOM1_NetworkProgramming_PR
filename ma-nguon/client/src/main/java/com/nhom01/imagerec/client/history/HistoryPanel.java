package com.nhom01.imagerec.client.history;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Stub giao diện lịch sử (sẽ được cài đặt trong PLAN-D / Issue 9).
 */
public class HistoryPanel extends JPanel {

    private final HistoryStore store;

    public HistoryPanel(HistoryStore store) {
        super(new BorderLayout());
        this.store = store;
        add(new JLabel("Lịch sử: chưa cài đặt", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    public void refresh() {
        // Stub: sẽ được cài đặt trong PLAN-D / Issue 9
    }
}
