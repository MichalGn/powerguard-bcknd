package pl.atrop.powerguard.bcknd.measurement.infrastructure.rest;

import java.math.BigDecimal;
import java.time.Instant;
import pl.atrop.powerguard.bcknd.measurement.domain.MeasurementSnapshot;

public record MeasurementSnapshotView(
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
        Instant lastUpdated) {
    public static MeasurementSnapshotView from(MeasurementSnapshot m) {
        return new MeasurementSnapshotView(m.meterId(), m.meterUniqueKey(), m.currentPowerKw(), m.currentQuarterPowerKw(),
                m.activeEnergyImportKwh(), m.activeEnergyExportKwh(),
                m.voltageL1V(), m.voltageL2V(), m.voltageL3V(),
                m.currentL1A(), m.currentL2A(), m.currentL3A(), m.frequencyHz(), m.lastUpdated());
    }
}
