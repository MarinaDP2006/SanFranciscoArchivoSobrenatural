package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.TipoZona;
import com.sfarchive.core.model.ZonaSegura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ZonaSeguraDao {

    static ZonaSegura map(ResultSet rs) throws SQLException {
        return new ZonaSegura(rs.getInt("id"), rs.getString("nombre"), TipoZona.valueOf(rs.getString("tipo")),
                rs.getString("direccion"), rs.getString("barrio"), rs.getDouble("lat"), rs.getDouble("lng"),
                rs.getString("telefono"), rs.getString("horario"), rs.getBoolean("activa"));
    }

    public List<ZonaSegura> listar() {
        return Jdbc.query("SELECT * FROM zonas_seguras ORDER BY tipo, nombre", ZonaSeguraDao::map);
    }

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

    public void borrar(int id) {
        Jdbc.update("DELETE FROM zonas_seguras WHERE id = ?", id);
    }
}
