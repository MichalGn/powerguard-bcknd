package pl.atrop.powerguard.bcknd.auth.domain;

import java.util.Set;

/** Fields an administrator or installer may set for another user. */
public record ManagedUserInput(
        String username,
        String firstname,
        String lastname,
        String email,
        String phone,
        String passwordMd5,
        UserRole role,
        boolean enabled,
        Set<Long> siteIds) {

    public ManagedUserInput {
        siteIds = siteIds == null ? Set.of() : Set.copyOf(siteIds);
    }
}
