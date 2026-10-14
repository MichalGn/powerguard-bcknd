package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "communication_status_history")
public class CommunicationStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="client_id") private String clientId;
    @Column(name="site_id") private String siteId;
    @Column(name="edge_id") private String edgeId;
    @Column(name="meter_unique_key") private String meterUniqueKey;
    @Column(name="entity_type", nullable=false) private String entityType;
    @Column(name="status", nullable=false) private String status;
    @Column(name="observed_at", nullable=false) private Instant observedAt;

    protected CommunicationStatusHistory() { }
    public CommunicationStatusHistory(String clientId, String siteId, String edgeId, String meterUniqueKey, String entityType, String status, Instant observedAt) {
        this.clientId=clientId; this.siteId=siteId; this.edgeId=edgeId; this.meterUniqueKey=meterUniqueKey; this.entityType=entityType; this.status=status; this.observedAt=observedAt;
    }
    public Long getId(){return id;} public String getClientId(){return clientId;} public String getSiteId(){return siteId;} public String getEdgeId(){return edgeId;}
    public String getMeterUniqueKey(){return meterUniqueKey;} public String getEntityType(){return entityType;} public String getStatus(){return status;} public Instant getObservedAt(){return observedAt;}
}
