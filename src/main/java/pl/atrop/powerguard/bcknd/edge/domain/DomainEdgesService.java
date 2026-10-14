package pl.atrop.powerguard.bcknd.edge.domain;
import jakarta.enterprise.context.ApplicationScoped; import jakarta.inject.Inject; import java.util.Optional; import pl.atrop.powerguard.bcknd.site.domain.SitesService;
@ApplicationScoped public class DomainEdgesService implements EdgesService {
 @Inject EdgesRepository repository; @Inject SitesService sites;
 public Optional<EdgeIdentity> findByIdentity(String clientId,String siteId,String edgeId){ if(edgeId==null)return Optional.empty(); return sites.findByIdentity(clientId,siteId).flatMap(s->repository.findByIdentity(s.id(),edgeId.trim())); }
 public EdgeIdentity ensure(String clientId,String siteId,String edgeId){String e=edgeId==null?"":edgeId.trim(); if(e.isBlank())throw new IllegalArgumentException("edgeId is required"); var s=sites.ensure(clientId,siteId); return repository.findByIdentity(s.id(),e).orElseGet(()->repository.create(new EdgeIdentity(null,s.id(),s.clientId(),s.siteId(),e)));}
}
