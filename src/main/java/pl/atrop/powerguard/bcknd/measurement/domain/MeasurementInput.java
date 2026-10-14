package pl.atrop.powerguard.bcknd.measurement.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MeasurementInput(
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
        BigDecimal frequencyHz,
        Long readDurationMs,
        Boolean bccValid
) { }
