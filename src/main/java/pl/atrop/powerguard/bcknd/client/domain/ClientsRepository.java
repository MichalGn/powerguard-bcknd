package pl.atrop.powerguard.bcknd.client.domain;

import java.util.List;
import java.util.Optional;

public interface ClientsRepository {
    List<ClientConfiguration> findAll();
    Optional<ClientConfiguration> findById(Long id);
    Optional<ClientConfiguration> findByClientId(String clientId);
    ClientConfiguration create(ClientConfiguration client);
    ClientConfiguration update(ClientConfiguration client);
    boolean delete(Long id);
}
