package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.EstadoPotencial;
import com.sfarchive.core.model.Potencial;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PotencialDao {

    private static final String SELECT = """
            SELECT p.*, g.nombre AS grupo_nombre, u.username
            FROM potenciales p
            LEFT JOIN grupos_tacticos g ON g.id = p.grupo_id
            LEFT JOIN usuarios u ON u.id = p.usuario_id
            """;

    static Potencial map(ResultSet rs) throws SQLException {
        return new Potencial(rs.getInt("id"), Jdbc.intOrNull(rs, "usuario_id"), rs.getString("username"),
                rs.getString("alias"), rs.getString("nombre_real"), Jdbc.intOrNull(rs, "edad"),
                rs.getString("habilidad"), rs.getString("descripcion"), rs.getInt("nivel"),
                EstadoPotencial.valueOf(rs.getString("estado")), rs.getString("barrio"),
                rs.getString("ciudad"), rs.getString("pais"), Jdbc.doubleOrNull(rs, "lat"),
                Jdbc.doubleOrNull(rs, "lng"), Jdbc.intOrNull(rs, "grupo_id"), rs.getString("grupo_nombre"),
                rs.getBigDecimal("saldo"), Jdbc.date(rs, "fecha_reclutamiento"));
    }

    public List<Potencial> listar() {
        return Jdbc.query(SELECT + " ORDER BY p.alias", PotencialDao::map);
    }

    public Optional<Potencial> porId(int id) {
        return Jdbc.one(SELECT + " WHERE p.id = ?", PotencialDao::map, id);
    }

    public Optional<Potencial> porId(Connection c, int id) throws SQLException {
        return Jdbc.one(c, SELECT + " WHERE p.id = ? FOR UPDATE", PotencialDao::map, id);
    }

    public Optional<Potencial> porUsuario(int usuarioId) {
        return Jdbc.one(SELECT + " WHERE p.usuario_id = ?", PotencialDao::map, usuarioId);
    }

    public List<Potencial> porGrupo(Integer grupoId) {
        if (grupoId == null) return Jdbc.query(SELECT + " WHERE p.grupo_id IS NULL ORDER BY p.alias", PotencialDao::map);
        return Jdbc.query(SELECT + " WHERE p.grupo_id = ? ORDER BY p.alias", PotencialDao::map, grupoId);
    }

    public int crear(Connection c, Potencial p) throws SQLException {
        return Jdbc.insert(c, """
                INSERT INTO potenciales (usuario_id, alias, nombre_real, edad, habilidad, descripcion, nivel, estado,
                                         barrio, ciudad, pais, lat, lng, grupo_id, fecha_reclutamiento)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                p.usuarioId(), p.alias(), p.nombreReal(), p.edad(), p.habilidad(), p.descripcion(), p.nivel(),
                p.estado(), p.barrio(), p.ciudad(), p.pais(), p.lat(), p.lng(), p.grupoId(), p.fechaReclutamiento());
    }

    public void actualizar(Potencial p) {
        Jdbc.update("""
                UPDATE potenciales SET alias = ?, nombre_real = ?, edad = ?, habilidad = ?, descripcion = ?, nivel = ?,
                       estado = ?, barrio = ?, ciudad = ?, pais = ?, lat = ?, lng = ?, grupo_id = ?, fecha_reclutamiento = ?
                WHERE id = ?""",
                p.alias(), p.nombreReal(), p.edad(), p.habilidad(), p.descripcion(), p.nivel(), p.estado(),
                p.barrio(), p.ciudad(), p.pais(), p.lat(), p.lng(), p.grupoId(), p.fechaReclutamiento(), p.id());
    }

    public void cambiarGrupo(int potencialId, Integer grupoId) {
        Jdbc.update("UPDATE potenciales SET grupo_id = ? WHERE id = ?", grupoId, potencialId);
    }

    public void cambiarEstado(int potencialId, EstadoPotencial estado) {
        Jdbc.update("UPDATE potenciales SET estado = ? WHERE id = ?", estado, potencialId);
    }

    public void cambiarEstado(Connection c, int potencialId, EstadoPotencial estado) throws SQLException {
        Jdbc.update(c, "UPDATE potenciales SET estado = ? WHERE id = ?", estado, potencialId);
    }

    public void borrar(int id) {
        Jdbc.update("DELETE FROM potenciales WHERE id = ?", id);
    }
}
