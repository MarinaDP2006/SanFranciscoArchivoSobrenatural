package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.Vinculo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Acceso a la tabla {@code vinculos_familiares}.
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class VinculoDao {

    /**
     * Convierte la fila actual del ResultSet en un objeto Vinculo.
     * Se usa como "mapper": {@code Jdbc.query(sql, VinculoDao::map)}.
     */
    static Vinculo map(ResultSet rs) throws SQLException {
        return new Vinculo(rs.getInt("id"), rs.getInt("potencial_id"), rs.getString("nombre"),
                rs.getString("parentesco"), Jdbc.intOrNull(rs, "edad"), rs.getString("ciudad"),
                rs.getBoolean("conoce_secreto"), rs.getBoolean("en_riesgo"), rs.getString("notas"));
    }

    /** Familiares de un potencial. */
    public List<Vinculo> porPotencial(int potencialId) {
        return Jdbc.query("SELECT * FROM vinculos_familiares WHERE potencial_id = ? ORDER BY nombre",
                VinculoDao::map, potencialId);
    }

    /**
     * Crea (id 0) o actualiza un vínculo. El UPDATE comprueba también el potencial
     * para que un potencial no pueda editar los familiares de otro.
     */
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

    /** Borra un vínculo (solo si pertenece a ese potencial). */
    public void borrar(int id, int potencialId) {
        Jdbc.update("DELETE FROM vinculos_familiares WHERE id = ? AND potencial_id = ?", id, potencialId);
    }
}
