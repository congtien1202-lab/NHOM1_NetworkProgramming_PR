package com.nhom01.imagerec.client.ui;

import com.nhom01.imagerec.client.net.ServerConnection;
import com.nhom01.imagerec.client.net.ServerErrorException;
import com.nhom01.imagerec.client.render.BoxRenderer;
import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import com.nhom01.imagerec.common.ErrorCode;
import com.nhom01.imagerec.common.Protocol;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/** Cửa sổ chính của Client: kết nối Server, chọn ảnh, nhận dạng và xem kết quả. */
public final class MainFrame extends JFrame {

    private static final String TITLE = "Nhận dạng hình ảnh - Client";
    private static final Color GRAY = Color.GRAY;
    private static final Color GREEN = new Color(0x1E8E3E);
    private static final Color RED = new Color(0xD93025);
    /** Server triển khai trên VPS (docs/deploy.md); đổi bằng {@code -Dimagerec.host=localhost} khi chạy thử local. */
    private static final String DEFAULT_HOST = System.getProperty("imagerec.host", "ai.devlinh.com");
    private static final int HOST_COLUMNS = 14;
    private static final int PORT_COLUMNS = 5;
    private static final int NAME_COLUMNS = 10;
    private final JTextField hostField = new JTextField(DEFAULT_HOST, HOST_COLUMNS);
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), PORT_COLUMNS);
    private final JTextField nameField = new JTextField(defaultClientName(), NAME_COLUMNS);
    private final JButton connectButton = new JButton("Kết nối");
    private final JLabel connectionLabel = new JLabel();
    private final JButton chooseButton = new JButton("Chọn ảnh");
    private final JButton captureButton = new JButton("Mô phỏng chụp ảnh");
    private final JButton detectButton = new JButton("Nhận dạng");
    private final ImagePanel imagePanel = new ImagePanel();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"STT", "Nhãn", "Độ tin cậy (%)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel statusBar = new JLabel(" ");

    private final SampleImages sampleImages = new SampleImages();
    private final Timer pingTimer = new Timer(Protocol.PING_INTERVAL_MS, event -> pingServer());

    private ServerConnection connection;
    private boolean busy;
    private boolean pingInFlight;

    private String imageName;
    private byte[] imageBytes;
    private BufferedImage sourceImage;
    private BufferedImage annotatedImage;
    private DetectionResult lastResult;

    public MainFrame() {
        super(TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildLayout();
        wireActions();
        showDisconnected("Chưa kết nối", GRAY);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void dispose() {
        pingTimer.stop();
        ServerConnection current = connection;
        connection = null;
        if (current != null) {
            current.close();
        }
        super.dispose();
    }

    // ---- Bố cục ----

    private void buildLayout() {
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        top.add(buildConnectionBar(), BorderLayout.NORTH);
        top.add(buildActionBar(), BorderLayout.SOUTH);

        JTable table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new java.awt.Dimension(260, 0));
        JPanel recognizeTab = new JPanel(new BorderLayout());
        recognizeTab.add(imagePanel, BorderLayout.CENTER);
        recognizeTab.add(tableScroll, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Nhận dạng", recognizeTab);

        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel buildConnectionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(new JLabel("Host:"));
        bar.add(hostField);
        bar.add(new JLabel("Port:"));
        bar.add(portField);
        bar.add(new JLabel("Tên:"));
        bar.add(nameField);
        bar.add(connectButton);
        bar.add(connectionLabel);
        return bar;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton button : new JButton[] {chooseButton, captureButton, detectButton}) {
            button.setMargin(new Insets(4, 10, 4, 10));
            bar.add(button);
        }
        return bar;
    }

    private void wireActions() {
        connectButton.addActionListener(event -> toggleConnection());
        chooseButton.addActionListener(event -> chooseImage());
        captureButton.addActionListener(event -> simulateCapture());
        detectButton.addActionListener(event -> detect());
        imagePanel.setTransferHandler(new ImageDropHandler(() -> !busy, this::loadImageFile));
    }

    // ---- Trạng thái ----

    private boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    private void updateButtons() {
        boolean connected = isConnected();
        connectButton.setText(connected ? "Ngắt" : "Kết nối");
        connectButton.setEnabled(!busy);
        chooseButton.setEnabled(!busy);
        captureButton.setEnabled(!busy);
        detectButton.setEnabled(!busy && connected && imageBytes != null);
    }

    private void showConnectionStatus(String text, Color color) {
        connectionLabel.setText(text);
        connectionLabel.setForeground(color);
    }

    private void showDisconnected(String text, Color color) {
        pingTimer.stop();
        showConnectionStatus(text, color);
        updateButtons();
    }

    private void setStatus(String text) {
        statusBar.setText(text);
    }

    private void onConnectionLost() {
        ServerConnection lost = connection;
        connection = null;
        if (lost != null) {
            lost.close();
        }
        showDisconnected("Mất kết nối", RED);
        setStatus("Mất kết nối tới Server");
    }

    // ---- Kết nối ----

    private void toggleConnection() {
        if (isConnected()) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().strip());
        } catch (NumberFormatException e) {
            warn("Port phải là số nguyên");
            return;
        }
        String host = hostField.getText().strip();
        String name = nameField.getText().strip();
        if (host.isEmpty() || name.isEmpty()) {
            warn("Host và Tên không được để trống");
            return;
        }
        ServerConnection candidate = new ServerConnection();
        busy = true;
        showConnectionStatus("Đang kết nối...", GRAY);
        updateButtons();
        runAsync(() -> candidate.connect(host, port, name), sessionId -> {
            busy = false;
            connection = candidate;
            showConnectionStatus("Đã kết nối (phiên #" + sessionId + ")", GREEN);
            setStatus("Đã kết nối tới " + host + ":" + port);
            pingTimer.restart();
            updateButtons();
        }, error -> {
            busy = false;
            if (error instanceof ServerErrorException server && server.code() == ErrorCode.SERVER_BUSY) {
                showDisconnected("Server đang bận, thử lại sau", RED);
                setStatus("Server đang bận, thử lại sau");
            } else if (error instanceof ServerErrorException server) {
                showDisconnected("Chưa kết nối", GRAY);
                setStatus(server.getMessage());
                warn(server.getMessage());
            } else {
                showDisconnected("Mất kết nối", RED);
                setStatus("Không kết nối được: " + error.getMessage());
                warn("Không kết nối được tới " + host + ":" + port + "\n" + error.getMessage());
            }
        });
    }

    private void disconnect() {
        ServerConnection closing = connection;
        connection = null;
        showDisconnected("Chưa kết nối", GRAY);
        setStatus("Đã ngắt kết nối");
        runAsync(() -> {
            closing.close();
            return null;
        }, ignored -> { }, error -> setStatus("Lỗi khi ngắt kết nối: " + error.getMessage()));
    }

    private void pingServer() {
        ServerConnection target = connection;
        if (target == null || pingInFlight) {
            return;
        }
        pingInFlight = true;
        runAsync(target::ping, alive -> {
            pingInFlight = false;
            if (!alive && connection == target) {
                onConnectionLost();
            }
        }, error -> pingInFlight = false);
    }

    // ---- Chọn ảnh ----

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh JPG/PNG", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        loadImageFile(chooser.getSelectedFile());
    }

    /** Dùng chung cho nút Chọn ảnh và kéo thả. */
    private void loadImageFile(File file) {
        Optional<String> problem = ImageFiles.validate(file);
        if (problem.isPresent()) {
            warn(problem.get());
            return;
        }
        try {
            showImage(file.getName(), Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            warn("Không đọc được file: " + e.getMessage());
        }
    }

    private void simulateCapture() {
        try {
            SampleImages.Sample sample = sampleImages.pickRandom();
            showImage(sample.fileName(), sample.bytes());
        } catch (IOException e) {
            warn("Không lấy được ảnh mẫu: " + e.getMessage());
        }
    }

    private void showImage(String name, byte[] bytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("Không đọc được nội dung ảnh");
        }
        imageName = name;
        imageBytes = bytes;
        sourceImage = image;
        annotatedImage = null;
        lastResult = null;
        tableModel.setRowCount(0);
        imagePanel.setImage(image);
        setStatus("Đã chọn ảnh " + name);
        updateButtons();
    }

    // ---- Nhận dạng ----

    private void detect() {
        ServerConnection target = connection;
        String name = imageName;
        byte[] bytes = imageBytes;
        BufferedImage source = sourceImage;
        busy = true;
        setStatus("Đang nhận dạng...");
        updateButtons();
        runAsync(() -> target.detect(name, bytes), result -> {
            busy = false;
            showResult(name, source, result);
        }, error -> {
            busy = false;
            if (error instanceof ServerErrorException server) {
                setStatus(server.getMessage());
                warn(server.getMessage());
                updateButtons();
            } else {
                onConnectionLost();
            }
        });
    }

    private void showResult(String name, BufferedImage source, DetectionResult result) {
        annotatedImage = BoxRenderer.render(source, result);
        lastResult = result;
        imagePanel.setImage(annotatedImage);
        tableModel.setRowCount(0);
        int row = 1;
        for (Detection d : result.detections()) {
            tableModel.addRow(new Object[] {
                row++, d.label(), String.format(Locale.ROOT, "%.1f", d.confidence() * 100)});
        }
        String summary = result.detections().size() + " đối tượng, " + result.processingMillis() + " ms";
        setStatus(summary);
        updateButtons();
    }

    // ---- Tiện ích ----

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, TITLE, JOptionPane.WARNING_MESSAGE);
    }

    private static String defaultClientName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "client";
        }
    }

    /** Chạy tác vụ blocking ngoài EDT, trả kết quả/lỗi về EDT. */
    private static <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    onError.accept(cause instanceof Exception exception ? exception : e);
                }
            }
        }.execute();
    }
}
