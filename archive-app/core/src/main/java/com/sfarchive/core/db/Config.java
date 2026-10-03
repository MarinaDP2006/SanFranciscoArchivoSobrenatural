package com.sfarchive.core.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuración de la aplicación. Orden de prioridad (de menor a mayor):
 * <ol>
 *     <li>{@code archive.properties} dentro del classpath (valores por defecto)</li>
 *     <li>{@code archive.properties} en el directorio de trabajo</li>
 *     <li>Variables de entorno {@code SFA_DB_URL}, {@code SFA_DB_USER}, ...</li>
 * </ol>
 */
public final class Config {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = Config.class.getResourceAsStream("/archive.properties")) {
            if (in != null) PROPS.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
        Path local = Path.of("archive.properties");
        if (Files.isRegularFile(local)) {
            try (Reader r = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                PROPS.load(r);
            } catch (IOException ignored) { }
        }
    }

    private Config() { }

    /** Devuelve la clave buscando primero en entorno (db.url → SFA_DB_URL). */
    public static String get(String key, String def) {
        String env = System.getenv("SFA_" + key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env;
        String sys = System.getProperty("sfa." + key);
        if (sys != null && !sys.isBlank()) return sys;
        return PROPS.getProperty(key, def);
    }

    public static int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
