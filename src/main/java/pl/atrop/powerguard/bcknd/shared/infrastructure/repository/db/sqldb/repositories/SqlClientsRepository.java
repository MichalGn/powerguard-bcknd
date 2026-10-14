package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.repositories;
import jakarta.enterprise.context.ApplicationScoped; import jakarta.inject.Inject; import jakarta.transaction.Transactional; import java.util.*;
import pl.atrop.powerguard.bcknd.client.domain.*; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.ClientsBoundary; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Clients;
@ApplicationScoped public class SqlClientsRepository implements ClientsRepository{
 @Inject ClientsBoundary boundary;
 public List<ClientConfiguration> findAll(){return boundary.findAllOrdered().stream().map(this::toDomain).toList();}
 public Optional<ClientConfiguration> findById(Long id){return Optional.ofNullable(boundary.find(id)).map(this::toDomain);}
 public Optional<ClientConfiguration> findByClientId(String id){return boundary.findByClientId(id).map(this::toDomain);}
 @Transactional public ClientConfiguration create(ClientConfiguration c){return toDomain(boundary.save(new Clients(c.clientId(),c.name(),c.enabled(),c.displayOrder())));}
 @Transactional public ClientConfiguration update(ClientConfiguration c){Clients e=boundary.find(c.id()); if(e==null)throw new IllegalArgumentException("Client not found"); e.setName(c.name());e.setEnabled(c.enabled());e.setDisplayOrder(c.displayOrder()); return toDomain(boundary.save(e));}
 @Transactional public boolean delete(Long id){return boundary.deleteById(id);}
 private ClientConfiguration toDomain(Clients c){return new ClientConfiguration(c.getId(),c.getClientId(),c.getName(),c.isEnabled(),c.getDisplayOrder());}
}
