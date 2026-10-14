package pl.atrop.powerguard.bcknd.auth.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.auth.domain.AuthTokenService;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;
import pl.atrop.powerguard.bcknd.auth.domain.UserRole;
import pl.atrop.powerguard.bcknd.auth.domain.UsersService;

@ApplicationScoped
public class SiteAccessGuard {
    @Inject AuthTokenService tokens;
    @Inject UsersService users;

    public Optional<UserAccount> currentUser(String authorization) {
        return bearer(authorization).flatMap(tokens::resolve).flatMap(users::findById).filter(UserAccount::enabled);
    }

    public boolean canAccess(UserAccount user, Long siteId) {
        return users.canAccessSite(user, siteId);
    }

    public boolean isAdmin(UserAccount user) {
        return user != null && user.role() == UserRole.ADMIN;
    }

    public boolean isManager(UserAccount user) {
        return user != null && (user.role() == UserRole.ADMIN || user.role() == UserRole.INSTALATOR);
    }

    private Optional<String> bearer(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return Optional.empty();
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
