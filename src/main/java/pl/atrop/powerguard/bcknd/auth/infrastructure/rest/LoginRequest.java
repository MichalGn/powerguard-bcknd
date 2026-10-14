package pl.atrop.powerguard.bcknd.auth.infrastructure.rest;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/** Immutable REST input; JSON field names remain username and passwordMd5. */
public record LoginRequest(String username, String passwordMd5) {
    @JsonbCreator
    public LoginRequest(@JsonbProperty("username") String username,
                        @JsonbProperty("passwordMd5") String passwordMd5) {
        this.username = username;
        this.passwordMd5 = passwordMd5;
    }
}
