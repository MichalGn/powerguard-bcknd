package pl.atrop.powerguard.bcknd.auth.infrastructure.rest;

import jakarta.json.bind.annotation.JsonbProperty;

/** Immutable REST error response. */
public record AuthErrorResponse(@JsonbProperty("message") String message) { }
