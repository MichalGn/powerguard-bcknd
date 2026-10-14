package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Alarms;

@ApplicationScoped
public class AlarmsBoundary extends AbstractFacade<Alarms> {
    @PersistenceContext(unitName="powerguradPU") private EntityManager em;
    public AlarmsBoundary(){super(Alarms.class);} protected EntityManager getEntityManager(){return em;}
    public Optional<Alarms> findOpen(Integer meterId,String type){
        return em.createQuery("SELECT a FROM Alarms a WHERE a.meterId=:m AND a.alarmType=:t AND a.endedAt IS NULL ORDER BY a.startedAt DESC",Alarms.class)
                .setParameter("m",meterId).setParameter("t",type).setMaxResults(1).getResultStream().findFirst();
    }
    public List<Alarms> findActive(Integer meterId){
        return em.createQuery("SELECT a FROM Alarms a WHERE a.meterId=:m AND a.endedAt IS NULL ORDER BY a.startedAt DESC",Alarms.class)
                .setParameter("m",meterId).getResultList();
    }
    public List<Alarms> findBetween(Integer meterId,Instant from,Instant to){
        return em.createQuery("SELECT a FROM Alarms a WHERE a.meterId=:m AND a.startedAt < :to AND (a.endedAt IS NULL OR a.endedAt >= :from) ORDER BY a.startedAt DESC",Alarms.class)
                .setParameter("m",meterId).setParameter("from",from).setParameter("to",to).getResultList();
    }
    public Alarms save(Alarms a){
        if(a.getId()==null){em.persist(a);em.flush();return a;}
        Alarms existing=em.find(Alarms.class,a.getId());
        existing.setEndedAt(a.getEndedAt()); existing.setMaxValue(a.getMaxValue()); existing.setAcknowledgedAt(a.getAcknowledgedAt()); existing.setAcknowledgedBy(a.getAcknowledgedBy()); em.flush(); return existing;
    }
}
