package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import pl.atrop.powerguard.bcknd.system.domain.*;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.CommunicationStatusHistoryBoundary;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.CommunicationStatusHistory;

@ApplicationScoped
public class SqlCommunicationStatusRepository implements CommunicationStatusRepository {
    @Inject CommunicationStatusHistoryBoundary boundary;
    @Override @Transactional public CommunicationStatusSample save(CommunicationStatusSample s){
        var e=boundary.save(new CommunicationStatusHistory(s.clientId(),s.siteId(),s.edgeId(),s.meterUniqueKey(),s.entityType(),s.status(),s.observedAt())); return toDomain(e);
    }
    @Override public List<CommunicationStatusSample> findBetween(String clientId,String siteId,String edgeId,String meterUniqueKey,Instant from,Instant to){
        return boundary.findBetween(clientId,siteId,edgeId,meterUniqueKey,from,to).stream().map(this::toDomain).toList();
    }
    private CommunicationStatusSample toDomain(CommunicationStatusHistory e){return new CommunicationStatusSample(e.getId(),e.getClientId(),e.getSiteId(),e.getEdgeId(),e.getMeterUniqueKey(),e.getEntityType(),e.getStatus(),e.getObservedAt());}
}
