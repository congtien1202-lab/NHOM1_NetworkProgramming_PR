package com.nhom01.imagerec.server;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Danh sách phiên đang hoạt động; thread-safe. Listener được gọi từ luồng thực hiện thay đổi. */
public final class SessionRegistry {

    private final Map<Integer, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final List<Consumer<List<SessionInfo>>> listeners = new CopyOnWriteArrayList<>();

    public SessionInfo register(String clientName, String remoteAddress) {
        SessionInfo info = new SessionInfo(nextId.getAndIncrement(), clientName, remoteAddress,
                LocalTime.now().withNano(0), 0);
        sessions.put(info.id(), info);
        notifyListeners();
        return info;
    }

    public void recordImage(int id) {
        if (sessions.computeIfPresent(id, (key, info) -> info.withOneMoreImage()) != null) {
            notifyListeners();
        }
    }

    public void remove(int id) {
        if (sessions.remove(id) != null) {
            notifyListeners();
        }
    }

    public List<SessionInfo> snapshot() {
        return sessions.values().stream().sorted(Comparator.comparingInt(SessionInfo::id)).toList();
    }

    public void addListener(Consumer<List<SessionInfo>> listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        List<SessionInfo> current = snapshot();
        listeners.forEach(listener -> listener.accept(current));
    }
}
