package pl.atrop.powerguard.bcknd.client.domain;

public record ClientConfiguration(Long id, String clientId, String name, boolean enabled, int displayOrder) {}
