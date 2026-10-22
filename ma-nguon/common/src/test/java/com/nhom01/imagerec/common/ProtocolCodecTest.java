package com.nhom01.imagerec.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolCodecTest {

    private static Message roundTrip(Message message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ProtocolCodec.write(new DataOutputStream(bytes), message);
        return ProtocolCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }

    private static DataInputStream rawFrame(int commandCode, int length, byte[] body) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(commandCode);
        out.writeInt(length);
        out.write(body);
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    @Test
    void helloCarriesClientName() throws IOException {
        assertThat(roundTrip(new Message.Hello("Máy của Linh")))
                .isEqualTo(new Message.Hello("Máy của Linh"));
    }

    @Test
    void helloOkCarriesSessionId() throws IOException {
        assertThat(roundTrip(new Message.HelloOk(42))).isEqualTo(new Message.HelloOk(42));
    }

    @Test
    void detectRequestCarriesFileNameAndImageBytes() throws IOException {
        byte[] image = {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, 3};

        Message read = roundTrip(new Message.DetectRequest("cat.png", image));

        assertThat(read).isInstanceOf(Message.DetectRequest.class);
        Message.DetectRequest request = (Message.DetectRequest) read;
        assertThat(request.fileName()).isEqualTo("cat.png");
        assertThat(request.imageBytes()).containsExactly(image);
    }

    @Test
    void detectResultCarriesEveryDetection() throws IOException {
        DetectionResult result = new DetectionResult(123, List.of(
                new Detection("person", 0.92, 0.1, 0.2, 0.3, 0.4),
                new Detection("dog", 0.81, 0.5, 0.5, 0.25, 0.25)));

        assertThat(roundTrip(new Message.DetectResult(result))).isEqualTo(new Message.DetectResult(result));
    }

    @Test
    void detectResultWithNoObjectsIsValid() throws IOException {
        DetectionResult empty = new DetectionResult(5, List.of());

        assertThat(roundTrip(new Message.DetectResult(empty))).isEqualTo(new Message.DetectResult(empty));
    }

    @Test
    void errorReplyCarriesCodeAndMessage() throws IOException {
        Message.ErrorReply error = new Message.ErrorReply(ErrorCode.BAD_IMAGE, "Không đọc được ảnh");

        assertThat(roundTrip(error)).isEqualTo(error);
    }

    @Test
    void everyErrorCodeRoundTrips() throws IOException {
        for (ErrorCode code : ErrorCode.values()) {
            Message.ErrorReply error = new Message.ErrorReply(code, code.name());

            assertThat(roundTrip(error)).isEqualTo(error);
        }
    }

    @Test
    void serverBusyIsAnErrorCode() {
        assertThat(ErrorCode.SERVER_BUSY.code()).isEqualTo(6);
    }

    @Test
    void emptyBodyMessagesRoundTrip() throws IOException {
        assertThat(roundTrip(new Message.Ping())).isEqualTo(new Message.Ping());
        assertThat(roundTrip(new Message.Pong())).isEqualTo(new Message.Pong());
        assertThat(roundTrip(new Message.Bye())).isEqualTo(new Message.Bye());
    }

    @Test
    void severalMessagesOnOneStreamAreReadInOrder() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        ProtocolCodec.write(out, new Message.Hello("a"));
        ProtocolCodec.write(out, new Message.Ping());
        ProtocolCodec.write(out, new Message.Bye());
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));

        assertThat(ProtocolCodec.read(in)).isEqualTo(new Message.Hello("a"));
        assertThat(ProtocolCodec.read(in)).isEqualTo(new Message.Ping());
        assertThat(ProtocolCodec.read(in)).isEqualTo(new Message.Bye());
    }

    @Test
    void largestAllowedImageIsAccepted() throws IOException {
        byte[] image = new byte[Protocol.MAX_IMAGE_BYTES];

        Message read = roundTrip(new Message.DetectRequest("big.jpg", image));

        assertThat(((Message.DetectRequest) read).imageBytes()).hasSize(Protocol.MAX_IMAGE_BYTES);
    }

    @Test
    void imageOverLimitIsRejectedBeforeSending() {
        byte[] image = new byte[Protocol.MAX_IMAGE_BYTES + 1];

        assertThatThrownBy(() -> new Message.DetectRequest("huge.jpg", image))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readRejectsUnknownCommand() throws IOException {
        DataInputStream in = rawFrame(99, 0, new byte[0]);

        assertThatThrownBy(() -> ProtocolCodec.read(in)).isInstanceOf(ProtocolException.class);
    }

    @Test
    void readRejectsNegativeLength() throws IOException {
        DataInputStream in = rawFrame(Command.PING.code(), -1, new byte[0]);

        assertThatThrownBy(() -> ProtocolCodec.read(in)).isInstanceOf(ProtocolException.class);
    }

    @Test
    void readRejectsBodyLargerThanLimitWithoutReadingIt() throws IOException {
        DataInputStream in = rawFrame(Command.DETECT_REQUEST.code(), Protocol.MAX_BODY_BYTES + 1, new byte[0]);

        assertThatThrownBy(() -> ProtocolCodec.read(in)).isInstanceOf(ProtocolException.class);
    }

    @Test
    void readRejectsImageOverLimitInsideAnAcceptedBody() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(body);
        out.writeUTF("x.jpg");
        out.writeInt(Protocol.MAX_IMAGE_BYTES + 1);
        out.write(new byte[Protocol.MAX_IMAGE_BYTES + 1]);
        DataInputStream in = rawFrame(Command.DETECT_REQUEST.code(), body.size(), body.toByteArray());

        assertThatThrownBy(() -> ProtocolCodec.read(in)).isInstanceOf(ProtocolException.class);
    }

    @Test
    void readRejectsMalformedBody() throws IOException {
        DataInputStream in = rawFrame(Command.DETECT_RESULT.code(), 2, new byte[] {1, 2});

        assertThatThrownBy(() -> ProtocolCodec.read(in)).isInstanceOf(ProtocolException.class);
    }

    @Test
    void readRejectsTrailingBytesInBody() throws IOException {
        DataInputStream in = rawFrame(Command.PING.code(), 1, new byte[] {0});

        assertThatThrownBy(() -> ProtocolCodec.read(in)).isInstanceOf(ProtocolException.class);
    }

    @Test
    void readOnClosedStreamThrowsEof() {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> ProtocolCodec.read(in)).isInstanceOf(EOFException.class);
    }

    @Test
    void readOnTruncatedBodyThrowsEof() throws IOException {
        DataInputStream in = rawFrame(Command.HELLO.code(), 10, new byte[3]);

        assertThatThrownBy(() -> ProtocolCodec.read(in)).isInstanceOf(EOFException.class);
    }
}
