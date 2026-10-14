package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.CommunicationStatusHistory;

@ApplicationScoped
public class CommunicationStatusHistoryBoundary extends AbstractFacade<CommunicationStatusHistory> {
    @PersistenceContext(unitName="powerguradPU") private EntityManager em;
    public CommunicationStatusHistoryBoundary(){super(CommunicationStatusHistory.class);} @Override protected EntityManager getEntityManager(){return em;}
    public CommunicationStatusHistory save(CommunicationStatusHistory e){em.persist(e); em.flush(); return e;}
    public List<CommunicationStatusHistory> findBetween(String clientId,String siteId,String edgeId,String meterUniqueKey,Instant from,Instant to){
        String identity = "(h.entityType='SERVER' OR (h.clientId=:clientId AND h.siteId=:siteId AND h.edgeId=:edgeId" +
                (meterUniqueKey==null ? "" : " AND (h.meterUniqueKey IS NULL OR h.meterUniqueKey=:meter)") + "))";
        String jpql="SELECT h FROM CommunicationStatusHistory h WHERE " + identity +
                " AND h.observedAt>=:from AND h.observedAt<:to ORDER BY h.observedAt";
        var q=em.createQuery(jpql,CommunicationStatusHistory.class)
                .setParameter("clientId",clientId).setParameter("siteId",siteId).setParameter("edgeId",edgeId)
                .setParameter("from",from).setParameter("to",to);
        if(meterUniqueKey!=null) q.setParameter("meter",meterUniqueKey);
        return q.getResultList();
    }
}
