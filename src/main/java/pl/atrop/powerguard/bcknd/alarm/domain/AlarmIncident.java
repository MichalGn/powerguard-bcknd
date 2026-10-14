package pl.atrop.powerguard.bcknd.alarm.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AlarmIncident(
        Long id,
        Integer meterId,
        AlarmType type,
        Instant startedAt,
        Instant endedAt,
        BigDecimal startValue,
        BigDecimal maxValue,
        Instant acknowledgedAt,
        String acknowledgedBy) {
    public boolean active() { return endedAt == null; }
    public boolean acknowledged() { return acknowledgedAt != null; }
}
