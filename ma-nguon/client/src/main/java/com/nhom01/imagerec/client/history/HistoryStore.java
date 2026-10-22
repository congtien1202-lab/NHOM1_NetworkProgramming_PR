package com.nhom01.imagerec.client.history;

import com.nhom01.imagerec.client.export.ResultExporter;
import com.nhom01.imagerec.common.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Lưu lịch sử nhận dạng xuống đĩa.
 *
 * <p>Mỗi lần nhận dạng có id {@code yyyyMMdd-HHmmss-SSS}, gồm {@code <id>.png} (ảnh đã vẽ box),
 * {@code <id>.csv} (kết quả) và một dòng trong {@code index.tsv}:
 * {@code id TAB epochMillis TAB processingMillis TAB tên file gốc}.
 *
 * <p>Mặc định Client dùng thư mục {@link #defaultRoot()}. Test truyền thư mục tạm.
 */
public class HistoryStore {

    private static final String INDEX_FILE = "index.tsv";
    private static final DateTimeFormatter ID_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.systemDefault());
    private static final int INDEX_COLUMNS = 4;

    private final Path root;

    public HistoryStore(Path root) {
        this.root = root;
    }

    public static Path defaultRoot() {
        return Path.of(System.getProperty("user.home"), ".image-recognition", "history");
    }

    public Path root() {
        return root;
    }

    /** Lưu ảnh đã vẽ box và kết quả; trả về mục vừa lưu. */
    public synchronized HistoryEntry save(String fileName, BufferedImage annotated, DetectionResult result)
            throws IOException {
        Files.createDirectories(root);
        Instant now = Instant.now();
        String id = uniqueId(now);
        String cleanName = sanitize(fileName);
        Path png = root.resolve(id + ".png");

        ResultExporter.exportImage(annotated, png.toFile());
        ResultExporter.exportCsv(result, root.resolve(id + ".csv").toFile());

        String line = id + '\t' + now.toEpochMilli() + '\t' + result.processingMillis() + '\t' + cleanName + '\n';
        Files.writeString(root.resolve(INDEX_FILE), line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return new HistoryEntry(now, cleanName, png, result);
    }

    /** Toàn bộ lịch sử, mới nhất trước. Dòng hỏng hoặc thiếu file bị bỏ qua. */
    public synchronized List<HistoryEntry> list() throws IOException {
        Path index = root.resolve(INDEX_FILE);
        if (!Files.exists(index)) {
            return List.of();
        }
        List<HistoryEntry> entries = new ArrayList<>();
        for (String line : Files.readAllLines(index, StandardCharsets.UTF_8)) {
            parse(line).ifPresent(entries::add);
        }
        Collections.reverse(entries);
        return entries;
    }

    private Optional<HistoryEntry> parse(String line) {
        String[] columns = line.split("\t", -1);
        if (columns.length != INDEX_COLUMNS) {
            return Optional.empty();
        }
        String id = columns[0];
        Path png = root.resolve(id + ".png");
        Path csv = root.resolve(id + ".csv");
        if (!Files.exists(png) || !Files.exists(csv)) {
            return Optional.empty();
        }
        try {
            Instant time = Instant.ofEpochMilli(Long.parseLong(columns[1]));
            long processingMillis = Long.parseLong(columns[2]);
            return Optional.of(new HistoryEntry(time, columns[3], png,
                    ResultExporter.readCsv(csv, processingMillis)));
        } catch (NumberFormatException | IOException e) {
            return Optional.empty();
        }
    }

    private String uniqueId(Instant time) {
        String base = ID_FORMAT.format(time);
        String id = base;
        for (int suffix = 2; Files.exists(root.resolve(id + ".png")); suffix++) {
            id = base + '-' + suffix;
        }
        return id;
    }

    private static String sanitize(String fileName) {
        return fileName.replaceAll("[\\t\\r\\n]", " ");
    }
}
