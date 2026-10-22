package com.nhom01.imagerec.common;

import java.io.IOException;

/** Dữ liệu nhận được không đúng giao thức (mã lệnh lạ, độ dài sai, body hỏng). */
public class ProtocolException extends IOException {

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
