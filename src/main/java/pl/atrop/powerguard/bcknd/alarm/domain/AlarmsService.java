package pl.atrop.powerguard.bcknd.alarm.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AlarmsService {
    void evaluate(Integer meterId, BigDecimal instantaneousKw, BigDecimal quarterHourKw, Instant measuredAt);
    List<AlarmIncident> active(Integer meterId);
    List<AlarmIncident> findDay(Integer meterId, LocalDate date);
    List<AlarmIncident> findRange(Integer meterId, LocalDate from, LocalDate to);
    Optional<AlarmIncident> findById(Long alarmId);
    Optional<AlarmIncident> acknowledge(Long alarmId, String acknowledgedBy);
}
