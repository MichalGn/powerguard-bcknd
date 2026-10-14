package pl.atrop.powerguard.bcknd.measurement.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MeasurementSnapshot(
        Integer meterId,
        String meterUniqueKey,
        BigDecimal currentPowerKw,
        BigDecimal currentQuarterPowerKw,
        BigDecimal activeEnergyImportKwh,
        BigDecimal activeEnergyExportKwh,
        BigDecimal voltageL1V,
        BigDecimal voltageL2V,
        BigDecimal voltageL3V,
        BigDecimal currentL1A,
        BigDecimal currentL2A,
        BigDecimal currentL3A,
        BigDecimal frequencyHz,
        Instant lastUpdated
) { }
