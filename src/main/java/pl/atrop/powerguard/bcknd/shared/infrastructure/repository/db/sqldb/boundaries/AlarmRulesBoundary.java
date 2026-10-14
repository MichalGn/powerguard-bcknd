package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;

import jakarta.enterprise.context.ApplicationScoped; import jakarta.persistence.*; import java.util.Optional; import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.AlarmRules;
@ApplicationScoped public class AlarmRulesBoundary extends AbstractFacade<AlarmRules>{
 @PersistenceContext(unitName="powerguradPU") private EntityManager em; public AlarmRulesBoundary(){super(AlarmRules.class);} protected EntityManager getEntityManager(){return em;}
 public Optional<AlarmRules> findByMeterId(Integer id){return em.createQuery("SELECT a FROM AlarmRules a WHERE a.meterId=:id",AlarmRules.class).setParameter("id",id).setMaxResults(1).getResultStream().findFirst();}
 public AlarmRules save(AlarmRules a){AlarmRules r=a.getId()==null?a:em.merge(a);if(a.getId()==null)em.persist(a);em.flush();return r;}
 public void deleteByMeterId(Integer id){findByMeterId(id).ifPresent(this::remove);em.flush();}
}
