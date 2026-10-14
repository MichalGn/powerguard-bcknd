package pl.atrop.powerguard.bcknd.auth.infrastructure.rest;

import java.util.Set;

public record ManagedUserRequest(
        String username,
        String firstname,
        String lastname,
        String email,
        String phone,
        String passwordMd5,
        String role,
        boolean enabled,
        Set<Long> siteIds) { }
