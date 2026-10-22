package com.nhom01.imagerec.common;

import java.util.Arrays;
import java.util.Objects;

/**
 * Một message của giao thức. Mỗi loại lệnh là một record riêng, nên bên nhận dùng
 * {@code instanceof} để xử lý theo kiểu, không phải tự đọc byte.
 *
 * <p>Mã hóa/giải mã trên đường truyền do {@link ProtocolCodec} đảm nhiệm.
 */
public sealed interface Message {

    Command command();

    /** C→S: mở phiên, gửi tên hiển thị của Client. */
    record Hello(String clientName) implements Message {
        public Hello {
            Objects.requireNonNull(clientName);
        }

        @Override
        public Command command() {
            return Command.HELLO;
        }
    }

    /** S→C: chấp nhận phiên. */
    record HelloOk(int sessionId) implements Message {
        @Override
        public Command command() {
            return Command.HELLO_OK;
        }
    }

    /** C→S: yêu cầu nhận dạng một ảnh JPG/PNG, tối đa {@link Protocol#MAX_IMAGE_BYTES}. */
    record DetectRequest(String fileName, byte[] imageBytes) implements Message {
        public DetectRequest {
            Objects.requireNonNull(fileName);
            Objects.requireNonNull(imageBytes);
            if (imageBytes.length > Protocol.MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("Ảnh vượt quá " + Protocol.MAX_IMAGE_BYTES + " byte");
            }
        }

        @Override
        public Command command() {
            return Command.DETECT_REQUEST;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof DetectRequest that
                    && fileName.equals(that.fileName)
                    && Arrays.equals(imageBytes, that.imageBytes);
        }

        @Override
        public int hashCode() {
            return 31 * fileName.hashCode() + Arrays.hashCode(imageBytes);
        }

        @Override
        public String toString() {
            return "DetectRequest[fileName=" + fileName + ", imageBytes=" + imageBytes.length + " bytes]";
        }
    }

    /** S→C: kết quả nhận dạng. */
    record DetectResult(DetectionResult result) implements Message {
        public DetectResult {
            Objects.requireNonNull(result);
        }

        @Override
        public Command command() {
            return Command.DETECT_RESULT;
        }
    }

    /** S→C: yêu cầu trước đó thất bại. */
    record ErrorReply(ErrorCode code, String message) implements Message {
        public ErrorReply {
            Objects.requireNonNull(code);
            Objects.requireNonNull(message);
        }

        @Override
        public Command command() {
            return Command.ERROR;
        }
    }

    /** C→S: kiểm tra kết nối còn sống. */
    record Ping() implements Message {
        @Override
        public Command command() {
            return Command.PING;
        }
    }

    /** S→C: trả lời PING. */
    record Pong() implements Message {
        @Override
        public Command command() {
            return Command.PONG;
        }
    }

    /** C→S: Client chủ động kết thúc phiên. */
    record Bye() implements Message {
        @Override
        public Command command() {
            return Command.BYE;
        }
    }
}
