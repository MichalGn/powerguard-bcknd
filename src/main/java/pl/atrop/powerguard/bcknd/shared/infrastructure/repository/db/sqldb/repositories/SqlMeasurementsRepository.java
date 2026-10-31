package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.measurement.domain.*;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.MeasurementsBoundary;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Measurements;

import jakarta.transaction.Transactional;
@ApplicationScoped
public class SqlMeasurementsRepository implements MeasurementsRepository {
    @Inject MeasurementsBoundary boundary;

    @Override
    @Transactional
    public MeasurementReading save(MeasurementReading measurement) {
        Optional<Measurements> existing = boundary.findByMeterAndTime(measurement.meterId(), measurement.measuredAt());
        if (existing.isPresent()) return toDomain(existing.get());
        Measurements entity = new Measurements(
                measurement.meterId(), measurement.measuredAt(), measurement.meterMeasuredAt(),
                measurement.activePowerKw(), measurement.activePowerSource(), measurement.activePowerQuality(), measurement.activeEnergyImportKwh(), measurement.activeEnergyExportKwh(),
                measurement.currentQuarterPowerKw(),
                measurement.voltageL1V(), measurement.voltageL2V(), measurement.voltageL3V(),
                measurement.currentL1A(), measurement.currentL2A(), measurement.currentL3A(),
                measurement.frequencyHz(), measurement.readDurationMs(), measurement.bccValid());
        return toDomain(boundary.save(entity));
    }

    @Override
    public Optional<MeasurementReading> findLatest(Integer meterId) {
        return boundary.findLatest(meterId).map(this::toDomain);
    }

    @Override
    public List<MeasurementReading> findBetween(Integer meterId, Instant from, Instant to) {
        return boundary.findBetween(meterId, from, to).stream().map(this::toDomain).toList();
    }

    @Override
    public List<MeasurementReading> findBetweenSampled(Integer meterId, Instant from, Instant to, int bucketSeconds) {
        return boundary.findBetweenSampled(meterId, from, to, bucketSeconds).stream().map(this::toDomain).toList();
    }


    private MeasurementReading toDomain(Measurements e) {
        return new MeasurementReading(e.getId(), e.getMeterId(), e.getMeasuredAt(), e.getMeterMeasuredAt(),
                e.getActivePowerKw(), e.getActivePowerSource(), e.getActivePowerQuality(), e.getActiveEnergyImportKwh(), e.getActiveEnergyExportKwh(), e.getCurrentQuarterPowerKw(),
                e.getVoltageL1V(), e.getVoltageL2V(), e.getVoltageL3V(),
                e.getCurrentL1A(), e.getCurrentL2A(), e.getCurrentL3A(),
                e.getFrequencyHz(), e.getReadDurationMs(), e.getBccValid());
    }
}
