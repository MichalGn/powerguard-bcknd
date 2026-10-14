package pl.atrop.powerguard.bcknd.auth.infrastructure.rest;

import jakarta.json.bind.annotation.JsonbProperty;
import java.util.Set;

public record LoginResponse(
        @JsonbProperty("authenticated") boolean authenticated,
        @JsonbProperty("id") Long id,
        @JsonbProperty("username") String username,
        @JsonbProperty("firstname") String firstname,
        @JsonbProperty("lastname") String lastname,
        @JsonbProperty("email") String email,
        @JsonbProperty("phone") String phone,
        @JsonbProperty("role") String role,
        @JsonbProperty("siteIds") Set<Long> siteIds,
        @JsonbProperty("token") String token) { }
