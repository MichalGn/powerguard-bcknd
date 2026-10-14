package pl.atrop.powerguard.bcknd.system.infrastructure.scheduler;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import java.time.Instant;
import pl.atrop.powerguard.bcknd.system.domain.CommunicationStatusService;

/**
 * Persists backend liveness independently of Edge/Meter messages.
 * Missing samples later let the dashboard distinguish a backend outage
 * from an Edge or meter communication problem.
 */
@Singleton
@Startup
public class ServerHeartbeatScheduler {
    @Inject CommunicationStatusService communicationStatusService;

    @Schedule(hour = "*", minute = "*", second = "0,30", persistent = false)
    public void heartbeat() {
        communicationStatusService.recordServer(Instant.now());
    }
}
