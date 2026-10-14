package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="alarm_rules")
public class AlarmRules {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="meter_id",nullable=false,unique=true) private Integer meterId;
    @Column(name="instantaneous_alarm_on_kw",precision=16,scale=6) private BigDecimal instantaneousAlarmOnKw;
    @Column(name="instantaneous_alarm_off_kw",precision=16,scale=6) private BigDecimal instantaneousAlarmOffKw;
    @Column(name="quarter_hour_alarm_on_kw",precision=16,scale=6) private BigDecimal quarterHourAlarmOnKw;
    @Column(name="quarter_hour_alarm_off_kw",precision=16,scale=6) private BigDecimal quarterHourAlarmOffKw;
    @Column(name="enabled",nullable=false) private boolean enabled=true;

    protected AlarmRules(){}
    public AlarmRules(Integer meterId, BigDecimal instantOn, BigDecimal instantOff, BigDecimal quarterOn, BigDecimal quarterOff){
        this.meterId=meterId;
        this.instantaneousAlarmOnKw=instantOn;
        this.instantaneousAlarmOffKw=instantOff;
        this.quarterHourAlarmOnKw=quarterOn;
        this.quarterHourAlarmOffKw=quarterOff;
    }
    public Integer getId(){return id;}
    public Integer getMeterId(){return meterId;}
    public BigDecimal getInstantaneousAlarmOnKw(){return instantaneousAlarmOnKw;}
    public BigDecimal getInstantaneousAlarmOffKw(){return instantaneousAlarmOffKw;}
    public BigDecimal getQuarterHourAlarmOnKw(){return quarterHourAlarmOnKw;}
    public BigDecimal getQuarterHourAlarmOffKw(){return quarterHourAlarmOffKw;}
    public void setInstantaneousAlarmOnKw(BigDecimal v){instantaneousAlarmOnKw=v;}
    public void setInstantaneousAlarmOffKw(BigDecimal v){instantaneousAlarmOffKw=v;}
    public void setQuarterHourAlarmOnKw(BigDecimal v){quarterHourAlarmOnKw=v;}
    public void setQuarterHourAlarmOffKw(BigDecimal v){quarterHourAlarmOffKw=v;}
}
