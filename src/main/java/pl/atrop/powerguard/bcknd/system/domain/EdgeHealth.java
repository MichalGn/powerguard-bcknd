package pl.atrop.powerguard.bcknd.system.domain;

import java.time.Instant;
import java.util.List;

public record EdgeHealth(
        String clientId,
        String siteId,
        String edgeId,
        String status,
        Instant timestamp,
        Instant edgeStartedAt,
        Long uptimeSeconds,
        Long systemUptimeSeconds,
        Double cpuLoadPercent,
        Double cpuTemperatureC,
        Double memoryUsedPercent,
        Long memoryTotalBytes,
        Long memoryFreeBytes,
        Double diskUsedPercent,
        Long diskTotalBytes,
        Long diskFreeBytes,
        String hostname,
        List<String> ipAddresses,
        String version,
        List<String> serialPorts,
        List<EdgeMqttHealth> mqttTargets,
        List<EdgeMeterHealth> meters) {

    public EdgeHealth {
        ipAddresses = ipAddresses == null ? List.of() : List.copyOf(ipAddresses);
        serialPorts = serialPorts == null ? List.of() : List.copyOf(serialPorts);
        mqttTargets = mqttTargets == null ? List.of() : List.copyOf(mqttTargets);
        meters = meters == null ? List.of() : List.copyOf(meters);
    }
}
