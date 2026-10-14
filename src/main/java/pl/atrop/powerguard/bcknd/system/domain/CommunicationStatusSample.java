package pl.atrop.powerguard.bcknd.system.domain;

import java.time.Instant;

public record CommunicationStatusSample(
        Long id,
        String clientId,
        String siteId,
        String edgeId,
        String meterUniqueKey,
        String entityType,
        String status,
        Instant observedAt) { }
