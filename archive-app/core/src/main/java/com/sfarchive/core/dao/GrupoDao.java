package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.GrupoTactico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code grupos_tacticos}.
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class GrupoDao {

    /** Máximo de potenciales por grupo (también lo comprueba un trigger en MySQL). */
    public static final int MAX_MIEMBROS = 6;

    /** Consulta base: cada grupo + cuántos miembros tiene (subconsulta COUNT). */
    private static final String SELECT = """
            SELECT g.*, (SELECT COUNT(*) FROM potenciales p WHERE p.grupo_id = g.id) AS miembros
            FROM grupos_tacticos g
            """;

    /**
     * Convierte la fila actual del ResultSet en un objeto GrupoTactico.
     * Se usa como "mapper": {@code Jdbc.query(sql, GrupoDao::map)}.
     */
    static GrupoTactico map(ResultSet rs) throws SQLException {
        return new GrupoTactico(rs.getInt("id"), rs.getString("nombre"), rs.getString("pais"),
                rs.getString("ciudad"), rs.getString("zona"), Jdbc.doubleOrNull(rs, "lat"),
                Jdbc.doubleOrNull(rs, "lng"), rs.getString("descripcion"), rs.getBoolean("activo"),
                rs.getInt("miembros"));
    }

    /** Todos los grupos ordenados por país y nombre. */
    public List<GrupoTactico> listar() {
        return Jdbc.query(SELECT + " ORDER BY g.pais, g.nombre", GrupoDao::map);
    }

    /** Busca un grupo por id. */
    public Optional<GrupoTactico> porId(int id) {
        return Jdbc.one(SELECT + " WHERE g.id = ?", GrupoDao::map, id);
    }

    /** Si el id es 0 crea el grupo (INSERT); si no, lo actualiza (UPDATE). Devuelve el id. */
    public int guardar(GrupoTactico g) {
        if (g.id() == 0) {
            return Jdbc.insert("""
                    INSERT INTO grupos_tacticos (nombre, pais, ciudad, zona, lat, lng, descripcion, activo)
                    VALUES (?,?,?,?,?,?,?,?)""",
                    g.nombre(), g.pais(), g.ciudad(), g.zona(), g.lat(), g.lng(), g.descripcion(), g.activo());
        }
        Jdbc.update("""
                UPDATE grupos_tacticos SET nombre = ?, pais = ?, ciudad = ?, zona = ?, lat = ?, lng = ?,
                       descripcion = ?, activo = ? WHERE id = ?""",
                g.nombre(), g.pais(), g.ciudad(), g.zona(), g.lat(), g.lng(), g.descripcion(), g.activo(), g.id());
        return g.id();
    }

    /** Borra un grupo. Sus miembros quedan sin grupo (ON DELETE SET NULL). */
    public void borrar(int id) {
        Jdbc.update("DELETE FROM grupos_tacticos WHERE id = ?", id);
    }
}
