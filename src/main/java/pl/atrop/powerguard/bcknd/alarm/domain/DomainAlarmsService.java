package pl.atrop.powerguard.bcknd.alarm.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.meter.domain.MeterConfiguration;
import pl.atrop.powerguard.bcknd.meter.domain.MetersRepository;

@ApplicationScoped
public class DomainAlarmsService implements AlarmsService {
    @Inject AlarmsRepository alarms;
    @Inject MetersRepository meters;

    @Override
    public void evaluate(Integer meterId, BigDecimal instantaneousKw, BigDecimal quarterHourKw, Instant measuredAt) {
        MeterConfiguration meter = meters.findById(meterId).orElse(null);
        if (meter == null || !meter.enabled()) return;
        evaluateOne(meterId, AlarmType.INSTANTANEOUS_POWER, instantaneousKw,
                meter.instantaneousAlarmOnKw(), meter.instantaneousAlarmOffKw(), measuredAt);
        evaluateOne(meterId, AlarmType.QUARTER_HOUR_POWER, quarterHourKw,
                meter.quarterHourAlarmOnKw(), meter.quarterHourAlarmOffKw(), measuredAt);
    }

    private void evaluateOne(Integer meterId, AlarmType type, BigDecimal value,
            BigDecimal on, BigDecimal off, Instant measuredAt) {
        if (value == null || on == null || off == null) return;
        var open = alarms.findOpen(meterId, type);
        if (open.isEmpty()) {
            if (value.compareTo(on) >= 0) {
                alarms.save(new AlarmIncident(null, meterId, type, measuredAt, null, value, value, null, null));
            }
            return;
        }
        AlarmIncident current = open.get();
        BigDecimal max = current.maxValue() == null || value.compareTo(current.maxValue()) > 0 ? value : current.maxValue();
        if (value.compareTo(off) <= 0) {
            alarms.save(new AlarmIncident(current.id(), meterId, type, current.startedAt(), measuredAt,
                    current.startValue(), max, current.acknowledgedAt(), current.acknowledgedBy()));
        } else if (current.maxValue() == null || max.compareTo(current.maxValue()) != 0) {
            alarms.save(new AlarmIncident(current.id(), meterId, type, current.startedAt(), null,
                    current.startValue(), max, current.acknowledgedAt(), current.acknowledgedBy()));
        }
    }

    @Override public List<AlarmIncident> active(Integer meterId) { return alarms.findActive(meterId); }

    @Override
    public List<AlarmIncident> findDay(Integer meterId, LocalDate date) {
        if (meterId == null || date == null || meters.findById(meterId).isEmpty()) return List.of();
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        return alarms.findBetween(meterId, from, to);
    }


    @Override
    public List<AlarmIncident> findRange(Integer meterId, LocalDate fromDate, LocalDate toDate) {
        if (meterId == null || fromDate == null || toDate == null || toDate.isBefore(fromDate) || meters.findById(meterId).isEmpty()) return List.of();
        if (java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) > 31) return List.of();
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        Instant from = fromDate.atStartOfDay(zone).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(zone).toInstant();
        return alarms.findBetween(meterId, from, to);
    }

    @Override public Optional<AlarmIncident> findById(Long alarmId) {
        return alarmId == null ? Optional.empty() : alarms.findById(alarmId);
    }

    @Override
    public Optional<AlarmIncident> acknowledge(Long alarmId, String acknowledgedBy) {
        if (alarmId == null) return Optional.empty();
        return alarms.findById(alarmId).map(existing -> {
            if (existing.acknowledged()) return existing;
            String by = acknowledgedBy == null || acknowledgedBy.isBlank() ? "dashboard" : acknowledgedBy.trim();
            return alarms.save(new AlarmIncident(existing.id(), existing.meterId(), existing.type(),
                    existing.startedAt(), existing.endedAt(), existing.startValue(), existing.maxValue(), Instant.now(), by));
        });
    }
}
