package com.nhom01.imagerec.client.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

/** Ảnh mẫu đóng gói trong jar cho chức năng "Mô phỏng chụp ảnh". */
final class SampleImages {

    /** Một ảnh mẫu đã đọc: tên file và nội dung JPG/PNG nguyên bản. */
    record Sample(String fileName, byte[] bytes) {
    }

    private static final String BASE = "/samples/";
    private static final String INDEX = BASE + "index.txt";

    private final Random random = new Random();

    /** Chọn ngẫu nhiên một ảnh trong {@code samples/index.txt}. */
    Sample pickRandom() throws IOException {
        List<String> names = readIndex();
        if (names.isEmpty()) {
            throw new IOException("Không có ảnh mẫu nào trong samples/index.txt");
        }
        String name = names.get(random.nextInt(names.size()));
        return new Sample(name, readResource(BASE + name));
    }

    private List<String> readIndex() throws IOException {
        String text = new String(readResource(INDEX), StandardCharsets.UTF_8);
        return text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
    }

    private static byte[] readResource(String path) throws IOException {
        try (InputStream in = SampleImages.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Thiếu tài nguyên " + path);
            }
            return in.readAllBytes();
        }
    }
}
