package com.nhom01.imagerec.client.net;

import com.nhom01.imagerec.common.ErrorCode;

public class ServerErrorException extends Exception {
    private final ErrorCode code;

    public ServerErrorException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
