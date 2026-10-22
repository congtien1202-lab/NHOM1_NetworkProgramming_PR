package com.nhom01.imagerec.server;

import com.nhom01.imagerec.common.Detection;
import com.nhom01.imagerec.common.DetectionResult;
import com.nhom01.imagerec.common.ErrorCode;
import com.nhom01.imagerec.common.Message;
import com.nhom01.imagerec.common.Protocol;
import com.nhom01.imagerec.common.ProtocolCodec;
import com.nhom01.imagerec.common.ProtocolException;
import com.nhom01.imagerec.server.detect.ObjectDetector;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Phục vụ một Client trên một luồng riêng, từ HELLO đến BYE hoặc khi mất kết nối. */
final class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ObjectDetector detector;
    private final SessionRegistry registry;
    private final int readTimeoutMs;
    private SessionInfo session;

    ClientHandler(Socket socket, ObjectDetector detector, SessionRegistry registry, int readTimeoutMs) {
        this.socket = socket;
        this.detector = detector;
        this.registry = registry;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public void run() {
        try (socket) {
            socket.setSoTimeout(readTimeoutMs);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            converse(in, out);
        } catch (SocketTimeoutException e) {
            LOG.info(label() + " timed out");
        } catch (EOFException e) {
            LOG.info(label() + " connection lost");
        } catch (ProtocolException e) {
            LOG.warning(label() + " protocol error: " + e.getMessage());
            replyQuietly(ErrorCode.BAD_MESSAGE, e.getMessage());
        } catch (IOException e) {
            LOG.info(label() + " connection lost");
        } finally {
            if (session != null) {
                registry.remove(session.id());
            }
        }
    }

    /** @return khi Client kết thúc phiên hợp lệ (BYE) hoặc bị từ chối bắt tay; ngoại lệ I/O đẩy lên run(). */
    private void converse(DataInputStream in, DataOutputStream out) throws IOException {
        Message first = ProtocolCodec.read(in);
        if (!(first instanceof Message.Hello hello)) {
            reply(out, ErrorCode.NOT_GREETED, "Phải gửi HELLO trước");
            return;
        }
        session = registry.register(hello.clientName(), String.valueOf(socket.getInetAddress()));
        ProtocolCodec.write(out, new Message.HelloOk(session.id()));
        LOG.info("Client #" + session.id() + " (" + hello.clientName() + ", " + session.remoteAddress()
                + ") connected");

        while (true) {
            Message message = ProtocolCodec.read(in);
            if (message instanceof Message.Bye) {
                LOG.info(label() + " disconnected");
                return;
            }
            handle(out, message);
        }
    }

    private void handle(DataOutputStream out, Message message) throws IOException {
        if (message instanceof Message.Ping) {
            ProtocolCodec.write(out, new Message.Pong());
        } else if (message instanceof Message.DetectRequest request) {
            detect(out, request);
        } else {
            reply(out, ErrorCode.BAD_MESSAGE, "Lệnh không hợp lệ: " + message.command());
        }
    }

    private void detect(DataOutputStream out, Message.DetectRequest request) throws IOException {
        byte[] bytes = request.imageBytes();
        long pixels = readPixelCount(bytes);
        if (pixels < 0) {
            reply(out, ErrorCode.BAD_IMAGE, "Không đọc được ảnh JPG/PNG");
            return;
        }
        if (pixels > Protocol.MAX_IMAGE_PIXELS) {
            reply(out, ErrorCode.IMAGE_TOO_LARGE, "Ảnh quá lớn: " + pixels + " điểm ảnh, tối đa "
                    + Protocol.MAX_IMAGE_PIXELS);
            return;
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            reply(out, ErrorCode.BAD_IMAGE, "Không đọc được ảnh JPG/PNG");
            return;
        }

        long start = System.nanoTime();
        List<Detection> detections;
        try {
            detections = detector.detect(image);
        } catch (Exception e) {
            LOG.log(Level.WARNING, label() + " detection failed for " + request.fileName(), e);
            reply(out, ErrorCode.DETECTION_FAILED, "Nhận dạng thất bại: " + e.getMessage());
            return;
        }
        long millis = (System.nanoTime() - start) / 1_000_000;

        ProtocolCodec.write(out, new Message.DetectResult(new DetectionResult(millis, detections)));
        registry.recordImage(session.id());
        LOG.info(label() + " detect " + request.fileName() + ": " + detections.size() + " objects in "
                + millis + " ms");
    }

    /**
     * Đọc kích thước từ header ảnh, chưa giải mã điểm ảnh (chống "bom ảnh").
     *
     * @return số điểm ảnh, hoặc -1 nếu không phải ảnh đọc được
     */
    private static long readPixelCount(byte[] bytes) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) {
                return -1;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return -1;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                return (long) reader.getWidth(0) * reader.getHeight(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    private void reply(DataOutputStream out, ErrorCode code, String text) throws IOException {
        ProtocolCodec.write(out, new Message.ErrorReply(code, text));
    }

    private void replyQuietly(ErrorCode code, String text) {
        try {
            reply(new DataOutputStream(socket.getOutputStream()), code, text);
        } catch (IOException e) {
            LOG.fine(label() + " không gửi được ErrorReply: " + e.getMessage());
        }
    }

    private String label() {
        return session == null ? "Client (no HELLO yet)" : "Client #" + session.id();
    }
}
