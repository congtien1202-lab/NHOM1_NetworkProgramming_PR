package com.nhom01.imagerec.client.net;

import com.nhom01.imagerec.common.DetectionResult;
import com.nhom01.imagerec.common.Message;
import com.nhom01.imagerec.common.ProtocolCodec;
import com.nhom01.imagerec.common.ProtocolException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Kết nối TCP tới Server, giấu toàn bộ chi tiết socket và giao thức.
 *
 * <p>Hợp đồng:
 * <ul>
 *   <li>Mọi method thread-safe và tuần tự: một yêu cầu, chờ một phản hồi (PING không chen giữa DETECT).</li>
 *   <li>Method mạng là blocking: GUI phải gọi từ SwingWorker, không gọi trên EDT.</li>
 *   <li>{@link IOException}: mất kết nối; sau đó {@link #isConnected()} trả false.</li>
 *   <li>{@link ServerErrorException}: Server từ chối yêu cầu, kết nối vẫn dùng tiếp được
 *       (riêng khi {@code connect} bị từ chối thì chưa có kết nối).</li>
 * </ul>
 */
public class ServerConnection implements AutoCloseable {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int RESPONSE_TIMEOUT_MS = 30_000;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    /** Mở socket, gửi HELLO, chờ HELLO_OK. Trả về session id. */
    public synchronized int connect(String host, int port, String clientName)
            throws IOException, ServerErrorException {
        closeQuietly();
        Socket candidate = new Socket();
        try {
            candidate.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            candidate.setSoTimeout(RESPONSE_TIMEOUT_MS);
            socket = candidate;
            in = new DataInputStream(candidate.getInputStream());
            out = new DataOutputStream(candidate.getOutputStream());
            Message reply = exchange(new Message.Hello(clientName));
            if (reply instanceof Message.HelloOk ok) {
                return ok.sessionId();
            }
            throw unexpected(reply);
        } catch (IOException | ServerErrorException | RuntimeException e) {
            // Server từ chối hay lỗi mạng đều không để lại kết nối dở dang.
            closeQuietly();
            candidate.close();
            throw e;
        }
    }

    /** Gửi DETECT_REQUEST, chờ DETECT_RESULT. */
    public synchronized DetectionResult detect(String fileName, byte[] imageBytes)
            throws IOException, ServerErrorException {
        requireConnected();
        try {
            Message reply = exchange(new Message.DetectRequest(fileName, imageBytes));
            if (reply instanceof Message.DetectResult result) {
                return result.result();
            }
            throw unexpected(reply);
        } catch (IOException e) {
            closeQuietly();
            throw e;
        }
    }

    /** Gửi PING, chờ PONG. Trả false (không ném) nếu kết nối đã hỏng. */
    public synchronized boolean ping() {
        if (!isConnected()) {
            return false;
        }
        try {
            if (exchange(new Message.Ping()) instanceof Message.Pong) {
                return true;
            }
        } catch (IOException | ServerErrorException e) {
            // Kết nối hỏng hay Server trả lỗi cho PING đều coi như mất kết nối.
        }
        closeQuietly();
        return false;
    }

    public synchronized boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    /** Gửi BYE (nếu còn kết nối) rồi đóng socket. Gọi nhiều lần không lỗi. */
    @Override
    public synchronized void close() {
        if (isConnected()) {
            try {
                ProtocolCodec.write(out, new Message.Bye());
            } catch (IOException e) {
                // Kết nối đã hỏng: không còn ai nhận BYE, chỉ cần đóng socket.
            }
        }
        closeQuietly();
    }

    private Message exchange(Message request) throws IOException, ServerErrorException {
        ProtocolCodec.write(out, request);
        Message reply = ProtocolCodec.read(in);
        if (reply instanceof Message.ErrorReply error) {
            throw new ServerErrorException(error.code(), error.message());
        }
        return reply;
    }

    private void requireConnected() throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối tới Server");
        }
    }

    private static ProtocolException unexpected(Message reply) {
        return new ProtocolException("Server trả message không mong đợi: " + reply.command());
    }

    private void closeQuietly() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // Đóng socket thất bại không còn gì để xử lý thêm.
            }
        }
        socket = null;
        in = null;
        out = null;
    }
}
