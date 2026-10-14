package pl.atrop.powerguard.bcknd.auth.domain;

import java.util.Set;

/** Immutable authentication result. */
public record AuthenticatedUser(
        Long id,
        String username,
        String firstname,
        String lastname,
        String email,
        String phone,
        UserRole role,
        Set<Long> siteIds,
        String token) { }
