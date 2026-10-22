package com.nhom01.imagerec.common;

/** Hằng số dùng chung của giao thức, Client và Server phải giống hệt nhau. */
public final class Protocol {

    public static final int DEFAULT_PORT = 5000;

    /** Dung lượng ảnh tối đa Client được gửi (10 MB). */
    public static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    /** Kích thước body tối đa của một message: ảnh + tên file và các trường đi kèm. */
    public static final int MAX_BODY_BYTES = MAX_IMAGE_BYTES + 64 * 1024;

    /**
     * Số điểm ảnh tối đa (rộng × cao) Server chịu giải mã. Chặn "bom ảnh": file nén nhỏ nhưng giải nén
     * ra ảnh khổng lồ làm hết RAM. Server phải kiểm tra kích thước trước khi giải mã toàn bộ ảnh.
     */
    public static final long MAX_IMAGE_PIXELS = 40_000_000L;

    /** Client gửi PING theo chu kỳ này. */
    public static final int PING_INTERVAL_MS = 15_000;

    /** Server đóng kết nối nếu không nhận được gì trong khoảng này (dùng cho Socket.setSoTimeout). */
    public static final int READ_TIMEOUT_MS = 60_000;

    private Protocol() {
    }
}
