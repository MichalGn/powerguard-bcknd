package pl.atrop.powerguard.bcknd.auth.infrastructure.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Optional;

/** Single-installation default account. Exposes chosen account without credentials. */
@ApplicationScoped
public class InstallationAutoLoginService {
    @PersistenceContext(unitName = "powerguradPU") EntityManager em;

    public Optional<Long> configuredUserId() {
        var rows = em.createNativeQuery("SELECT s.auto_login_user_id FROM application_settings s " +
                "JOIN users u ON u.id = s.auto_login_user_id " +
                "WHERE s.id = 1 AND u.enabled = true")
                .setMaxResults(1).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(((Number) rows.get(0)).longValue());
    }

    @Transactional
    public void configure(Long userId) {
        em.createNativeQuery("UPDATE application_settings SET auto_login_user_id = ?1 WHERE id = 1")
                .setParameter(1, userId).executeUpdate();
    }
}
