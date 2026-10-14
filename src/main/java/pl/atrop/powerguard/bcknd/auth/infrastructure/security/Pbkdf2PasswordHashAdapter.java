package pl.atrop.powerguard.bcknd.auth.infrastructure.security;

import jakarta.enterprise.context.ApplicationScoped;
import pl.atrop.powerguard.bcknd.auth.domain.PasswordHasherPort;

@ApplicationScoped
public class Pbkdf2PasswordHashAdapter implements PasswordHasherPort {
    @Override public String hash(String passwordMd5) {
        return PasswordHasher.hash(passwordMd5);
    }
    @Override public boolean verify(String passwordMd5, String storedHash) {
        return PasswordHasher.verify(passwordMd5, storedHash);
    }
}
