package pl.atrop.powerguard.bcknd.auth.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.auth.domain.AuthTokenService;
import pl.atrop.powerguard.bcknd.auth.domain.AuthenticatedUser;
import pl.atrop.powerguard.bcknd.auth.domain.UsersService;
import pl.atrop.powerguard.bcknd.shared.infrastructure.interceptors.Timed;

@ApplicationScoped
@Timed(logParameters = false)
public class LoginUseCase {
    @Inject UsersService usersService;
    @Inject AuthTokenService tokens;

    public Optional<AuthenticatedUser> execute(LoginCommand command) {
        if (command == null) return Optional.empty();
        return usersService.authenticate(command.username(), command.passwordMd5())
                .map(user -> new AuthenticatedUser(
                        user.id(), user.username(), user.firstname(), user.lastname(), user.email(), user.phone(),
                        user.role(), user.siteIds(), tokens.issue(user.id())));
    }
}
