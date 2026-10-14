package pl.atrop.powerguard.bcknd.auth.infrastructure.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import pl.atrop.powerguard.bcknd.auth.domain.AuthTokenService;

@ApplicationScoped
public class InMemoryAuthTokenService implements AuthTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ConcurrentHashMap<String, Long> sessions = new ConcurrentHashMap<>();

    @Override
    public String issue(Long userId) {
        byte[] token = new byte[32];
        RANDOM.nextBytes(token);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        sessions.put(encoded, userId);
        return encoded;
    }

    @Override
    public Optional<Long> resolve(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return Optional.ofNullable(sessions.get(token.trim()));
    }

    @Override
    public void revoke(String token) {
        if (token != null) sessions.remove(token.trim());
    }
}
