package pl.atrop.powerguard.bcknd.auth.infrastructure.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.logging.Logger;
import pl.atrop.powerguard.bcknd.auth.application.InitialAdminUseCase;

@ApplicationScoped
public class InitialAdminBootstrap {

    private static final Logger LOG
            = Logger.getLogger(InitialAdminBootstrap.class.getName());

    @Inject
    InitialAdminUseCase useCase;

    @Transactional
    public void initialize(
            @Observes @Initialized(ApplicationScoped.class) Object event) {

        if (useCase.execute()) {
            LOG.warning(
                    "Created initial Powerguard admin user. Change default credentials immediately."
            );
        }
    }

    public void onShutdown(
            @Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {

        System.out.println("Application is shutting down");
    }
}
