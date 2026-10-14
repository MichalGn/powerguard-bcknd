package pl.atrop.powerguard.bcknd.client.infrastructure.rest;
import pl.atrop.powerguard.bcknd.client.domain.ClientConfiguration;
public record ClientView(Long id, String clientId, String name, boolean enabled, int displayOrder) {
    public static ClientView from(ClientConfiguration c){ return new ClientView(c.id(),c.clientId(),c.name(),c.enabled(),c.displayOrder()); }
}
