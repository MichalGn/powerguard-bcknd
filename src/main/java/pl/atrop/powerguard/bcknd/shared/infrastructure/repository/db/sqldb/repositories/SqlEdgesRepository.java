package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.repositories;
import jakarta.enterprise.context.ApplicationScoped; import jakarta.inject.Inject; import jakarta.transaction.Transactional; import java.util.*; import pl.atrop.powerguard.bcknd.edge.domain.*; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.*; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.*;
@ApplicationScoped public class SqlEdgesRepository implements EdgesRepository {
 @Inject EdgesBoundary edges; @Inject SitesBoundary sites; @Inject ClientsBoundary clients;
 public Optional<EdgeIdentity> findById(Long id){return Optional.ofNullable(edges.find(id)).map(this::toDomain);} public Optional<EdgeIdentity> findByIdentity(Long sitePk,String edgeId){return edges.findBySiteFkAndEdgeId(sitePk,edgeId).map(this::toDomain);} public List<EdgeIdentity> findAll(){return edges.findAll().stream().map(this::toDomain).toList();}
 @Transactional public EdgeIdentity create(EdgeIdentity e){return toDomain(edges.save(new Edges(e.sitePk(),e.edgeId())));} @Transactional public boolean delete(Long id){return edges.deleteById(id);}
 private EdgeIdentity toDomain(Edges e){Sites s=sites.find(e.getSiteFk()); Clients c=s==null?null:clients.find(s.getClientFk()); return new EdgeIdentity(e.getId(),e.getSiteFk(),c==null?"":c.getClientId(),s==null?"":s.getSiteId(),e.getEdgeId());}
}
