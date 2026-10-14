package pl.atrop.powerguard.bcknd.meter.domain;
import java.math.BigDecimal;
public record MeterConfiguration(Integer id, Long sitePk, Long edgePk, String clientId, String clientName, String siteId, String siteName, String edgeId, String uniqueKey, String name, BigDecimal instantaneousAlarmOnKw, BigDecimal instantaneousAlarmOffKw, BigDecimal quarterHourAlarmOnKw, BigDecimal quarterHourAlarmOffKw, boolean enabled, int displayOrder) {}
