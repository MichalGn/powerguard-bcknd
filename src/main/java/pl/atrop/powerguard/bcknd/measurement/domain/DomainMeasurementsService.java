package pl.atrop.powerguard.bcknd.measurement.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.alarm.domain.AlarmsService;
import pl.atrop.powerguard.bcknd.meter.domain.MeterConfiguration;
import pl.atrop.powerguard.bcknd.meter.domain.MetersRepository;

@ApplicationScoped
public class DomainMeasurementsService implements MeasurementsService {
    @Inject MeasurementsRepository measurements;
    @Inject MetersRepository meters;
    @Inject AlarmsService alarms;

    @Override
    public Optional<MeasurementReading> record(MeasurementInput input) {
        if (input == null || input.meterId() == null || meters.findById(input.meterId()).isEmpty()) {
            return Optional.empty();
        }
        if (input.currentQuarterPowerKw() != null && input.currentQuarterPowerKw().signum() < 0) return Optional.empty();

        Instant measuredAt = input.measuredAt() == null ? Instant.now() : input.measuredAt();
        MeasurementReading saved = measurements.save(new MeasurementReading(
                null,
                input.meterId(),
                measuredAt,
                input.meterMeasuredAt(),
                input.activePowerKw(), input.activePowerSource(), input.activePowerQuality(),
                input.activeEnergyImportKwh(),
                input.activeEnergyExportKwh(),
                input.currentQuarterPowerKw(),
                input.voltageL1V(), input.voltageL2V(), input.voltageL3V(),
                input.currentL1A(), input.currentL2A(), input.currentL3A(),
                input.frequencyHz(), input.readDurationMs(), input.bccValid()
        ));

        // Edge calculates the live 15-minute power from cumulative energy.
        // Alarm evaluation therefore uses that value directly instead of averaging samples in BE.
        if (input.activePowerKw() != null || input.currentQuarterPowerKw() != null) {
            alarms.evaluate(input.meterId(), input.activePowerKw(), input.currentQuarterPowerKw(), measuredAt);
        }
        return Optional.of(saved);
    }

    @Override
    public Optional<MeasurementSnapshot> latest(Integer meterId) {
        if (meterId == null) return Optional.empty();
        Optional<MeterConfiguration> meter = meters.findById(meterId);
        if (meter.isEmpty()) return Optional.empty();
        return measurements.findLatest(meterId).map(latest -> new MeasurementSnapshot(
                meterId,
                meter.get().uniqueKey(),
                latest.activePowerKw(),
                latest.currentQuarterPowerKw(),
                latest.activeEnergyImportKwh(),
                latest.activeEnergyExportKwh(),
                latest.voltageL1V(), latest.voltageL2V(), latest.voltageL3V(),
                latest.currentL1A(), latest.currentL2A(), latest.currentL3A(),
                latest.frequencyHz(),
                latest.measuredAt()
        ));
    }

    @Override
    public List<MeasurementReading> findDay(Integer meterId, LocalDate date) {
        if (meterId == null || date == null || meters.findById(meterId).isEmpty()) return List.of();
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        return measurements.findBetween(meterId, from, to);
    }
    @Override
    public List<MeasurementReading> findRange(Integer meterId, LocalDate fromDate, LocalDate toDate) {
        if (meterId == null || fromDate == null || toDate == null || toDate.isBefore(fromDate) || meters.findById(meterId).isEmpty()) return List.of();
        if (java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) > 31) return List.of();
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        Instant from = fromDate.atStartOfDay(zone).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(zone).toInstant();
        long days = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        int bucketSeconds = days <= 1 ? 1 : days <= 3 ? 60 : days <= 7 ? 300 : 900;
        return measurements.findBetweenSampled(meterId, from, to, bucketSeconds);
    }

}
