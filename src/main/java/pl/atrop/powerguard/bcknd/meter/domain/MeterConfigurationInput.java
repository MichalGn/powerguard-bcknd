package pl.atrop.powerguard.bcknd.meter.domain;
import java.math.BigDecimal;
public record MeterConfigurationInput(String clientId,String siteId,String edgeId,String uniqueKey,String name,BigDecimal instantaneousAlarmOnKw,BigDecimal instantaneousAlarmOffKw,BigDecimal quarterHourAlarmOnKw,BigDecimal quarterHourAlarmOffKw,boolean enabled) {}
