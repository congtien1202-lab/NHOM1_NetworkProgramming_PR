package com.nhom01.imagerec.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import com.nhom01.imagerec.common.ErrorCode;
import com.nhom01.imagerec.common.Message;
import com.nhom01.imagerec.common.ProtocolCodec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerConnectionTest {

    private static final int WAIT_SECONDS = 5;
    private static final String LOCALHOST = "localhost";

    /** Một kịch bản phía Server: nhận stream đã accept và đọc/ghi theo ý test. */
    @FunctionalInterface
    private interface Script {
        void run(DataInputStream in, DataOutputStream out, Socket socket) throws IOException;
    }

    private ServerSocket listener;
    private final BlockingQueue<Message> received = new LinkedBlockingQueue<>();
    private ServerConnection connection;

    @BeforeEach
    void openListener() throws IOException {
        listener = new ServerSocket(0);
        connection = new ServerConnection();
    }

    @AfterEach
    void cleanUp() throws IOException {
        connection.close();
        listener.close();
    }

    private void serve(Script script) {
        Thread thread = new Thread(() -> {
            try (Socket socket = listener.accept()) {
                script.run(new DataInputStream(socket.getInputStream()),
                        new DataOutputStream(socket.getOutputStream()), socket);
            } catch (IOException ignored) {
                // Client đóng socket là kịch bản bình thường của test.
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private Message read(DataInputStream in) throws IOException {
        Message message = ProtocolCodec.read(in);
        received.add(message);
        return message;
    }

    private int port() {
        return listener.getLocalPort();
    }

    @Test
    void connectSendsHelloWithNameAndReturnsSessionId() throws Exception {
        serve((in, out, socket) -> {
            read(in);
            ProtocolCodec.write(out, new Message.HelloOk(7));
        });

        int sessionId = connection.connect(LOCALHOST, port(), "may-01");

        assertEquals(7, sessionId);
        assertTrue(connection.isConnected());
        assertEquals(new Message.Hello("may-01"), received.poll(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void detectReturnsResultAndConnectionSurvivesServerError() throws Exception {
        DetectionResult expected = new DetectionResult(42, List.of(new Detection("dog", 0.9, 0.1, 0.2, 0.3, 0.4)));
        serve((in, out, socket) -> {
            read(in);
            ProtocolCodec.write(out, new Message.HelloOk(1));
            read(in);
            ProtocolCodec.write(out, new Message.ErrorReply(ErrorCode.BAD_IMAGE, "Không đọc được ảnh"));
            read(in);
            ProtocolCodec.write(out, new Message.DetectResult(expected));
        });
        connection.connect(LOCALHOST, port(), "c");

        ServerErrorException error = assertThrows(ServerErrorException.class,
                () -> connection.detect("bad.png", new byte[] {1, 2}));
        assertEquals(ErrorCode.BAD_IMAGE, error.code());
        assertEquals("Không đọc được ảnh", error.getMessage());
        assertTrue(connection.isConnected());

        DetectionResult actual = connection.detect("ok.png", new byte[] {3});
        assertEquals(expected, actual);
        received.poll(WAIT_SECONDS, TimeUnit.SECONDS); // Hello
        assertEquals(new Message.DetectRequest("bad.png", new byte[] {1, 2}), received.poll(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void connectRejectedWhenServerBusy() {
        serve((in, out, socket) -> {
            read(in);
            ProtocolCodec.write(out, new Message.ErrorReply(ErrorCode.SERVER_BUSY, "Server đang bận"));
        });

        ServerErrorException error = assertThrows(ServerErrorException.class,
                () -> connection.connect(LOCALHOST, port(), "c"));

        assertEquals(ErrorCode.SERVER_BUSY, error.code());
        assertFalse(connection.isConnected());
    }

    @Test
    void connectFailsOnUnexpectedReply() {
        serve((in, out, socket) -> {
            read(in);
            ProtocolCodec.write(out, new Message.Pong());
        });

        assertThrows(IOException.class, () -> connection.connect(LOCALHOST, port(), "c"));
        assertFalse(connection.isConnected());
    }

    @Test
    void pingReturnsTrueWhenServerAnswersPong() throws Exception {
        serve((in, out, socket) -> {
            read(in);
            ProtocolCodec.write(out, new Message.HelloOk(1));
            read(in);
            ProtocolCodec.write(out, new Message.Pong());
        });
        connection.connect(LOCALHOST, port(), "c");

        assertTrue(connection.ping());
        assertTrue(connection.isConnected());
    }

    @Test
    void pingReturnsFalseWhenServerClosesSocket() throws Exception {
        serve((in, out, socket) -> {
            read(in);
            ProtocolCodec.write(out, new Message.HelloOk(1));
        });
        connection.connect(LOCALHOST, port(), "c");

        assertFalse(connection.ping());
        assertFalse(connection.isConnected());
    }

    @Test
    void detectOnDroppedConnectionThrowsIoExceptionAndMarksDisconnected() throws Exception {
        serve((in, out, socket) -> {
            read(in);
            ProtocolCodec.write(out, new Message.HelloOk(1));
        });
        connection.connect(LOCALHOST, port(), "c");

        assertThrows(IOException.class, () -> connection.detect("a.png", new byte[] {1}));
        assertFalse(connection.isConnected());
    }

    @Test
    void closeSendsByeAndIsIdempotent() throws Exception {
        serve((in, out, socket) -> {
            read(in);
            ProtocolCodec.write(out, new Message.HelloOk(1));
            read(in);
        });
        connection.connect(LOCALHOST, port(), "c");

        connection.close();
        connection.close();

        assertFalse(connection.isConnected());
        received.poll(WAIT_SECONDS, TimeUnit.SECONDS); // Hello
        assertInstanceOf(Message.Bye.class, received.poll(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void connectToPortWithNoListenerThrowsIoException() throws IOException {
        int unusedPort = port();
        listener.close();

        assertThrows(IOException.class, () -> connection.connect(LOCALHOST, unusedPort, "c"));
        assertFalse(connection.isConnected());
    }
}
