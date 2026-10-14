package pl.atrop.powerguard.bcknd.alarm.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlarmsRepository {
    Optional<AlarmIncident> findOpen(Integer meterId, AlarmType type);
    Optional<AlarmIncident> findById(Long id);
    List<AlarmIncident> findActive(Integer meterId);
    List<AlarmIncident> findBetween(Integer meterId, Instant from, Instant to);
    AlarmIncident save(AlarmIncident incident);
}
