package pl.atrop.powerguard.bcknd.system.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface CommunicationStatusService {
    void recordServer(Instant observedAt);
    void recordEdge(String clientId, String siteId, String edgeId, String status, Instant observedAt);
    void recordMeter(String clientId, String siteId, String edgeId, String meterUniqueKey, String status, Instant observedAt);
    List<CommunicationStatusSample> findRange(String clientId, String siteId, String edgeId, String meterUniqueKey, LocalDate from, LocalDate to);
}
