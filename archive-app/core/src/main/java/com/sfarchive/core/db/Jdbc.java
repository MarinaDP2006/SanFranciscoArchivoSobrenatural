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
 * Pequeño ayudante JDBC: evita repetir en cada DAO el código de siempre
 * (abrir conexión, PreparedStatement, setXxx, ResultSet, cerrar todo, capturar SQLException...).
 * <p>
 * Ejemplo de uso desde un DAO:
 * <pre>
 *   List&lt;ZonaSegura&gt; zonas = Jdbc.query("SELECT * FROM zonas_seguras", ZonaSeguraDao::map);
 *   Jdbc.update("UPDATE potenciales SET estado = ? WHERE id = ?", EstadoPotencial.HERIDO, 7);
 * </pre>
 * Todos los métodos usan PreparedStatement con {@code ?}: los valores nunca se pegan al SQL,
 * así que es imposible la inyección SQL.
 * <p>
 * Casi todos los métodos tienen dos versiones:
 * <ul>
 *   <li>Sin {@code Connection}: abre una conexión, hace la operación y la cierra.</li>
 *   <li>Con {@code Connection}: usa una conexión que ya tienes (para meter varias
 *       operaciones en la misma transacción, ver {@link #inTransaction}).</li>
 * </ul>
 */
public final class Jdbc {

    /**
     * Convierte UNA fila del ResultSet en UN objeto.
     * Cada DAO tiene su método {@code map(ResultSet rs)} que cumple esta interfaz.
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Trabajo que se ejecuta dentro de una transacción, con la conexión que se le pasa. */
    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    /** Constructor privado: es una clase de utilidades, solo tiene métodos static. */
    private Jdbc() { }

    /**
     * Ejecuta un SELECT y devuelve todas las filas convertidas en objetos.
     *
     * @param sql    consulta con {@code ?} en lugar de los valores
     * @param mapper cómo convertir cada fila (p. ej. {@code PotencialDao::map})
     * @param params valores para los {@code ?}, en orden
     */
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        // try-with-resources: la conexión se devuelve al pool sola al salir del bloque
        try (Connection c = Database.connection()) {
            return query(c, sql, mapper, params);
        } catch (SQLException e) {
            throw wrap(e); // convertimos el error técnico en un DataException con mensaje claro
        }
    }

    /** Igual que el anterior pero con una conexión que ya tienes (dentro de una transacción). */
    public static <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);                          // rellena los ?
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs)); // una fila → un objeto
                return out;
            }
        }
    }

    /**
     * SELECT que como mucho devuelve un resultado (por ejemplo, buscar por id).
     * Devuelve un {@link Optional}: vacío si no hay ninguna fila.
     */
    public static <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Versión de {@link #one} con una conexión ya abierta. */
    public static <T> Optional<T> one(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> list = query(c, sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Ejecuta un UPDATE o DELETE.
     *
     * @return cuántas filas se han modificado
     */
    public static int update(String sql, Object... params) {
        try (Connection c = Database.connection()) {
            return update(c, sql, params);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Versión de {@link #update} con una conexión ya abierta. */
    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /**
     * Ejecuta un INSERT y devuelve la clave autogenerada (el id AUTO_INCREMENT de la fila nueva).
     */
    public static int insert(String sql, Object... params) {
        try (Connection c = Database.connection()) {
            return insert(c, sql, params);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Versión de {@link #insert} con una conexión ya abierta. */
    public static int insert(Connection c, String sql, Object... params) throws SQLException {
        // RETURN_GENERATED_KEYS: pide a MySQL que nos diga el id que ha creado
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /**
     * Ejecuta varias operaciones en una única transacción: o se guardan TODAS o NINGUNA.
     * <p>
     * Ejemplo: asignar un contrato cambia el contrato, el potencial y el monedero.
     * Si falla el monedero, no puede quedar el contrato asignado → se deshace todo (rollback).
     * <pre>
     *   Jdbc.inTransaction(c -&gt; {
     *       Jdbc.update(c, "UPDATE contratos ...");
     *       Jdbc.update(c, "UPDATE potenciales ...");   // misma conexión c
     *       return null;
     *   });
     * </pre>
     */
    public static <T> T inTransaction(TxWork<T> work) {
        try (Connection c = Database.connection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);         // a partir de aquí nada se guarda hasta el commit
            try {
                T result = work.run(c);     // ejecutamos todas las operaciones
                c.commit();                 // todo bien → se guardan de golpe
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();               // algo falló → se deshace todo
                throw e;
            } finally {
                c.setAutoCommit(auto);      // dejamos la conexión como estaba antes de devolverla al pool
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** Atajo para consultas {@code SELECT COUNT(*) ...}: devuelve el número directamente. */
    public static long count(String sql, Object... params) {
        return one(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    /**
     * Rellena los {@code ?} del PreparedStatement con los parámetros, eligiendo el
     * {@code setXxx} adecuado según el tipo Java de cada valor.
     * Ojo: en JDBC los {@code ?} empiezan a contar en 1, no en 0.
     */
    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            if (p == null) ps.setNull(idx, Types.NULL);
            else if (p instanceof LocalDateTime ldt) ps.setObject(idx, ldt); // sin conversión de zona horaria
            else if (p instanceof LocalDate ld) ps.setObject(idx, ld);
            else if (p instanceof Boolean b) ps.setBoolean(idx, b);           // TINYINT(1) en MySQL
            else if (p instanceof Enum<?> en) ps.setString(idx, en.name());   // enum Java → ENUM de MySQL por su nombre
            else if (p instanceof BigDecimal bd) ps.setBigDecimal(idx, bd);   // dinero
            else ps.setObject(idx, p);                                        // String, Integer, Double...
        }
    }

    /**
     * Traduce errores de MySQL a mensajes comprensibles para el usuario.
     * Los códigos de error vienen de la documentación de MySQL.
     */
    public static DataException wrap(SQLException e) {
        String state = e.getSQLState();
        // 45000 = error lanzado por nosotros con SIGNAL en un trigger (p. ej. "máximo 6 por grupo")
        if ("45000".equals(state)) return new DataException(e.getMessage(), e);
        // 1062 = clave duplicada (UNIQUE): ya existe ese usuario, alias, código...
        if (e.getErrorCode() == 1062) return new DataException("Ya existe un registro con ese valor único.", e);
        // 1451 = no se puede borrar porque otra tabla lo referencia (FOREIGN KEY)
        if (e.getErrorCode() == 1451) return new DataException("No se puede borrar: hay registros que dependen de este.", e);
        // SQLState 08xxx = errores de conexión
        if (state != null && state.startsWith("08"))
            return new DataException("No se pudo conectar con MySQL. Revisa que el servidor esté arrancado.", e);
        return new DataException("Error de base de datos: " + e.getMessage(), e);
    }

    // ---- Utilidades de lectura que respetan los NULL de la base de datos ----
    // rs.getInt() devuelve 0 cuando la columna es NULL; con wasNull() distinguimos "0" de "vacío".

    /** Lee un INT que puede ser NULL (devuelve null en vez de 0). */
    public static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    /** Lee un DECIMAL/DOUBLE que puede ser NULL (por ejemplo, coordenadas sin rellenar). */
    public static Double doubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    /** Lee un DATETIME como LocalDateTime tal cual está guardado (sin cambiar de zona horaria). */
    public static LocalDateTime dateTime(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, LocalDateTime.class);
    }

    /** Lee un DATE como LocalDate. */
    public static LocalDate date(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, LocalDate.class);
    }
}
