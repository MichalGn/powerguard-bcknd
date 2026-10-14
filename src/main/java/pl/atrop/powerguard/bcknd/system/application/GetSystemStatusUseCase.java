package pl.atrop.powerguard.bcknd.system.application;

import jakarta.enterprise.context.ApplicationScoped;
import pl.atrop.powerguard.bcknd.system.domain.SystemStatus;
import pl.atrop.powerguard.bcknd.shared.infrastructure.interceptors.Timed;

@ApplicationScoped
@Timed
public class GetSystemStatusUseCase {

    public SystemStatus execute() {
        return new SystemStatus("PowerGuard backend is running");
    }
}
