package pl.atrop.powerguard.bcknd.system.infrastructure.rest;
import java.time.Instant;
import pl.atrop.powerguard.bcknd.system.domain.CommunicationStatusSample;
public record CommunicationStatusView(String clientId,String siteId,String edgeId,String meterUniqueKey,String entityType,String status,Instant observedAt){
    public static CommunicationStatusView from(CommunicationStatusSample s){return new CommunicationStatusView(s.clientId(),s.siteId(),s.edgeId(),s.meterUniqueKey(),s.entityType(),s.status(),s.observedAt());}
}
