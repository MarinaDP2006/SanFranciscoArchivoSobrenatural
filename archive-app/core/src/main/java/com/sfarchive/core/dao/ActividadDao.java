package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.Actividad;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de auditoría (tabla {@code registro_actividad}): quién hizo qué y cuándo.
 * Se ve en la pantalla "Usuarios y actividad".
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class ActividadDao {

    /** Las últimas acciones registradas. */
    public List<Actividad> recientes(int limite) {
        return Jdbc.query("""
                SELECT a.*, u.username FROM registro_actividad a LEFT JOIN usuarios u ON u.id = a.usuario_id
                ORDER BY a.fecha DESC, a.id DESC LIMIT ?""",
                rs -> new Actividad(rs.getInt("id"), rs.getString("username"), rs.getString("accion"),
                        rs.getString("entidad"), Jdbc.intOrNull(rs, "entidad_id"), rs.getString("detalle"),
                        Jdbc.dateTime(rs, "fecha")), limite);
    }

    /** Apunta una acción. Si falla, se ignora: la auditoría nunca debe impedir la operación principal. */
    public void registrar(Integer usuarioId, String accion, String entidad, Integer entidadId, String detalle) {
        try {
            Jdbc.insert("INSERT INTO registro_actividad (usuario_id, accion, entidad, entidad_id, detalle, fecha) VALUES (?,?,?,?,?,?)",
                    usuarioId, accion, entidad, entidadId, recortar(detalle), LocalDateTime.now());
        } catch (RuntimeException ignored) {
            // la auditoría nunca debe impedir la operación principal
        }
    }

    /** Apunta una acción dentro de una transacción (si la transacción se deshace, el apunte también). */
    public void registrar(Connection c, Integer usuarioId, String accion, String entidad, Integer entidadId, String detalle)
            throws SQLException {
        Jdbc.insert(c, "INSERT INTO registro_actividad (usuario_id, accion, entidad, entidad_id, detalle, fecha) VALUES (?,?,?,?,?,?)",
                usuarioId, accion, entidad, entidadId, recortar(detalle), LocalDateTime.now());
    }

    /** Corta el texto a 400 caracteres (tamaño de la columna detalle). */
    private static String recortar(String s) {
        return s == null || s.length() <= 400 ? s : s.substring(0, 400);
    }
}
