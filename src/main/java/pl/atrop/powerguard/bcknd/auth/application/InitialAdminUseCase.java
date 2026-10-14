package pl.atrop.powerguard.bcknd.auth.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pl.atrop.powerguard.bcknd.auth.domain.UsersService;

@ApplicationScoped
public class InitialAdminUseCase {
    private static final String INITIAL_PASSWORD_MD5 = "21232f297a57a5a743894a0e4a801fc3";
    @Inject UsersService usersService;

    public boolean execute() {
        return usersService.initializeAdminIfEmpty("admin", INITIAL_PASSWORD_MD5);
    }
}
