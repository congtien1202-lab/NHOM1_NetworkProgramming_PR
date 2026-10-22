package com.nhom01.imagerec.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Đọc/ghi {@link Message} trên luồng TCP.
 *
 * <p>Khung message: [command: 1 byte][length: 4 byte int][body: length byte], big-endian.
 * Body được giải mã ngay trong {@link #read}, nên message trả về luôn hợp lệ.
 *
 * <p>Lỗi khi đọc: {@link EOFException} nếu luồng đóng (giữa chừng hoặc trước message);
 * {@link ProtocolException} nếu dữ liệu sai giao thức. Hai lớp không thread-safe trên cùng một luồng:
 * mỗi luồng chỉ nên có một bên đọc và một bên ghi tại một thời điểm.
 */
public final class ProtocolCodec {

    private ProtocolCodec() {
    }

    public static void write(DataOutputStream out, Message message) throws IOException {
        byte[] body = encodeBody(message);
        out.writeByte(message.command().code());
        out.writeInt(body.length);
        out.write(body);
        out.flush();
    }

    public static Message read(DataInputStream in) throws IOException {
        Command command = Command.fromCode(in.readByte());
        int length = in.readInt();
        if (length < 0 || length > Protocol.MAX_BODY_BYTES) {
            throw new ProtocolException("Độ dài body không hợp lệ: " + length);
        }
        byte[] body = new byte[length];
        in.readFully(body);
        return decodeBody(command, body);
    }

    private static byte[] encodeBody(Message message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        if (message instanceof Message.Hello hello) {
            out.writeUTF(hello.clientName());
        } else if (message instanceof Message.HelloOk ok) {
            out.writeInt(ok.sessionId());
        } else if (message instanceof Message.DetectRequest request) {
            out.writeUTF(request.fileName());
            out.writeInt(request.imageBytes().length);
            out.write(request.imageBytes());
        } else if (message instanceof Message.DetectResult result) {
            writeResult(out, result.result());
        } else if (message instanceof Message.ErrorReply error) {
            out.writeInt(error.code().code());
            out.writeUTF(error.message());
        }
        // Ping, Pong, Bye: body rỗng.
        return bytes.toByteArray();
    }

    private static Message decodeBody(Command command, byte[] body) throws ProtocolException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(body));
        try {
            Message message = switch (command) {
                case HELLO -> new Message.Hello(in.readUTF());
                case HELLO_OK -> new Message.HelloOk(in.readInt());
                case DETECT_REQUEST -> readDetectRequest(in);
                case DETECT_RESULT -> new Message.DetectResult(readResult(in));
                case ERROR -> new Message.ErrorReply(ErrorCode.fromCode(in.readInt()), in.readUTF());
                case PING -> new Message.Ping();
                case PONG -> new Message.Pong();
                case BYE -> new Message.Bye();
            };
            if (in.available() > 0) {
                throw new ProtocolException("Body của " + command + " thừa " + in.available() + " byte");
            }
            return message;
        } catch (ProtocolException e) {
            throw e;
        } catch (IOException | IllegalArgumentException e) {
            throw new ProtocolException("Body của " + command + " sai cấu trúc", e);
        }
    }

    private static Message.DetectRequest readDetectRequest(DataInputStream in) throws IOException {
        String fileName = in.readUTF();
        int imageLength = in.readInt();
        if (imageLength < 0 || imageLength > Protocol.MAX_IMAGE_BYTES) {
            throw new ProtocolException("Kích thước ảnh không hợp lệ: " + imageLength);
        }
        byte[] image = new byte[imageLength];
        in.readFully(image);
        return new Message.DetectRequest(fileName, image);
    }

    private static void writeResult(DataOutputStream out, DetectionResult result) throws IOException {
        out.writeLong(result.processingMillis());
        out.writeInt(result.detections().size());
        for (Detection d : result.detections()) {
            out.writeUTF(d.label());
            out.writeDouble(d.confidence());
            out.writeDouble(d.x());
            out.writeDouble(d.y());
            out.writeDouble(d.width());
            out.writeDouble(d.height());
        }
    }

    private static DetectionResult readResult(DataInputStream in) throws IOException {
        long processingMillis = in.readLong();
        int count = in.readInt();
        if (count < 0 || count > 10_000) {
            throw new ProtocolException("Số đối tượng không hợp lệ: " + count);
        }
        List<Detection> detections = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            detections.add(new Detection(in.readUTF(), in.readDouble(),
                    in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble()));
        }
        return new DetectionResult(processingMillis, detections);
    }
}
