package com.nhom01.imagerec.server;

import com.nhom01.imagerec.common.Protocol;
import java.util.Properties;
import java.util.Set;

/**
 * Cấu hình Server. Thứ tự ưu tiên: tham số dòng lệnh ({@code --port=5000}) &gt; server.properties &gt; mặc định.
 *
 * <p>{@code model}: đường dẫn file ONNX cho detector djl, {@code null} nghĩa là dùng mô hình đóng trong jar.
 * {@code ortThreads}: số luồng intra-op của ONNX Runtime cho mỗi lần suy luận, {@code null} nghĩa là để mặc định.
 */
public record ServerConfig(int port, String detector, boolean gui, int maxClients, String model,
                           Integer ortThreads) {

    public static final String DETECTOR_FAKE = "fake";
    public static final String DETECTOR_DJL = "djl";

    private static final int DEFAULT_MAX_CLIENTS = 20;
    private static final int MAX_PORT = 65_535;
    private static final int MAX_ORT_THREADS = 256;
    private static final String ARG_PREFIX = "--";
    private static final Set<String> DETECTORS = Set.of(DETECTOR_FAKE, DETECTOR_DJL);

    public static ServerConfig parse(Properties properties, String[] args) {
        Properties merged = new Properties();
        merged.putAll(properties);
        for (String arg : args) {
            int equals = arg.indexOf('=');
            if (!arg.startsWith(ARG_PREFIX) || equals < 0) {
                throw new IllegalArgumentException("Tham số không hợp lệ: " + arg + " (dạng --tên=giá trị)");
            }
            merged.setProperty(arg.substring(ARG_PREFIX.length(), equals), arg.substring(equals + 1));
        }

        int port = intValue(merged, "port", Protocol.DEFAULT_PORT, 0, MAX_PORT);
        String detector = merged.getProperty("detector", DETECTOR_FAKE).trim();
        if (!DETECTORS.contains(detector)) {
            throw new IllegalArgumentException("detector phải là 'fake' hoặc 'djl', nhận được: " + detector);
        }
        boolean gui = booleanValue(merged, "gui", true);
        int maxClients = intValue(merged, "maxClients", DEFAULT_MAX_CLIENTS, 1, Integer.MAX_VALUE);
        String model = merged.getProperty("model", "").trim();
        String threads = merged.getProperty("ortThreads", "").trim();
        Integer ortThreads = threads.isEmpty() ? null : intValue(merged, "ortThreads", 0, 1, MAX_ORT_THREADS);
        return new ServerConfig(port, detector, gui, maxClients, model.isEmpty() ? null : model, ortThreads);
    }

    private static int intValue(Properties props, String key, int fallback, int min, int max) {
        String raw = props.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                throw new IllegalArgumentException(key + " phải nằm trong khoảng " + min + ".." + max + ": " + raw);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " phải là số nguyên, nhận được: " + raw, e);
        }
    }

    private static boolean booleanValue(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(key + " phải là true hoặc false, nhận được: " + raw);
        }
        return Boolean.parseBoolean(value);
    }
}
