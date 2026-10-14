package pl.atrop.powerguard.bcknd.system.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class EdgeConfigurationRegistry {
    private final ConcurrentHashMap<String, JsonObject> configurations = new ConcurrentHashMap<>();

    public void update(String clientId, String siteId, String edgeId, JsonObject configuration) {
        if (clientId == null || clientId.isBlank() || siteId == null || siteId.isBlank() || edgeId == null || edgeId.isBlank() || configuration == null) return;
        configurations.put(key(clientId, siteId, edgeId), configuration);
    }

    public Optional<JsonObject> find(String clientId, String siteId, String edgeId) {
        return Optional.ofNullable(configurations.get(key(clientId, siteId, edgeId)));
    }

    public void remove(String clientId, String siteId, String edgeId) {
        configurations.remove(key(clientId, siteId, edgeId));
    }

    private static String key(String clientId, String siteId, String edgeId) {
        return clientId + "\n" + siteId + "\n" + edgeId;
    }
}
