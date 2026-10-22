package com.nhom01.imagerec.client.export;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Xuất kết quả nhận dạng ra file (PNG, CSV) và đọc lại CSV. */
public final class ResultExporter {

    private static final String BOM = "﻿";
    private static final String HEADER = "label,confidence,x,y,width,height";
    private static final String NEWLINE = "\r\n";
    private static final int COLUMN_COUNT = 6;

    private ResultExporter() {
    }

    /** Ghi ảnh đã vẽ bounding box ra file PNG. */
    public static void exportImage(BufferedImage annotated, File out) throws IOException {
        if (!ImageIO.write(annotated, "png", out)) {
            throw new IOException("Không ghi được ảnh PNG: " + out);
        }
    }

    /** Ghi kết quả ra CSV UTF-8 (có BOM): label,confidence,x,y,width,height. Số luôn dùng dấu chấm. */
    public static void exportCsv(DetectionResult result, File out) throws IOException {
        StringBuilder csv = new StringBuilder(BOM).append(HEADER).append(NEWLINE);
        for (Detection d : result.detections()) {
            // Double.toString không phụ thuộc Locale, luôn dùng dấu chấm.
            csv.append(quote(d.label()))
                    .append(',').append(d.confidence())
                    .append(',').append(d.x())
                    .append(',').append(d.y())
                    .append(',').append(d.width())
                    .append(',').append(d.height())
                    .append(NEWLINE);
        }
        Files.writeString(out.toPath(), csv, StandardCharsets.UTF_8);
    }

    /** Đọc lại file CSV do {@link #exportCsv} tạo ra. */
    public static DetectionResult readCsv(Path csv, long processingMillis) throws IOException {
        String text = Files.readString(csv, StandardCharsets.UTF_8);
        if (text.startsWith(BOM)) {
            text = text.substring(1);
        }
        List<Detection> detections = new ArrayList<>();
        boolean header = true;
        for (String line : text.split("\r?\n")) {
            if (line.isEmpty()) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            detections.add(parseLine(line));
        }
        return new DetectionResult(processingMillis, detections);
    }

    private static Detection parseLine(String line) throws IOException {
        List<String> cells = splitCsv(line);
        if (cells.size() != COLUMN_COUNT) {
            throw new IOException("Dòng CSV không đủ " + COLUMN_COUNT + " cột: " + line);
        }
        try {
            return new Detection(cells.get(0),
                    Double.parseDouble(cells.get(1)), Double.parseDouble(cells.get(2)),
                    Double.parseDouble(cells.get(3)), Double.parseDouble(cells.get(4)),
                    Double.parseDouble(cells.get(5)));
        } catch (NumberFormatException e) {
            throw new IOException("Số không hợp lệ trong dòng CSV: " + line, e);
        }
    }

    private static List<String> splitCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    private static String quote(String label) {
        if (label.contains(",") || label.contains("\"") || label.contains("\n") || label.contains("\r")) {
            return '"' + label.replace("\"", "\"\"") + '"';
        }
        return label;
    }
}
