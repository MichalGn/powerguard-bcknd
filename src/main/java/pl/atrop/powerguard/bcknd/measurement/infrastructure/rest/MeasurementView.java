package pl.atrop.powerguard.bcknd.measurement.infrastructure.rest;

import java.math.BigDecimal;
import java.time.Instant;
import pl.atrop.powerguard.bcknd.measurement.domain.MeasurementReading;

public record MeasurementView(
        Long id,
        Integer meterId,
        Instant measuredAt,
        Instant meterMeasuredAt,
        BigDecimal activePowerKw,
        BigDecimal activeEnergyImportKwh,
        BigDecimal activeEnergyExportKwh,
        BigDecimal currentQuarterPowerKw,
        BigDecimal voltageL1V,
        BigDecimal voltageL2V,
        BigDecimal voltageL3V,
        BigDecimal currentL1A,
        BigDecimal currentL2A,
        BigDecimal currentL3A,
        BigDecimal frequencyHz) {
    public static MeasurementView from(MeasurementReading m) {
        return new MeasurementView(m.id(), m.meterId(), m.measuredAt(), m.meterMeasuredAt(),
                m.activePowerKw(), m.activeEnergyImportKwh(), m.activeEnergyExportKwh(), m.currentQuarterPowerKw(),
                m.voltageL1V(), m.voltageL2V(), m.voltageL3V(),
                m.currentL1A(), m.currentL2A(), m.currentL3A(), m.frequencyHz());
    }
}
