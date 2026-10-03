package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.TipoZona;
import com.sfarchive.core.model.ZonaSegura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Acceso a la tabla {@code zonas_seguras} (pins verdes del mapa público).
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class ZonaSeguraDao {

    /**
     * Convierte la fila actual del ResultSet en un objeto ZonaSegura.
     * Se usa como "mapper": {@code Jdbc.query(sql, ZonaSeguraDao::map)}.
     */
    static ZonaSegura map(ResultSet rs) throws SQLException {
        return new ZonaSegura(rs.getInt("id"), rs.getString("nombre"), TipoZona.valueOf(rs.getString("tipo")),
                rs.getString("direccion"), rs.getString("barrio"), rs.getDouble("lat"), rs.getDouble("lng"),
                rs.getString("telefono"), rs.getString("horario"), rs.getBoolean("activa"));
    }

    /** Todas las zonas, ordenadas por tipo y nombre. */
    public List<ZonaSegura> listar() {
        return Jdbc.query("SELECT * FROM zonas_seguras ORDER BY tipo, nombre", ZonaSeguraDao::map);
    }

    /** Crea (id 0) o actualiza una zona segura. Devuelve el id. */
    public int guardar(ZonaSegura z) {
        if (z.id() == 0) {
            return Jdbc.insert("""
                    INSERT INTO zonas_seguras (nombre, tipo, direccion, barrio, lat, lng, telefono, horario, activa)
                    VALUES (?,?,?,?,?,?,?,?,?)""",
                    z.nombre(), z.tipo(), z.direccion(), z.barrio(), z.lat(), z.lng(), z.telefono(), z.horario(), z.activa());
        }
        Jdbc.update("""
                UPDATE zonas_seguras SET nombre = ?, tipo = ?, direccion = ?, barrio = ?, lat = ?, lng = ?, telefono = ?,
                       horario = ?, activa = ? WHERE id = ?""",
                z.nombre(), z.tipo(), z.direccion(), z.barrio(), z.lat(), z.lng(), z.telefono(), z.horario(), z.activa(), z.id());
        return z.id();
    }

    /** Borra una zona segura. */
    public void borrar(int id) {
        Jdbc.update("DELETE FROM zonas_seguras WHERE id = ?", id);
    }
}
