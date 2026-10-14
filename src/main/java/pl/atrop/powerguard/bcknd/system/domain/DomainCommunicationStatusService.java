package pl.atrop.powerguard.bcknd.system.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.*;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class DomainCommunicationStatusService implements CommunicationStatusService {
    private static final Set<String> STATUSES = Set.of("ONLINE", "DEGRADED", "OFFLINE", "STARTING");
    @Inject CommunicationStatusRepository repository;

    @Override
    public void recordServer(Instant observedAt) {
        record(null, null, null, null, "SERVER", "ONLINE", observedAt);
    }

    @Override
    public void recordEdge(String clientId, String siteId, String edgeId, String status, Instant observedAt) {
        record(clientId, siteId, edgeId, null, "EDGE", status, observedAt);
    }

    @Override
    public void recordMeter(String clientId, String siteId, String edgeId, String meterUniqueKey, String status, Instant observedAt) {
        record(clientId, siteId, edgeId, meterUniqueKey, "METER", status, observedAt);
    }

    private void record(String clientId, String siteId, String edgeId, String meterUniqueKey, String entityType, String status, Instant observedAt) {
        if (!"SERVER".equals(entityType) && (blank(clientId) || blank(siteId) || blank(edgeId))) return;
        if ("METER".equals(entityType) && blank(meterUniqueKey)) return;
        String normalized = status == null ? "OFFLINE" : status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) normalized = "DEGRADED";
        repository.save(new CommunicationStatusSample(null, clientId, siteId, edgeId, meterUniqueKey, entityType, normalized,
                observedAt == null ? Instant.now() : observedAt));
    }

    @Override
    public List<CommunicationStatusSample> findRange(String clientId, String siteId, String edgeId, String meterUniqueKey, LocalDate fromDate, LocalDate toDate) {
        if (blank(clientId) || blank(siteId) || blank(edgeId) || fromDate == null || toDate == null || toDate.isBefore(fromDate)) return List.of();
        if (java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) > 31) return List.of();
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        Instant from = fromDate.atStartOfDay(zone).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(zone).toInstant();
        return repository.findBetween(clientId, siteId, edgeId, meterUniqueKey, from, to);
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
