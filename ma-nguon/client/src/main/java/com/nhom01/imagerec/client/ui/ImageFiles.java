package com.nhom01.imagerec.client.ui;

import com.nhom01.imagerec.common.Protocol;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import javax.imageio.ImageIO;

/** Kiểm tra file ảnh do người dùng chọn trước khi gửi lên Server. */
final class ImageFiles {

    static final String INVALID_MESSAGE = "Ảnh phải là JPG/PNG và không quá 10 MB";

    private ImageFiles() {
    }

    /** Trả thông báo lỗi tiếng Việt nếu file không dùng được, rỗng nếu hợp lệ. */
    static Optional<String> validate(File file) {
        if (!hasImageExtension(file.getName()) || !file.isFile() || file.length() > Protocol.MAX_IMAGE_BYTES) {
            return Optional.of(INVALID_MESSAGE);
        }
        try {
            if (ImageIO.read(file) == null) {
                return Optional.of("Không đọc được nội dung ảnh. " + INVALID_MESSAGE);
            }
        } catch (IOException e) {
            return Optional.of("Không đọc được nội dung ảnh. " + INVALID_MESSAGE);
        }
        return Optional.empty();
    }

    private static boolean hasImageExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }
}
