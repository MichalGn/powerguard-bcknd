package pl.atrop.powerguard.bcknd.measurement.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeasurementsService {
    Optional<MeasurementReading> record(MeasurementInput input);
    Optional<MeasurementSnapshot> latest(Integer meterId);
    List<MeasurementReading> findDay(Integer meterId, LocalDate date);
    List<MeasurementReading> findRange(Integer meterId, LocalDate from, LocalDate to);
}
