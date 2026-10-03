package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.Vinculo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class VinculoDao {

    static Vinculo map(ResultSet rs) throws SQLException {
        return new Vinculo(rs.getInt("id"), rs.getInt("potencial_id"), rs.getString("nombre"),
                rs.getString("parentesco"), Jdbc.intOrNull(rs, "edad"), rs.getString("ciudad"),
                rs.getBoolean("conoce_secreto"), rs.getBoolean("en_riesgo"), rs.getString("notas"));
    }

    public List<Vinculo> porPotencial(int potencialId) {
        return Jdbc.query("SELECT * FROM vinculos_familiares WHERE potencial_id = ? ORDER BY nombre",
                VinculoDao::map, potencialId);
    }

    public int guardar(Vinculo v) {
        if (v.id() == 0) {
            return Jdbc.insert("""
                    INSERT INTO vinculos_familiares (potencial_id, nombre, parentesco, edad, ciudad, conoce_secreto, en_riesgo, notas)
                    VALUES (?,?,?,?,?,?,?,?)""",
                    v.potencialId(), v.nombre(), v.parentesco(), v.edad(), v.ciudad(), v.conoceSecreto(), v.enRiesgo(), v.notas());
        }
        Jdbc.update("""
                UPDATE vinculos_familiares SET nombre = ?, parentesco = ?, edad = ?, ciudad = ?, conoce_secreto = ?,
                       en_riesgo = ?, notas = ? WHERE id = ? AND potencial_id = ?""",
                v.nombre(), v.parentesco(), v.edad(), v.ciudad(), v.conoceSecreto(), v.enRiesgo(), v.notas(),
                v.id(), v.potencialId());
        return v.id();
    }

    public void borrar(int id, int potencialId) {
        Jdbc.update("DELETE FROM vinculos_familiares WHERE id = ? AND potencial_id = ?", id, potencialId);
    }
}
