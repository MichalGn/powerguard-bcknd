package pl.atrop.powerguard.bcknd.client.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.site.domain.SitesRepository;

@ApplicationScoped
public class DomainClientsService implements ClientsService {
    @Inject ClientsRepository repository;
    @Inject SitesRepository sites;

    @Override public List<ClientConfiguration> findPublicClients() { return repository.findAll().stream().filter(ClientConfiguration::enabled).toList(); }
    @Override public List<ClientConfiguration> findAll() { return repository.findAll(); }
    @Override public Optional<ClientConfiguration> findByClientId(String clientId) {
        return clientId == null ? Optional.empty() : repository.findByClientId(clientId.trim());
    }
    @Override public Optional<ClientConfiguration> create(ClientConfigurationInput input) {
        if (!valid(input) || repository.findByClientId(input.clientId().trim()).isPresent()) return Optional.empty();
        int order = repository.findAll().stream().mapToInt(ClientConfiguration::displayOrder).max().orElse(0) + 1;
        return Optional.of(repository.create(new ClientConfiguration(null, input.clientId().trim(), input.name().trim(), input.enabled(), order)));
    }
    @Override public Optional<ClientConfiguration> update(String clientId, ClientConfigurationInput input) {
        if (clientId == null || !valid(input)) return Optional.empty();
        var current = repository.findByClientId(clientId.trim());
        if (current.isEmpty()) return Optional.empty();
        return Optional.of(repository.update(new ClientConfiguration(current.get().id(), current.get().clientId(), input.name().trim(), input.enabled(), current.get().displayOrder())));
    }
    @Override public boolean delete(String clientId) {
        var current = findByClientId(clientId);
        if (current.isEmpty()) return false;
        if (sites.findAll().stream().anyMatch(s -> current.get().id().equals(s.clientPk()))) return false;
        return repository.delete(current.get().id());
    }
    @Override public ClientConfiguration ensure(String clientId) {
        String id = clientId == null ? "" : clientId.trim();
        if (id.isBlank()) throw new IllegalArgumentException("clientId is required");
        return repository.findByClientId(id).orElseGet(() -> {
            int order = repository.findAll().stream().mapToInt(ClientConfiguration::displayOrder).max().orElse(0) + 1;
            return repository.create(new ClientConfiguration(null, id, id, true, order));
        });
    }
    private boolean valid(ClientConfigurationInput i) { return i != null && text(i.clientId(),128) && text(i.name(),255); }
    private boolean text(String s,int max){ return s!=null&&!s.trim().isEmpty()&&s.trim().length()<=max; }
}
