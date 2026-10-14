package pl.atrop.powerguard.bcknd.system.domain;

import java.time.Instant;

public record EdgeMeterHealth(
        String meterId,
        String serialPortName,
        String status,
        Instant lastStatusAt,
        Integer consecutiveFailures,
        Long lastReadDurationMs,
        String message) { }
