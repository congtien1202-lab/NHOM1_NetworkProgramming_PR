package com.nhom01.imagerec.client.net;

import com.nhom01.imagerec.common.DetectionResult;
import java.io.IOException;

/**
 * Stub kết nối mạng phía Client (sẽ được cài đặt trong PLAN-C / Issue 6).
 */
public class ServerConnection implements AutoCloseable {

    public synchronized int connect(String host, int port, String clientName)
            throws IOException, ServerErrorException {
        throw new UnsupportedOperationException("PLAN-C");
    }

    public synchronized DetectionResult detect(String fileName, byte[] imageBytes)
            throws IOException, ServerErrorException {
        throw new UnsupportedOperationException("PLAN-C");
    }

    public synchronized boolean ping() {
        return false;
    }

    public synchronized boolean isConnected() {
        return false;
    }

    @Override
    public synchronized void close() {
    }
}
