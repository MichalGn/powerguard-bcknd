package pl.atrop.powerguard.bcknd.auth.infrastructure.rest;

import java.util.Set;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;

public record UserView(
        Long id,
        String username,
        String firstname,
        String lastname,
        String email,
        String phone,
        String role,
        boolean enabled,
        Set<Long> siteIds) {
    public static UserView from(UserAccount user) {
        return new UserView(user.id(), user.username(), user.firstname(), user.lastname(),
                user.email(), user.phone(), user.role().name(), user.enabled(), user.siteIds());
    }
}
