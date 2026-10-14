package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;
import jakarta.enterprise.context.ApplicationScoped; import jakarta.persistence.*; import java.util.*; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Clients;
@ApplicationScoped public class ClientsBoundary extends AbstractFacade<Clients>{
 @PersistenceContext(unitName="powerguradPU") private EntityManager em; public ClientsBoundary(){super(Clients.class);} protected EntityManager getEntityManager(){return em;}
 public Optional<Clients> findByClientId(String clientId){return em.createQuery("SELECT c FROM Clients c WHERE c.clientId=:id",Clients.class).setParameter("id",clientId).setMaxResults(1).getResultStream().findFirst();}
 public List<Clients> findAllOrdered(){return em.createQuery("SELECT c FROM Clients c ORDER BY c.displayOrder,c.name,c.clientId",Clients.class).getResultList();}
 public Clients save(Clients c){if(c.getId()==null){em.persist(c);em.flush();return c;} Clients x=em.find(Clients.class,c.getId()); x.setName(c.getName());x.setEnabled(c.isEnabled());x.setDisplayOrder(c.getDisplayOrder());em.flush();return x;}
 public boolean deleteById(Long id){Clients c=find(id);if(c==null)return false;remove(c);em.flush();return true;}
}
