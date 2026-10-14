package pl.atrop.powerguard.bcknd.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** One-way PBKDF2 of the frontend-supplied MD5 hex digest, not plaintext or reversible encryption. */
public final class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private PasswordHasher() { }

    public static String hash(String md5Hex) {
        if (!validMd5(md5Hex)) throw new IllegalArgumentException("Expected MD5 hex digest");
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(md5Hex.toLowerCase(java.util.Locale.ROOT), salt, ITERATIONS);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String md5Hex, String stored) {
        if (!validMd5(md5Hex) || stored == null) return false;
        String[] parts = stored.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length != SALT_BYTES || expected.length != KEY_BITS / 8) return false;
            byte[] candidate = derive(md5Hex.toLowerCase(java.util.Locale.ROOT), salt, iterations);
            return MessageDigest.isEqual(expected, candidate);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean validMd5(String md5Hex) {
        return md5Hex != null && md5Hex.matches("(?i)[0-9a-f]{32}");
    }

    private static byte[] derive(String md5Hex, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(md5Hex.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }
}
