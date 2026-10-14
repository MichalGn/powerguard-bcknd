package pl.atrop.powerguard.bcknd.system.application;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import pl.atrop.powerguard.bcknd.system.domain.EdgeHealth;

@ApplicationScoped
public class EdgeHealthRegistry {
    private final ConcurrentHashMap<String, EdgeHealth> health = new ConcurrentHashMap<>();

    public void update(EdgeHealth value) {
        if (value == null || value.clientId() == null || value.siteId() == null || value.edgeId() == null) return;
        health.put(key(value.clientId(), value.siteId(), value.edgeId()), value);
    }

    public List<EdgeHealth> list() {
        Instant now = Instant.now();
        return health.values().stream()
                .map(h -> {
                    boolean stale = h.timestamp() == null || Duration.between(h.timestamp(), now).getSeconds() > 90;
                    if (!stale) return h;
                    return new EdgeHealth(
                            h.clientId(), h.siteId(), h.edgeId(), "OFFLINE", h.timestamp(), h.edgeStartedAt(),
                            h.uptimeSeconds(), h.systemUptimeSeconds(), h.cpuLoadPercent(), h.cpuTemperatureC(),
                            h.memoryUsedPercent(), h.memoryTotalBytes(), h.memoryFreeBytes(),
                            h.diskUsedPercent(), h.diskTotalBytes(), h.diskFreeBytes(),
                            h.hostname(), h.ipAddresses(), h.version(), h.serialPorts(), h.mqttTargets(), h.meters());
                })
                .sorted(Comparator.comparing(EdgeHealth::clientId).thenComparing(EdgeHealth::siteId).thenComparing(EdgeHealth::edgeId))
                .toList();
    }

    public java.util.Optional<EdgeHealth> find(String clientId, String siteId, String edgeId) {
        return java.util.Optional.ofNullable(health.get(key(clientId, siteId, edgeId)));
    }

    public boolean isOffline(String clientId, String siteId, String edgeId) {
        EdgeHealth value = health.get(key(clientId, siteId, edgeId));
        if (value == null) return false;
        if ("OFFLINE".equalsIgnoreCase(value.status())) return true;
        return value.timestamp() == null || Duration.between(value.timestamp(), Instant.now()).getSeconds() > 90;
    }

    public void remove(String clientId, String siteId, String edgeId) {
        health.remove(key(clientId, siteId, edgeId));
    }

    private static String key(String clientId, String siteId, String edgeId) {
        return clientId + "\n" + siteId + "\n" + edgeId;
    }
}
