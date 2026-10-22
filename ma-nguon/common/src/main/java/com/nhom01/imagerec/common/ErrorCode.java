package com.nhom01.imagerec.common;

/** Lý do Server trả về ERROR. */
public enum ErrorCode {
    /** Body ảnh không giải mã được thành JPG/PNG. */
    BAD_IMAGE(1),
    /** Ảnh vượt quá {@link Protocol#MAX_IMAGE_BYTES}. */
    IMAGE_TOO_LARGE(2),
    /** Client gửi lệnh khác trước khi HELLO. */
    NOT_GREETED(3),
    /** Mô hình nhận dạng gặp lỗi khi xử lý ảnh. */
    DETECTION_FAILED(4),
    /** Message sai cấu trúc hoặc lệnh không hợp lệ theo chiều Client → Server. */
    BAD_MESSAGE(5),
    /** Server đã đủ {@code maxClients} kết nối; Client thử lại sau. */
    SERVER_BUSY(6);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ErrorCode fromCode(int code) throws ProtocolException {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        throw new ProtocolException("Mã lỗi không hợp lệ: " + code);
    }
}
