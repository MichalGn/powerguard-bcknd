package pl.atrop.powerguard.bcknd.client.domain;

import java.util.List;
import java.util.Optional;

public interface ClientsService {
    List<ClientConfiguration> findPublicClients();
    List<ClientConfiguration> findAll();
    Optional<ClientConfiguration> findByClientId(String clientId);
    Optional<ClientConfiguration> create(ClientConfigurationInput input);
    Optional<ClientConfiguration> update(String clientId, ClientConfigurationInput input);
    boolean delete(String clientId);
    ClientConfiguration ensure(String clientId);
}
