package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Measurements;

@ApplicationScoped
public class MeasurementsBoundary extends AbstractFacade<Measurements> {
    @PersistenceContext(unitName = "powerguradPU")
    private EntityManager em;

    public MeasurementsBoundary() { super(Measurements.class); }

    @Override
    protected EntityManager getEntityManager() { return em; }

    public Measurements save(Measurements measurement) {
        em.persist(measurement);
        em.flush();
        return measurement;
    }

    public Optional<Measurements> findByMeterAndTime(Integer meterId, Instant measuredAt) {
        return em.createQuery(
                "SELECT m FROM Measurements m WHERE m.meterId=:meterId AND m.measuredAt=:measuredAt",
                Measurements.class)
                .setParameter("meterId", meterId)
                .setParameter("measuredAt", measuredAt)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    public Optional<Measurements> findLatest(Integer meterId) {
        return em.createQuery(
                "SELECT m FROM Measurements m WHERE m.meterId=:meterId ORDER BY m.measuredAt DESC",
                Measurements.class)
                .setParameter("meterId", meterId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    public List<Measurements> findBetween(Integer meterId, Instant from, Instant to) {
        return em.createQuery(
                "SELECT m FROM Measurements m WHERE m.meterId=:meterId AND m.measuredAt>=:from AND m.measuredAt<:to ORDER BY m.measuredAt",
                Measurements.class)
                .setParameter("meterId", meterId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }
    public List<Measurements> findBetweenSampled(Integer meterId, Instant from, Instant to, int bucketSeconds) {
        if (bucketSeconds <= 1) return findBetween(meterId, from, to);
        return em.createNativeQuery(
                "SELECT id, meter_id, measured_at, meter_measured_at, active_power_kw, active_energy_import_kwh, active_energy_export_kwh," +
                " current_quarter_power_kw, voltage_l1_v, voltage_l2_v, voltage_l3_v, current_l1_a, current_l2_a, current_l3_a," +
                " frequency_hz, read_duration_ms, bcc_valid FROM (" +
                " SELECT m.*, row_number() OVER (" +
                "   PARTITION BY floor(extract(epoch FROM measured_at) / ?4) ORDER BY measured_at DESC" +
                " ) AS pg_rn FROM measurements m" +
                " WHERE meter_id=?1 AND measured_at>=?2 AND measured_at<?3" +
                ") q WHERE pg_rn=1 ORDER BY measured_at",
                Measurements.class)
                .setParameter(1, meterId)
                .setParameter(2, from)
                .setParameter(3, to)
                .setParameter(4, bucketSeconds)
                .getResultList();
    }

}
