package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;
import pl.atrop.powerguard.bcknd.alarm.domain.*;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.AlarmsBoundary;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Alarms;

import jakarta.transaction.Transactional;
@ApplicationScoped
public class SqlAlarmsRepository implements AlarmsRepository {
    @Inject AlarmsBoundary boundary;
    public Optional<AlarmIncident> findOpen(Integer meterId,AlarmType type){return boundary.findOpen(meterId,type.name()).map(this::toDomain);}
    public Optional<AlarmIncident> findById(Long id){return Optional.ofNullable(boundary.find(id)).map(this::toDomain);}
    public List<AlarmIncident> findActive(Integer meterId){return boundary.findActive(meterId).stream().map(this::toDomain).toList();}
    public List<AlarmIncident> findBetween(Integer meterId,Instant from,Instant to){return boundary.findBetween(meterId,from,to).stream().map(this::toDomain).toList();}
    @Transactional
    public AlarmIncident save(AlarmIncident i){
        Alarms e=new Alarms(i.meterId(),i.type().name(),i.startedAt(),i.endedAt(),i.startValue(),i.maxValue(),i.acknowledgedAt(),i.acknowledgedBy());
        if(i.id()!=null){
            Alarms current=boundary.find(i.id());
            if(current!=null){current.setEndedAt(i.endedAt());current.setMaxValue(i.maxValue());current.setAcknowledgedAt(i.acknowledgedAt());current.setAcknowledgedBy(i.acknowledgedBy());return toDomain(boundary.save(current));}
        }
        return toDomain(boundary.save(e));
    }
    private AlarmIncident toDomain(Alarms e){return new AlarmIncident(e.getId(),e.getMeterId(),AlarmType.valueOf(e.getAlarmType()),e.getStartedAt(),e.getEndedAt(),e.getStartValue(),e.getMaxValue(),e.getAcknowledgedAt(),e.getAcknowledgedBy());}
}
