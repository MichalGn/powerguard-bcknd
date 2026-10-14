package pl.atrop.powerguard.bcknd.site.domain;

import jakarta.enterprise.context.ApplicationScoped; import jakarta.inject.Inject; import java.util.*;
import pl.atrop.powerguard.bcknd.client.domain.ClientsService; import pl.atrop.powerguard.bcknd.meter.domain.MetersRepository; import pl.atrop.powerguard.bcknd.edge.domain.EdgesRepository;
@ApplicationScoped public class DomainSitesService implements SitesService {
 @Inject SitesRepository repository; @Inject ClientsService clients; @Inject MetersRepository meters; @Inject EdgesRepository edges;
 public List<SiteConfiguration> findPublicSites(){return repository.findAll().stream().filter(SiteConfiguration::enabled).toList();}
 public List<SiteConfiguration> findAll(){return repository.findAll();}
 public Optional<SiteConfiguration> findById(Long id){return id==null?Optional.empty():repository.findById(id);}
 public Optional<SiteConfiguration> findByIdentity(String clientId,String siteId){return clientId==null||siteId==null?Optional.empty():repository.findByClientIdAndSiteId(clientId.trim(),siteId.trim());}
 public Optional<SiteConfiguration> findUniqueBySiteId(String siteId){ if(siteId==null)return Optional.empty(); var all=repository.findBySiteId(siteId.trim()); return all.size()==1?Optional.of(all.get(0)):Optional.empty(); }
 public Optional<SiteConfiguration> create(SiteConfigurationInput i){
   if(!valid(i))return Optional.empty(); var c=clients.findByClientId(i.clientId().trim()); if(c.isEmpty())return Optional.empty();
   if(repository.findByClientIdAndSiteId(i.clientId().trim(),i.siteId().trim()).isPresent())return Optional.empty();
   int order=repository.findAll().stream().filter(s->s.clientPk().equals(c.get().id())).mapToInt(SiteConfiguration::displayOrder).max().orElse(0)+1;
   return Optional.of(repository.create(new SiteConfiguration(null,c.get().id(),c.get().clientId(),c.get().name(),i.siteId().trim(),i.name().trim(),i.enabled(),order)));
 }
 public Optional<SiteConfiguration> update(String clientId,String siteId,SiteConfigurationInput i){
   if(clientId==null||siteId==null||!valid(i))return Optional.empty(); var current=findByIdentity(clientId,siteId); if(current.isEmpty())return Optional.empty();
   return Optional.of(repository.update(new SiteConfiguration(current.get().id(),current.get().clientPk(),current.get().clientId(),current.get().clientName(),current.get().siteId(),i.name().trim(),i.enabled(),current.get().displayOrder())));
 }
 public boolean delete(String clientId,String siteId){ var current=findByIdentity(clientId,siteId); if(current.isEmpty())return false; if(meters.findAll().stream().anyMatch(m->current.get().id().equals(m.sitePk())))return false; if(edges.findAll().stream().anyMatch(e->current.get().id().equals(e.sitePk())))return false; return repository.delete(current.get().id()); }
 public SiteConfiguration ensure(String clientId,String siteId){
   String c=clientId==null?"":clientId.trim(), s=siteId==null?"":siteId.trim(); if(c.isBlank()||s.isBlank())throw new IllegalArgumentException("clientId and siteId are required");
   var client=clients.ensure(c); return repository.findByClientIdAndSiteId(c,s).orElseGet(()->{int order=repository.findAll().stream().filter(x->x.clientPk().equals(client.id())).mapToInt(SiteConfiguration::displayOrder).max().orElse(0)+1; return repository.create(new SiteConfiguration(null,client.id(),client.clientId(),client.name(),s,s,true,order));});
 }
 private boolean valid(SiteConfigurationInput i){return i!=null&&text(i.clientId(),128)&&text(i.siteId(),128)&&text(i.name(),255);}
 private boolean text(String s,int max){return s!=null&&!s.trim().isEmpty()&&s.trim().length()<=max;}
}
