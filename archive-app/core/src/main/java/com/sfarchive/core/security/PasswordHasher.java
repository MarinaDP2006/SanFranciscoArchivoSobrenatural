package com.sfarchive.core.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado (hash) de contraseñas con PBKDF2-HMAC-SHA256, incluido en el JDK.
 * <p>
 * Nunca se guarda la contraseña: se guarda una "huella" de la que no se puede sacar la original.
 * Para comprobar un login se calcula la huella de lo escrito y se compara.
 * <ul>
 *   <li>Sal (salt): 16 bytes aleatorios distintos para cada contraseña, así dos contraseñas iguales
 *       tienen huellas distintas.</li>
 *   <li>Iteraciones: se repite el cálculo 65 536 veces para que adivinar por fuerza bruta sea lentísimo.</li>
 * </ul>
 * Formato guardado: {@code pbkdf2_sha256$iteraciones$salBase64$hashBase64}
 */
public final class PasswordHasher {

    /** Algoritmo del JDK que se usa. */
    private static final String ALG = "PBKDF2WithHmacSHA256";
    /** Repeticiones del cálculo (más = más seguro y más lento). */
    private static final int ITERATIONS = 65536;
    /** Tamaño de la huella: 256 bits. */
    private static final int KEY_BITS = 256;
    /** Generador aleatorio criptográfico para la sal. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Clase de utilidades: solo métodos static. */
    private PasswordHasher() { }

    /** Calcula el hash de una contraseña nueva (con una sal aleatoria). Esto es lo que se guarda en la BD. */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] dk = derive(password.toCharArray(), salt, ITERATIONS);
        return "pbkdf2_sha256$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(dk);
    }

    /** Comprueba si una contraseña coincide con el hash guardado. */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        // Separamos las 4 partes: algoritmo, iteraciones, sal y hash
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) return false;
        try {
            int it = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            // Calculamos la huella de lo que ha escrito el usuario con la MISMA sal y las mismas iteraciones
            byte[] actual = derive(password.toCharArray(), salt, it);
            return MessageDigest.isEqual(expected, actual); // comparación en tiempo constante
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Hace el cálculo PBKDF2 en sí. */
    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALG).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        }
    }

    /**
     * Política mínima de contraseñas: al menos 8 caracteres, con letras y números.
     * @return el mensaje de error, o null si la contraseña es válida
     */
    public static String validarFortaleza(String pwd) {
        if (pwd == null || pwd.length() < 8) return "La contraseña debe tener al menos 8 caracteres.";
        if (!pwd.matches(".*[A-Za-z].*") || !pwd.matches(".*\\d.*"))
            return "La contraseña debe combinar letras y números.";
        return null;
    }
}
