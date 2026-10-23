package com.nhom01.imagerec.server;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Cửa sổ Server: bảng client đang kết nối + khung log. Mọi cập nhật Swing đi qua invokeLater. */
public final class ServerWindow extends JFrame {

    private static final String[] COLUMNS = {"ID", "Tên", "IP", "Kết nối lúc", "Số ảnh"};
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Dimension WINDOW_SIZE = new Dimension(900, 600);
    private static final int DIVIDER_LOCATION = 220;

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTextArea logArea = new JTextArea();

    public ServerWindow(SessionRegistry registry) {
        super("Image Recognition Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WINDOW_SIZE);
        setLocationRelativeTo(null);

        logArea.setEditable(false);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(new JTable(tableModel)), new JScrollPane(logArea));
        split.setDividerLocation(DIVIDER_LOCATION);
        add(split, BorderLayout.CENTER);

        registry.addListener(sessions -> SwingUtilities.invokeLater(() -> showSessions(sessions)));
        Logger.getLogger("").addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                String line = new ServerLogging.LineFormatter().format(record);
                SwingUtilities.invokeLater(() -> logArea.append(line));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    private void showSessions(List<SessionInfo> sessions) {
        tableModel.setRowCount(0);
        for (SessionInfo s : sessions) {
            tableModel.addRow(new Object[] {s.id(), s.clientName(), s.remoteAddress(),
                    s.connectedAt().format(TIME), s.imagesProcessed()});
        }
    }
}
