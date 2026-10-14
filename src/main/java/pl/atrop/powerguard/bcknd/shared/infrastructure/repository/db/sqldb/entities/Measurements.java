package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "measurements", uniqueConstraints = @UniqueConstraint(name = "uq_measurements_meter_time", columnNames = {"meter_id", "measured_at"}))
public class Measurements {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meter_id", nullable = false)
    private Integer meterId;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(name = "meter_measured_at")
    private Instant meterMeasuredAt;

    @Column(name = "active_power_kw", precision = 16, scale = 6)
    private BigDecimal activePowerKw;

    @Column(name = "active_energy_import_kwh", precision = 20, scale = 6)
    private BigDecimal activeEnergyImportKwh;

    @Column(name = "active_energy_export_kwh", precision = 20, scale = 6)
    private BigDecimal activeEnergyExportKwh;

    @Column(name = "current_quarter_power_kw", precision = 16, scale = 6)
    private BigDecimal currentQuarterPowerKw;

    @Column(name = "voltage_l1_v", precision = 12, scale = 4)
    private BigDecimal voltageL1V;
    @Column(name = "voltage_l2_v", precision = 12, scale = 4)
    private BigDecimal voltageL2V;
    @Column(name = "voltage_l3_v", precision = 12, scale = 4)
    private BigDecimal voltageL3V;

    @Column(name = "current_l1_a", precision = 12, scale = 4)
    private BigDecimal currentL1A;
    @Column(name = "current_l2_a", precision = 12, scale = 4)
    private BigDecimal currentL2A;
    @Column(name = "current_l3_a", precision = 12, scale = 4)
    private BigDecimal currentL3A;

    @Column(name = "frequency_hz", precision = 10, scale = 4)
    private BigDecimal frequencyHz;

    @Column(name = "read_duration_ms")
    private Long readDurationMs;

    @Column(name = "bcc_valid")
    private Boolean bccValid;

    protected Measurements() { }

    public Measurements(Integer meterId, Instant measuredAt, Instant meterMeasuredAt,
            BigDecimal activePowerKw, BigDecimal activeEnergyImportKwh, BigDecimal activeEnergyExportKwh,
            BigDecimal currentQuarterPowerKw, BigDecimal voltageL1V, BigDecimal voltageL2V, BigDecimal voltageL3V,
            BigDecimal currentL1A, BigDecimal currentL2A, BigDecimal currentL3A,
            BigDecimal frequencyHz, Long readDurationMs, Boolean bccValid) {
        this.meterId = meterId;
        this.measuredAt = measuredAt;
        this.meterMeasuredAt = meterMeasuredAt;
        this.activePowerKw = activePowerKw;
        this.activeEnergyImportKwh = activeEnergyImportKwh;
        this.activeEnergyExportKwh = activeEnergyExportKwh;
        this.currentQuarterPowerKw = currentQuarterPowerKw;
        this.voltageL1V = voltageL1V;
        this.voltageL2V = voltageL2V;
        this.voltageL3V = voltageL3V;
        this.currentL1A = currentL1A;
        this.currentL2A = currentL2A;
        this.currentL3A = currentL3A;
        this.frequencyHz = frequencyHz;
        this.readDurationMs = readDurationMs;
        this.bccValid = bccValid;
    }

    public Long getId() { return id; }
    public Integer getMeterId() { return meterId; }
    public Instant getMeasuredAt() { return measuredAt; }
    public Instant getMeterMeasuredAt() { return meterMeasuredAt; }
    public BigDecimal getActivePowerKw() { return activePowerKw; }
    public BigDecimal getActiveEnergyImportKwh() { return activeEnergyImportKwh; }
    public BigDecimal getActiveEnergyExportKwh() { return activeEnergyExportKwh; }
    public BigDecimal getCurrentQuarterPowerKw() { return currentQuarterPowerKw; }
    public BigDecimal getVoltageL1V() { return voltageL1V; }
    public BigDecimal getVoltageL2V() { return voltageL2V; }
    public BigDecimal getVoltageL3V() { return voltageL3V; }
    public BigDecimal getCurrentL1A() { return currentL1A; }
    public BigDecimal getCurrentL2A() { return currentL2A; }
    public BigDecimal getCurrentL3A() { return currentL3A; }
    public BigDecimal getFrequencyHz() { return frequencyHz; }
    public Long getReadDurationMs() { return readDurationMs; }
    public Boolean getBccValid() { return bccValid; }
}
