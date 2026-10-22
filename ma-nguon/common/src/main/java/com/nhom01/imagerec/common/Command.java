package com.nhom01.imagerec.common;

/** Mã lệnh nằm ở byte đầu tiên của mỗi message trên đường truyền. */
public enum Command {
    HELLO(1),
    HELLO_OK(2),
    DETECT_REQUEST(3),
    DETECT_RESULT(4),
    ERROR(5),
    PING(6),
    PONG(7),
    BYE(8);

    private final byte code;

    Command(int code) {
        this.code = (byte) code;
    }

    public byte code() {
        return code;
    }

    public static Command fromCode(byte code) throws ProtocolException {
        for (Command command : values()) {
            if (command.code == code) {
                return command;
            }
        }
        throw new ProtocolException("Mã lệnh không hợp lệ: " + code);
    }
}
