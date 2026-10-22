package com.nhom01.imagerec.server;

import java.time.LocalTime;

/** Thông tin một phiên Client đang kết nối. Bất biến: registry thay bản mới khi số ảnh tăng. */
public record SessionInfo(int id, String clientName, String remoteAddress, LocalTime connectedAt,
                          int imagesProcessed) {

    SessionInfo withOneMoreImage() {
        return new SessionInfo(id, clientName, remoteAddress, connectedAt, imagesProcessed + 1);
    }
}
