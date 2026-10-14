package pl.atrop.powerguard.bcknd.system.domain;

import java.time.Instant;
import java.util.List;

public interface CommunicationStatusRepository {
    CommunicationStatusSample save(CommunicationStatusSample sample);
    List<CommunicationStatusSample> findBetween(String clientId, String siteId, String edgeId, String meterUniqueKey, Instant from, Instant to);
}
