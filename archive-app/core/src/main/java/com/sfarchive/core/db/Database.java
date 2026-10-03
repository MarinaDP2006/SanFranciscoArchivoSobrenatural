package com.sfarchive.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pool de conexiones a MySQL compartido por la API y la aplicación de escritorio.
 */
public final class Database {

    private static volatile HikariDataSource dataSource;

    private Database() { }

    private static HikariDataSource ds() {
        if (dataSource == null) {
            synchronized (Database.class) {
                if (dataSource == null) {
                    HikariConfig cfg = new HikariConfig();
                    cfg.setJdbcUrl(Config.get("db.url", "jdbc:mysql://localhost:3306/sf_archive"));
                    cfg.setUsername(Config.get("db.user", "root"));
                    cfg.setPassword(Config.get("db.password", ""));
                    cfg.setMaximumPoolSize(Config.getInt("db.pool.size", 5));
                    cfg.setPoolName("sfa-pool");
                    cfg.setConnectionTimeout(5000);
                    cfg.setInitializationFailTimeout(-1); // no fallar al arrancar si MySQL aún no está
                    dataSource = new HikariDataSource(cfg);
                }
            }
        }
        return dataSource;
    }

    public static Connection connection() throws SQLException {
        return ds().getConnection();
    }

    /** Comprueba que la base de datos responde. */
    public static boolean ping() {
        try (Connection c = connection()) {
            return c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public static void close() {
        if (dataSource != null) dataSource.close();
    }
}
