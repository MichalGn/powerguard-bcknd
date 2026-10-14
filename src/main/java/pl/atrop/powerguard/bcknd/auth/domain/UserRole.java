package pl.atrop.powerguard.bcknd.auth.domain;

public enum UserRole {
    ADMIN,
    INSTALATOR,
    USER;

    public static UserRole from(String value) {
        if (value == null) return USER;
        return UserRole.valueOf(value.trim().toUpperCase());
    }
}
