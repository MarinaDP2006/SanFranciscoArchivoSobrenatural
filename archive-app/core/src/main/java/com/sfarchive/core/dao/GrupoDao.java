package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.GrupoTactico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GrupoDao {

    public static final int MAX_MIEMBROS = 6;

    private static final String SELECT = """
            SELECT g.*, (SELECT COUNT(*) FROM potenciales p WHERE p.grupo_id = g.id) AS miembros
            FROM grupos_tacticos g
            """;

    static GrupoTactico map(ResultSet rs) throws SQLException {
        return new GrupoTactico(rs.getInt("id"), rs.getString("nombre"), rs.getString("pais"),
                rs.getString("ciudad"), rs.getString("zona"), Jdbc.doubleOrNull(rs, "lat"),
                Jdbc.doubleOrNull(rs, "lng"), rs.getString("descripcion"), rs.getBoolean("activo"),
                rs.getInt("miembros"));
    }

    public List<GrupoTactico> listar() {
        return Jdbc.query(SELECT + " ORDER BY g.pais, g.nombre", GrupoDao::map);
    }

    public Optional<GrupoTactico> porId(int id) {
        return Jdbc.one(SELECT + " WHERE g.id = ?", GrupoDao::map, id);
    }

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

    public void borrar(int id) {
        Jdbc.update("DELETE FROM grupos_tacticos WHERE id = ?", id);
    }
}
