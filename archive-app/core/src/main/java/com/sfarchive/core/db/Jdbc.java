package com.sfarchive.core.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pequeño ayudante JDBC: evita repetir try/catch y el mapeo de parámetros.
 * Todos los métodos usan PreparedStatement (protección contra inyección SQL).
 */
public final class Jdbc {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    private Jdbc() { }

    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = Database.connection()) {
            return query(c, sql, mapper, params);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public static <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        }
    }

    public static <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public static <T> Optional<T> one(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> list = query(c, sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public static int update(String sql, Object... params) {
        try (Connection c = Database.connection()) {
            return update(c, sql, params);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Ejecuta un INSERT y devuelve la clave autogenerada. */
    public static int insert(String sql, Object... params) {
        try (Connection c = Database.connection()) {
            return insert(c, sql, params);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public static int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** Ejecuta varias operaciones en una única transacción (todo o nada). */
    public static <T> T inTransaction(TxWork<T> work) {
        try (Connection c = Database.connection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public static long count(String sql, Object... params) {
        return one(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            if (p == null) ps.setNull(idx, Types.NULL);
            else if (p instanceof LocalDateTime ldt) ps.setObject(idx, ldt); // sin conversión de zona horaria
            else if (p instanceof LocalDate ld) ps.setObject(idx, ld);
            else if (p instanceof Boolean b) ps.setBoolean(idx, b);
            else if (p instanceof Enum<?> en) ps.setString(idx, en.name());
            else if (p instanceof BigDecimal bd) ps.setBigDecimal(idx, bd);
            else ps.setObject(idx, p);
        }
    }

    /** Traduce errores de MySQL a mensajes comprensibles para el usuario. */
    public static DataException wrap(SQLException e) {
        String state = e.getSQLState();
        if ("45000".equals(state)) return new DataException(e.getMessage(), e);
        if (e.getErrorCode() == 1062) return new DataException("Ya existe un registro con ese valor único.", e);
        if (e.getErrorCode() == 1451) return new DataException("No se puede borrar: hay registros que dependen de este.", e);
        if (state != null && state.startsWith("08"))
            return new DataException("No se pudo conectar con MySQL. Revisa que el servidor esté arrancado.", e);
        return new DataException("Error de base de datos: " + e.getMessage(), e);
    }

    // ---- utilidades de lectura con nulos ----

    public static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static Double doubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    public static LocalDateTime dateTime(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, LocalDateTime.class);
    }

    public static LocalDate date(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, LocalDate.class);
    }
}
