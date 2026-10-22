package com.nhom01.imagerec.client.history;

import com.nhom01.imagerec.common.Detection;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Tab "Lịch sử" trong cửa sổ Client.
 *
 * <p>MainFrame (PLAN-C) gắn panel này vào tab và gọi {@link #refresh()} sau mỗi lần lưu lịch sử.
 * {@link #refresh()} phải gọi trên EDT.
 */
public class HistoryPanel extends JPanel {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy").withZone(ZoneId.systemDefault());
    private static final int PREVIEW_WIDTH = 480;
    private static final int PREVIEW_HEIGHT = 320;
    private static final int LIST_WIDTH = 320;
    private static final double PERCENT = 100.0;

    private final HistoryStore store;
    private final DefaultListModel<HistoryEntry> listModel = new DefaultListModel<>();
    private final JList<HistoryEntry> list = new JList<>(listModel);
    private final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[] {"Nhãn", "Độ tin cậy (%)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public HistoryPanel(HistoryStore store) {
        super(new BorderLayout());
        this.store = store;
        buildLayout();
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        refresh();
    }

    /** Nạp lại danh sách từ đĩa, giữ lựa chọn nếu mục đó còn. Gọi trên EDT. */
    public void refresh() {
        HistoryEntry selected = list.getSelectedValue();
        try {
            List<HistoryEntry> entries = store.list();
            listModel.clear();
            entries.forEach(listModel::addElement);
            statusLabel.setText(entries.size() + " mục");
            restoreSelection(selected);
        } catch (IOException e) {
            statusLabel.setText("Không đọc được lịch sử: " + e.getMessage());
        }
    }

    private void buildLayout() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, entry, index, isSelected, hasFocus) -> {
            JLabel label = new JLabel(describe(entry));
            label.setOpaque(true);
            label.setBackground(isSelected ? l.getSelectionBackground() : l.getBackground());
            label.setForeground(isSelected ? l.getSelectionForeground() : l.getForeground());
            return label;
        });

        JButton refreshButton = new JButton("Làm mới");
        refreshButton.addActionListener(e -> refresh());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(refreshButton, BorderLayout.WEST);
        bottom.add(statusLabel, BorderLayout.CENTER);

        JPanel left = new JPanel(new BorderLayout());
        left.add(new JScrollPane(list), BorderLayout.CENTER);
        left.add(bottom, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(LIST_WIDTH, PREVIEW_HEIGHT));

        imageLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        JPanel right = new JPanel(new BorderLayout());
        right.add(imageLabel, BorderLayout.CENTER);
        right.add(new JScrollPane(new JTable(tableModel)), BorderLayout.SOUTH);

        add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right), BorderLayout.CENTER);
    }

    private void restoreSelection(HistoryEntry previous) {
        if (previous == null) {
            showSelected();
            return;
        }
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).annotatedImage().equals(previous.annotatedImage())) {
                list.setSelectedIndex(i);
                return;
            }
        }
        showSelected();
    }

    private void showSelected() {
        tableModel.setRowCount(0);
        imageLabel.setIcon(null);
        imageLabel.setText("");
        HistoryEntry entry = list.getSelectedValue();
        if (entry == null) {
            return;
        }
        for (Detection d : entry.result().detections()) {
            tableModel.addRow(new Object[] {d.label(), String.format("%.1f", d.confidence() * PERCENT)});
        }
        try {
            BufferedImage image = ImageIO.read(entry.annotatedImage().toFile());
            if (image == null) {
                throw new IOException("File ảnh không hợp lệ");
            }
            imageLabel.setIcon(new ImageIcon(scaleToFit(image)));
        } catch (IOException e) {
            imageLabel.setText("Không đọc được ảnh: " + e.getMessage());
        }
    }

    private static Image scaleToFit(BufferedImage image) {
        double scale = Math.min(1.0,
                Math.min((double) PREVIEW_WIDTH / image.getWidth(), (double) PREVIEW_HEIGHT / image.getHeight()));
        return image.getScaledInstance(
                Math.max(1, (int) (image.getWidth() * scale)),
                Math.max(1, (int) (image.getHeight() * scale)),
                Image.SCALE_SMOOTH);
    }

    private static String describe(HistoryEntry entry) {
        return TIME_FORMAT.format(entry.time()) + " - " + entry.fileName()
                + " (" + entry.result().detections().size() + " đối tượng)";
    }
}
