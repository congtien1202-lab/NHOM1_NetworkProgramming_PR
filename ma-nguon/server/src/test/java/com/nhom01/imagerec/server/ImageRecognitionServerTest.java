package com.nhom01.imagerec.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.ErrorCode;
import com.nhom01.imagerec.common.Message;
import com.nhom01.imagerec.common.ProtocolCodec;
import com.nhom01.imagerec.server.detect.DetectionException;
import com.nhom01.imagerec.server.detect.FakeObjectDetector;
import com.nhom01.imagerec.server.detect.ObjectDetector;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ImageRecognitionServerTest {

    private static final int READ_TIMEOUT_MS = 5_000;
    private static final int DEFAULT_MAX_CLIENTS = 20;
    private static final int SHORT_TIMEOUT_MS = 300;
    private static final int BIG_WIDTH = 8000;
    private static final int BIG_HEIGHT = 6000;
    private static final long WAIT_LIMIT_MS = 3_000;
    private static final long POLL_MS = 20;

    private final SessionRegistry registry = new SessionRegistry();
    private final List<TestClient> clients = new ArrayList<>();
    private ImageRecognitionServer server;

    @AfterEach
    void tearDown() {
        clients.forEach(TestClient::close);
        if (server != null) {
            server.stop();
        }
    }

    private void startServer(ObjectDetector detector, int readTimeoutMs, int maxClients) throws IOException {
        server = new ImageRecognitionServer(0, detector, registry, readTimeoutMs, maxClients);
        server.start();
    }

    private void startServer() throws IOException {
        startServer(new FakeObjectDetector(), READ_TIMEOUT_MS, DEFAULT_MAX_CLIENTS);
    }

    private TestClient connect() throws IOException {
        TestClient client = new TestClient(server.port());
        clients.add(client);
        return client;
    }

    private TestClient connectAndGreet(String name) throws IOException {
        TestClient client = connect();
        assertThat(client.send(new Message.Hello(name))).isInstanceOf(Message.HelloOk.class);
        return client;
    }

    private static byte[] png(int width, int height, int type) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, type), "png", bytes);
        return bytes.toByteArray();
    }

    private static byte[] smallPng() throws IOException {
        return png(8, 8, BufferedImage.TYPE_INT_RGB);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_LIMIT_MS;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static ErrorCode errorCode(Message message) {
        assertThat(message).isInstanceOf(Message.ErrorReply.class);
        return ((Message.ErrorReply) message).code();
    }

    private static void assertConnectionClosed(TestClient client) throws IOException {
        try {
            client.receive();
            throw new AssertionError("server phải đóng kết nối");
        } catch (EOFException expected) {
            // đúng như mong đợi
        }
    }

    @Test
    void helloThenDetectReturnsFixedDetections() throws Exception {
        startServer();
        TestClient client = connectAndGreet("Linh-PC");

        Message reply = client.send(new Message.DetectRequest("cat.png", smallPng()));

        assertThat(reply).isInstanceOf(Message.DetectResult.class);
        List<Detection> detections = ((Message.DetectResult) reply).result().detections();
        assertThat(detections).isEqualTo(FakeObjectDetector.FIXED_DETECTIONS);
    }

    @Test
    void notAnImageGivesBadImageAndSessionContinues() throws Exception {
        startServer();
        TestClient client = connectAndGreet("a");

        Message bad = client.send(new Message.DetectRequest("x.txt", "hello".getBytes()));
        Message good = client.send(new Message.DetectRequest("cat.png", smallPng()));

        assertThat(errorCode(bad)).isEqualTo(ErrorCode.BAD_IMAGE);
        assertThat(good).isInstanceOf(Message.DetectResult.class);
    }

    @Test
    void imageWithTooManyPixelsGivesImageTooLarge() throws Exception {
        startServer();
        TestClient client = connectAndGreet("a");

        byte[] huge = png(BIG_WIDTH, BIG_HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        Message reply = client.send(new Message.DetectRequest("big.png", huge));

        assertThat(errorCode(reply)).isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
    }

    @Test
    void connectionBeyondMaxClientsGetsServerBusy() throws Exception {
        startServer(new FakeObjectDetector(), READ_TIMEOUT_MS, 2);
        connectAndGreet("a");
        connectAndGreet("b");

        TestClient third = connect();

        assertThat(errorCode(third.receive())).isEqualTo(ErrorCode.SERVER_BUSY);
    }

    @Test
    void firstMessageNotHelloGivesNotGreetedAndClosesConnection() throws Exception {
        startServer();
        TestClient client = connect();

        assertThat(errorCode(client.send(new Message.Ping()))).isEqualTo(ErrorCode.NOT_GREETED);
        assertConnectionClosed(client);
    }

    @Test
    void secondHelloGivesBadMessageAndKeepsConnection() throws Exception {
        startServer();
        TestClient client = connectAndGreet("a");

        assertThat(errorCode(client.send(new Message.Hello("again")))).isEqualTo(ErrorCode.BAD_MESSAGE);
        assertThat(client.send(new Message.Ping())).isInstanceOf(Message.Pong.class);
    }

    @Test
    void detectorFailureGivesDetectionFailed() throws Exception {
        ObjectDetector failing = new ObjectDetector() {
            @Override
            public List<Detection> detect(BufferedImage image) throws DetectionException {
                throw new DetectionException("model lỗi", new IllegalStateException("boom"));
            }

            @Override
            public void close() {
            }
        };
        startServer(failing, READ_TIMEOUT_MS, DEFAULT_MAX_CLIENTS);
        TestClient client = connectAndGreet("a");

        Message reply = client.send(new Message.DetectRequest("cat.png", smallPng()));

        assertThat(errorCode(reply)).isEqualTo(ErrorCode.DETECTION_FAILED);
        assertThat(client.send(new Message.Ping())).isInstanceOf(Message.Pong.class);
    }

    @Test
    void pingGetsPong() throws Exception {
        startServer();
        TestClient client = connectAndGreet("a");

        assertThat(client.send(new Message.Ping())).isInstanceOf(Message.Pong.class);
    }

    @Test
    void silentClientIsDisconnectedAfterReadTimeout() throws Exception {
        startServer(new FakeObjectDetector(), SHORT_TIMEOUT_MS, DEFAULT_MAX_CLIENTS);
        TestClient client = connectAndGreet("quiet");
        assertThat(registry.snapshot()).hasSize(1);

        awaitTrue(() -> registry.snapshot().isEmpty());
        assertConnectionClosed(client);
    }

    @Test
    void fiveConcurrentClientsEachGetTheirResult() throws Exception {
        startServer();
        int count = 5;
        byte[] image = smallPng();
        CountDownLatch greeted = new CountDownLatch(count);
        CountDownLatch go = new CountDownLatch(1);
        List<Message> replies = Collections.synchronizedList(new ArrayList<>());
        List<TestClient> five = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestClient client = connect();
            five.add(client);
            String name = "client-" + i;
            Thread thread = new Thread(() -> {
                try {
                    client.send(new Message.Hello(name));
                    greeted.countDown();
                    go.await();
                    replies.add(client.send(new Message.DetectRequest(name + ".png", image)));
                } catch (Exception e) {
                    replies.add(new Message.ErrorReply(ErrorCode.BAD_MESSAGE, String.valueOf(e)));
                }
            });
            threads.add(thread);
            thread.start();
        }
        assertThat(greeted.await(WAIT_LIMIT_MS, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(registry.snapshot()).extracting(SessionInfo::id).doesNotHaveDuplicates().hasSize(count);
        go.countDown();
        for (Thread thread : threads) {
            thread.join(WAIT_LIMIT_MS);
        }

        assertThat(replies).hasSize(count).allSatisfy(reply -> {
            assertThat(reply).isInstanceOf(Message.DetectResult.class);
            assertThat(((Message.DetectResult) reply).result().detections())
                    .isEqualTo(FakeObjectDetector.FIXED_DETECTIONS);
        });
        awaitTrue(() -> registry.snapshot().stream().allMatch(s -> s.imagesProcessed() == 1));

        for (TestClient client : five) {
            client.sendOnly(new Message.Bye());
        }
        awaitTrue(() -> registry.snapshot().isEmpty());
    }

    private static final class TestClient implements AutoCloseable {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;

        TestClient(int port) throws IOException {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout((int) WAIT_LIMIT_MS * 2);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        }

        void sendOnly(Message message) throws IOException {
            ProtocolCodec.write(out, message);
        }

        Message receive() throws IOException {
            return ProtocolCodec.read(in);
        }

        Message send(Message message) throws IOException {
            sendOnly(message);
            return receive();
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // đóng khi dọn dẹp test
            }
        }
    }
}
