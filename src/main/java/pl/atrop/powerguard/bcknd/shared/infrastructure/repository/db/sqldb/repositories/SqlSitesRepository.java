package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.repositories;
import jakarta.enterprise.context.ApplicationScoped; import jakarta.inject.Inject; import jakarta.transaction.Transactional; import java.util.*;
import pl.atrop.powerguard.bcknd.site.domain.*; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.*; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.*;
@ApplicationScoped public class SqlSitesRepository implements SitesRepository{
 @Inject SitesBoundary sites; @Inject ClientsBoundary clients;
 public List<SiteConfiguration> findAll(){return sites.findAllOrdered().stream().map(this::toDomain).toList();}
 public Optional<SiteConfiguration> findById(Long id){return Optional.ofNullable(sites.find(id)).map(this::toDomain);}
 public Optional<SiteConfiguration> findByClientIdAndSiteId(String clientId,String siteId){var c=clients.findByClientId(clientId);return c.flatMap(x->sites.findByClientFkAndSiteId(x.getId(),siteId)).map(this::toDomain);}
 public List<SiteConfiguration> findBySiteId(String siteId){return sites.findBySiteId(siteId).stream().map(this::toDomain).toList();}
 @Transactional public SiteConfiguration create(SiteConfiguration s){return toDomain(sites.save(new Sites(s.clientPk(),s.siteId(),s.name(),s.enabled(),s.displayOrder())));}
 @Transactional public SiteConfiguration update(SiteConfiguration s){Sites e=sites.find(s.id());if(e==null)throw new IllegalArgumentException("Site not found");e.setName(s.name());e.setEnabled(s.enabled());e.setDisplayOrder(s.displayOrder());return toDomain(sites.save(e));}
 @Transactional public boolean delete(Long id){return sites.deleteById(id);}
 private SiteConfiguration toDomain(Sites s){Clients c=clients.find(s.getClientFk());return new SiteConfiguration(s.getId(),s.getClientFk(),c==null?"":c.getClientId(),c==null?"":c.getName(),s.getSiteId(),s.getName(),s.isEnabled(),s.getDisplayOrder());}
}
