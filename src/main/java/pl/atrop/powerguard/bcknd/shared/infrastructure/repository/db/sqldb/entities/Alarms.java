package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="alarms")
public class Alarms {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="meter_id",nullable=false) private Integer meterId;
    @Column(name="alarm_type",nullable=false,length=32) private String alarmType;
    @Column(name="started_at",nullable=false) private Instant startedAt;
    @Column(name="ended_at") private Instant endedAt;
    @Column(name="start_value",precision=16,scale=6) private BigDecimal startValue;
    @Column(name="max_value",precision=16,scale=6) private BigDecimal maxValue;
    @Column(name="acknowledged_at") private Instant acknowledgedAt;
    @Column(name="acknowledged_by",length=128) private String acknowledgedBy;
    protected Alarms(){}
    public Alarms(Integer meterId,String alarmType,Instant startedAt,Instant endedAt,BigDecimal startValue,BigDecimal maxValue,Instant acknowledgedAt,String acknowledgedBy){
        this.meterId=meterId;this.alarmType=alarmType;this.startedAt=startedAt;this.endedAt=endedAt;this.startValue=startValue;this.maxValue=maxValue;this.acknowledgedAt=acknowledgedAt;this.acknowledgedBy=acknowledgedBy;
    }
    public Long getId(){return id;} public Integer getMeterId(){return meterId;} public String getAlarmType(){return alarmType;}
    public Instant getStartedAt(){return startedAt;} public Instant getEndedAt(){return endedAt;} public BigDecimal getStartValue(){return startValue;} public BigDecimal getMaxValue(){return maxValue;}
    public Instant getAcknowledgedAt(){return acknowledgedAt;} public String getAcknowledgedBy(){return acknowledgedBy;}
    public void setEndedAt(Instant v){endedAt=v;} public void setMaxValue(BigDecimal v){maxValue=v;} public void setAcknowledgedAt(Instant v){acknowledgedAt=v;} public void setAcknowledgedBy(String v){acknowledgedBy=v;}
}
