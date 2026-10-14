package pl.atrop.powerguard.bcknd.alarm.infrastructure.rest;

import java.math.BigDecimal;
import java.time.Instant;
import pl.atrop.powerguard.bcknd.alarm.domain.AlarmIncident;

public record AlarmView(Long id,Integer meterId,String type,Instant startedAt,Instant endedAt,
        BigDecimal startValue,BigDecimal maxValue,boolean active,Instant acknowledgedAt,String acknowledgedBy,boolean acknowledged){
    public static AlarmView from(AlarmIncident a){return new AlarmView(a.id(),a.meterId(),a.type().name(),a.startedAt(),a.endedAt(),a.startValue(),a.maxValue(),a.active(),a.acknowledgedAt(),a.acknowledgedBy(),a.acknowledged());}
}
