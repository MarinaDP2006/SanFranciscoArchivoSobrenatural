package com.sfarchive.core.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256 (incluido en el JDK, sin dependencias).
 * Formato almacenado: {@code pbkdf2_sha256$iteraciones$saltBase64$hashBase64}
 */
public final class PasswordHasher {

    private static final String ALG = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] dk = derive(password.toCharArray(), salt, ITERATIONS);
        return "pbkdf2_sha256$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(dk);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) return false;
        try {
            int it = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, it);
            return MessageDigest.isEqual(expected, actual); // comparación en tiempo constante
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALG).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        }
    }

    /** Política mínima de contraseñas para la app. */
    public static String validarFortaleza(String pwd) {
        if (pwd == null || pwd.length() < 8) return "La contraseña debe tener al menos 8 caracteres.";
        if (!pwd.matches(".*[A-Za-z].*") || !pwd.matches(".*\\d.*"))
            return "La contraseña debe combinar letras y números.";
        return null;
    }
}
