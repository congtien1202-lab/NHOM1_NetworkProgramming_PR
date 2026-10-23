package com.nhom01.imagerec.server;

import com.nhom01.imagerec.common.ErrorCode;
import com.nhom01.imagerec.common.Message;
import com.nhom01.imagerec.common.ProtocolCodec;
import com.nhom01.imagerec.server.detect.ObjectDetector;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Lắng nghe TCP, mỗi kết nối chạy một {@link ClientHandler} trong pool giới hạn {@code maxClients} luồng.
 * Kết nối vượt giới hạn nhận SERVER_BUSY rồi bị đóng (không xếp hàng, để chặn tràn RAM).
 */
public final class ImageRecognitionServer {

    private static final Logger LOG = Logger.getLogger(ImageRecognitionServer.class.getName());
    private static final int BUSY_WRITE_TIMEOUT_MS = 2_000;

    private final int requestedPort;
    private final ObjectDetector detector;
    private final SessionRegistry registry;
    private final int readTimeoutMs;
    private final int maxClients;
    private final AtomicInteger activeConnections = new AtomicInteger();
    private final Set<Socket> openSockets = ConcurrentHashMap.newKeySet();
    private volatile ServerSocket serverSocket;
    private ExecutorService workers;
    private Thread acceptThread;

    public ImageRecognitionServer(int port, ObjectDetector detector, SessionRegistry registry,
                                  int readTimeoutMs, int maxClients) {
        this.requestedPort = port;
        this.detector = detector;
        this.registry = registry;
        this.readTimeoutMs = readTimeoutMs;
        this.maxClients = maxClients;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        workers = Executors.newFixedThreadPool(maxClients);
        acceptThread = new Thread(this::acceptLoop, "accept-loop");
        acceptThread.start();
        LOG.info("Server listening on port " + port());
    }

    /** Port thật đang lắng nghe (khác tham số khi truyền 0). */
    public int port() {
        return serverSocket.getLocalPort();
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOG.warning("Lỗi khi đóng ServerSocket: " + e.getMessage());
        }
        openSockets.forEach(ImageRecognitionServer::closeQuietly);
        workers.shutdownNow();
        LOG.info("Server stopped");
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                dispatch(socket);
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    LOG.warning("Lỗi accept: " + e.getMessage());
                }
            }
        }
    }

    private void dispatch(Socket socket) {
        if (activeConnections.incrementAndGet() > maxClients) {
            activeConnections.decrementAndGet();
            rejectBusy(socket);
            return;
        }
        openSockets.add(socket);
        ClientHandler handler = new ClientHandler(socket, detector, registry, readTimeoutMs);
        workers.execute(() -> {
            try {
                handler.run();
            } finally {
                openSockets.remove(socket);
                activeConnections.decrementAndGet();
            }
        });
    }

    private void rejectBusy(Socket socket) {
        LOG.warning("Từ chối " + socket.getInetAddress() + ": đã đủ " + maxClients + " client");
        try (socket) {
            socket.setSoTimeout(BUSY_WRITE_TIMEOUT_MS);
            ProtocolCodec.write(new DataOutputStream(socket.getOutputStream()),
                    new Message.ErrorReply(ErrorCode.SERVER_BUSY, "Server đã đủ " + maxClients + " client"));
        } catch (IOException e) {
            LOG.fine("Không gửi được SERVER_BUSY: " + e.getMessage());
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.fine("Lỗi đóng socket: " + e.getMessage());
        }
    }
}
