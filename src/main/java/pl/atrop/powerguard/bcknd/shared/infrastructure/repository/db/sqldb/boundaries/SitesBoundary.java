package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;
import jakarta.enterprise.context.ApplicationScoped; import jakarta.persistence.*; import java.util.*; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Sites;
@ApplicationScoped public class SitesBoundary extends AbstractFacade<Sites>{
 @PersistenceContext(unitName="powerguradPU") private EntityManager em; public SitesBoundary(){super(Sites.class);} protected EntityManager getEntityManager(){return em;}
 public Optional<Sites> findByClientFkAndSiteId(Long clientFk,String siteId){return em.createQuery("SELECT s FROM Sites s WHERE s.clientFk=:c AND s.siteId=:s",Sites.class).setParameter("c",clientFk).setParameter("s",siteId).setMaxResults(1).getResultStream().findFirst();}
 public List<Sites> findBySiteId(String siteId){return em.createQuery("SELECT s FROM Sites s WHERE s.siteId=:s ORDER BY s.id",Sites.class).setParameter("s",siteId).getResultList();}
 public List<Sites> findAllOrdered(){return em.createQuery("SELECT s FROM Sites s ORDER BY s.clientFk,s.displayOrder,s.name,s.siteId",Sites.class).getResultList();}
 public Sites save(Sites s){if(s.getId()==null){em.persist(s);em.flush();return s;} Sites x=find(s.getId());x.setName(s.getName());x.setEnabled(s.isEnabled());x.setDisplayOrder(s.getDisplayOrder());em.flush();return x;}
 public boolean deleteById(Long id){Sites s=find(id);if(s==null)return false;remove(s);em.flush();return true;}
}
