package com.nhom01.imagerec.server;

import com.nhom01.imagerec.common.Protocol;
import com.nhom01.imagerec.server.detect.FakeObjectDetector;
import com.nhom01.imagerec.server.detect.ObjectDetector;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/** Điểm vào của Server: đọc cấu hình, tạo detector, mở cửa sổ (nếu gui=true) và bắt đầu lắng nghe. */
public final class ServerMain {

    private static final String PROPERTIES_RESOURCE = "/server.properties";
    private static final int EXIT_BAD_USAGE = 2;
    private static final int EXIT_STARTUP_FAILED = 1;

    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        ServerLogging.configure();
        Logger log = Logger.getLogger(ServerMain.class.getName());

        ServerConfig config;
        try {
            config = ServerConfig.parse(loadProperties(), args);
        } catch (IllegalArgumentException e) {
            log.severe("Cấu hình sai: " + e.getMessage());
            System.exit(EXIT_BAD_USAGE);
            return;
        }

        ObjectDetector detector;
        try {
            detector = createDetector(config);
        } catch (RuntimeException e) {
            log.severe("Không khởi tạo được detector '" + config.detector() + "': " + e.getMessage());
            System.exit(EXIT_STARTUP_FAILED);
            return;
        }

        SessionRegistry registry = new SessionRegistry();
        if (config.gui()) {
            SwingUtilities.invokeLater(() -> new ServerWindow(registry).setVisible(true));
        }

        ImageRecognitionServer server = new ImageRecognitionServer(config.port(), detector, registry,
                Protocol.READ_TIMEOUT_MS, config.maxClients());
        try {
            server.start();
        } catch (IOException e) {
            log.severe("Không mở được port " + config.port() + ": " + e.getMessage());
            detector.close();
            System.exit(EXIT_STARTUP_FAILED);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            detector.close();
        }, "shutdown"));
    }

    private static ObjectDetector createDetector(ServerConfig config) {
        if (!ServerConfig.DETECTOR_DJL.equals(config.detector())) {
            return new FakeObjectDetector();
        }
        try {
            Class<?> clazz = Class.forName("com.nhom01.imagerec.server.detect.DjlObjectDetector");
            return (ObjectDetector) clazz.getConstructor(Path.class)
                    .newInstance(config.model() == null ? null : Path.of(config.model()));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PLAN-B: DjlObjectDetector chưa được cài đặt", e);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Lỗi khởi tạo DjlObjectDetector: " + cause.getMessage(), cause);
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = ServerMain.class.getResourceAsStream(PROPERTIES_RESOURCE)) {
            if (in != null) {
                properties.load(in);
            }
        }
        return properties;
    }
}
