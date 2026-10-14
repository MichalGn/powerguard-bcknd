package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;
import jakarta.enterprise.context.ApplicationScoped; import jakarta.persistence.*; import java.util.*; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Edges;
@ApplicationScoped public class EdgesBoundary extends AbstractFacade<Edges>{
 @PersistenceContext(unitName="powerguradPU") private EntityManager em; public EdgesBoundary(){super(Edges.class);} protected EntityManager getEntityManager(){return em;}
 public Optional<Edges> findBySiteFkAndEdgeId(Long siteFk,String edgeId){return em.createQuery("SELECT e FROM Edges e WHERE e.siteFk=:s AND e.edgeId=:e",Edges.class).setParameter("s",siteFk).setParameter("e",edgeId).setMaxResults(1).getResultStream().findFirst();}
 public Edges save(Edges e){if(e.getId()==null){em.persist(e);em.flush();return e;}return em.merge(e);}
 public boolean deleteById(Long id){Edges e=find(id);if(e==null)return false;remove(e);em.flush();return true;}
}
