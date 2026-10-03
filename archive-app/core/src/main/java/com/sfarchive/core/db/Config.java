package com.sfarchive.core.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuración de la aplicación (sobre todo, cómo conectarse a MySQL).
 * <p>
 * Busca cada valor en este orden (gana el primero que encuentre):
 * <ol>
 *     <li>Variable de entorno: {@code db.url} → {@code SFA_DB_URL} (útil al desplegar en Railway...)</li>
 *     <li>Propiedad de sistema: {@code -Dsfa.db.url=...} al lanzar java</li>
 *     <li>{@code archive.properties} en la carpeta desde la que arrancas (para cambiarla sin recompilar)</li>
 *     <li>{@code archive.properties} dentro del proyecto ({@code core/src/main/resources}, valores por defecto)</li>
 * </ol>
 * Ejemplo: {@code Config.get("db.user", "root")} → "sfa_admin".
 */
public final class Config {

    /** Aquí se guardan todas las claves leídas de los archivos .properties. */
    private static final Properties PROPS = new Properties();

    // Bloque static: se ejecuta UNA vez, la primera vez que se usa la clase Config.
    static {
        // 1) Valores por defecto: archive.properties del classpath (dentro del .jar)
        try (InputStream in = Config.class.getResourceAsStream("/archive.properties")) {
            if (in != null) PROPS.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
        // 2) Si hay un archive.properties en la carpeta actual, sobrescribe los valores anteriores
        Path local = Path.of("archive.properties");
        if (Files.isRegularFile(local)) {
            try (Reader r = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                PROPS.load(r);
            } catch (IOException ignored) { }
        }
    }

    /** Clase de utilidades: no se crean objetos de ella. */
    private Config() { }

    /**
     * Devuelve el valor de una clave.
     *
     * @param key clave del .properties, p. ej. "db.url"
     * @param def valor por defecto si no se encuentra en ningún sitio
     */
    public static String get(String key, String def) {
        // "db.url" → variable de entorno "SFA_DB_URL"
        String env = System.getenv("SFA_" + key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env;
        // "db.url" → propiedad de sistema "sfa.db.url"
        String sys = System.getProperty("sfa." + key);
        if (sys != null && !sys.isBlank()) return sys;
        return PROPS.getProperty(key, def);
    }

    /** Igual que {@link #get} pero convierte el valor a número (si no es un número, usa el valor por defecto). */
    public static int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
