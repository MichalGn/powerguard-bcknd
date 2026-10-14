package pl.atrop.powerguard.bcknd.auth.domain;

import java.util.Optional;

/** Technology-independent authentication-session port. */
public interface AuthTokenService {
    String issue(Long userId);
    Optional<Long> resolve(String token);
    void revoke(String token);
}
