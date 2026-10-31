package pl.atrop.powerguard.bcknd.measurement.infrastructure.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/measurements")
public class LiveMeasurementsSocket {
    private static final Set<Session> SESSIONS = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        // Closed until token-authenticated, site-filtered live streaming is implemented.
        try { session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authenticated site stream required")); } catch (java.io.IOException ignored) {}
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        SESSIONS.remove(session);
    }

    public static void broadcast(String json) {
        if (json == null || json.isBlank()) return;
        for (Session session : SESSIONS) {
            if (!session.isOpen()) {
                SESSIONS.remove(session);
                continue;
            }
            session.getAsyncRemote().sendText(json);
        }
    }
}
