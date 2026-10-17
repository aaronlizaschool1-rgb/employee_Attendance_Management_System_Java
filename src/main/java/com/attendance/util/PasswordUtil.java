package com.attendance.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Password hashing with PBKDF2-HMAC-SHA256 (built into the JDK, no extra library).
 *
 * A stored password looks like:   pbkdf2$120000$<salt>$<hash>
 *   - pbkdf2   : the algorithm
 *   - 120000   : the number of iterations (stored so it can be raised later)
 *   - salt     : 16 random bytes, different for every password (Base64)
 *   - hash     : the derived 256-bit key (Base64)
 *
 * Two users with the same password get different stored values because of the salt.
 * A hash cannot be reversed, so the original password is never stored.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final Pattern HASHED =
            Pattern.compile("^pbkdf2\\$\\d+\\$[A-Za-z0-9+/=]+\\$[A-Za-z0-9+/=]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    /** True if the value is already in the pbkdf2$iterations$salt$hash format. */
    public static boolean isHashed(String stored) {
        return stored != null && HASHED.matcher(stored).matches();
    }

    /** Hashes a plain-text password with a new random salt. */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(key);
    }

    /** Checks a typed password against a stored hash. Returns false for anything invalid. */
    public static boolean verify(String password, String stored) {
        if (password == null || !isHashed(stored)) {
            return false;
        }
        try {
            String[] parts = stored.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual); // constant-time comparison
        } catch (IllegalArgumentException ex) {
            return false; // bad number or bad Base64
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations, int keyBits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing is not available on this system", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
