package pl.atrop.powerguard.bcknd.auth.domain;

/** Fields a signed-in user is allowed to change on their own account. */
public record UserProfileUpdate(
        String username,
        String firstname,
        String lastname,
        String email,
        String phone,
        String passwordMd5) { }
