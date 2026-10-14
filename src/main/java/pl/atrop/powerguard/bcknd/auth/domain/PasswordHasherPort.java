package pl.atrop.powerguard.bcknd.auth.domain;

/** Frontend currently submits MD5 hex; migrate to passwords over TLS when possible. */
public interface PasswordHasherPort {
    String hash(String passwordMd5);
    boolean verify(String passwordMd5, String storedHash);
}
