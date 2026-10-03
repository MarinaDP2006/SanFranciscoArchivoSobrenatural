package com.sfarchive.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pool de conexiones a MySQL compartido por la API y la aplicación de escritorio.
 * <p>
 * ¿Qué es un "pool"? Abrir una conexión a MySQL es lento (red, usuario, contraseña...).
 * HikariCP mantiene unas pocas conexiones abiertas y las "presta": cuando haces
 * {@code connection.close()} no se cierra de verdad, vuelve al pool para reutilizarse.
 * <p>
 * Uso: {@code try (Connection c = Database.connection()) { ... }}
 */
public final class Database {

    /**
     * El pool se crea la primera vez que se pide una conexión (no al arrancar).
     * {@code volatile} + {@code synchronized} evitan que dos hilos lo creen a la vez.
     */
    private static volatile HikariDataSource dataSource;

    /** Clase de utilidades: no se crean objetos de ella. */
    private Database() { }

    /** Crea el pool la primera vez (patrón "inicialización perezosa") y lo devuelve. */
    private static HikariDataSource ds() {
        if (dataSource == null) {
            synchronized (Database.class) {
                if (dataSource == null) {
                    HikariConfig cfg = new HikariConfig();
                    // Los datos de conexión salen de archive.properties (ver Config)
                    cfg.setJdbcUrl(Config.get("db.url", "jdbc:mysql://localhost:3306/sf_archive"));
                    cfg.setUsername(Config.get("db.user", "root"));
                    cfg.setPassword(Config.get("db.password", ""));
                    cfg.setMaximumPoolSize(Config.getInt("db.pool.size", 5)); // máximo de conexiones a la vez
                    cfg.setPoolName("sfa-pool");
                    cfg.setConnectionTimeout(5000);       // si MySQL no responde en 5 s, error
                    cfg.setInitializationFailTimeout(-1); // no fallar al arrancar si MySQL aún no está
                    dataSource = new HikariDataSource(cfg);
                }
            }
        }
        return dataSource;
    }

    /** Pide una conexión al pool. Ciérrala siempre (mejor con try-with-resources). */
    public static Connection connection() throws SQLException {
        return ds().getConnection();
    }

    /** Comprueba que la base de datos responde (se usa para dar mensajes de error claros). */
    public static boolean ping() {
        try (Connection c = connection()) {
            return c.isValid(2); // espera como mucho 2 segundos
        } catch (SQLException e) {
            return false;
        }
    }

    /** Cierra todas las conexiones del pool (al salir de la aplicación). */
    public static void close() {
        if (dataSource != null) dataSource.close();
    }
}
