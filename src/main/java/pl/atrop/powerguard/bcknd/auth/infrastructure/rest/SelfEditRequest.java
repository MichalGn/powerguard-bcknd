package pl.atrop.powerguard.bcknd.auth.infrastructure.rest;

public record SelfEditRequest(
        String username,
        String firstname,
        String lastname,
        String email,
        String phone,
        String passwordMd5) { }
