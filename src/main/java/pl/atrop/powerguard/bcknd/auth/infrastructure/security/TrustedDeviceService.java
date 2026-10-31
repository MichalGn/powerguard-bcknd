package pl.atrop.powerguard.bcknd.auth.infrastructure.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.auth.domain.*;

/** Revocable, hashed, expiring credentials for administrator-provisioned browsers. */
@ApplicationScoped
public class TrustedDeviceService {
    @PersistenceContext(unitName = "powerguradPU") EntityManager em;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DAYS = 30;

    @Transactional
    public String register(UserAccount user) {
        if (!user.enabled()) throw new IllegalArgumentException("Active account required");
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        em.createNativeQuery("INSERT INTO trusted_display_devices (user_id, token_hash, expires_at) VALUES (?1, ?2, ?3)")
          .setParameter(1, user.id()).setParameter(2, hash(secret))
          .setParameter(3, java.sql.Timestamp.from(Instant.now().plusSeconds((user.role() == UserRole.USER ? DAYS : 1) * 86400L))).executeUpdate();
        return secret;
    }
    @Transactional
    public Optional<Long> exchange(String secret) {
        if (secret == null || secret.length() < 40 || secret.length() > 160) return Optional.empty();
        java.util.List<?> ids = em.createNativeQuery("SELECT d.user_id FROM trusted_display_devices d JOIN users u ON u.id = d.user_id WHERE d.token_hash = ?1 AND d.expires_at > CURRENT_TIMESTAMP AND u.enabled = true")
            .setParameter(1, hash(secret)).setMaxResults(1).getResultList();
        return ids.isEmpty() ? Optional.empty() : Optional.of(((Number) ids.get(0)).longValue());
    }
    @Transactional
    public void revoke(String secret) {
        if (secret != null && !secret.isBlank()) em.createNativeQuery("DELETE FROM trusted_display_devices WHERE token_hash = ?1").setParameter(1, hash(secret)).executeUpdate();
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
