package com.nhom01.imagerec.server.detect;

/** Mô hình nhận dạng không xử lý được ảnh. */
public class DetectionException extends Exception {

    public DetectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
