package pl.atrop.powerguard.bcknd.measurement.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MeasurementsRepository {
    MeasurementReading save(MeasurementReading measurement);
    Optional<MeasurementReading> findLatest(Integer meterId);
    List<MeasurementReading> findBetween(Integer meterId, Instant from, Instant to);
    List<MeasurementReading> findBetweenSampled(Integer meterId, Instant from, Instant to, int bucketSeconds);
}
