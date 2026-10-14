package pl.atrop.powerguard.bcknd.auth.application;

/** Immutable application input. */
public record LoginCommand(String username, String passwordMd5) { }
