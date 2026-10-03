package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.EstadoPotencial;
import com.sfarchive.core.model.Potencial;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code potenciales} (los agentes de la red).
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class PotencialDao {

    /** Consulta base con JOIN para traer también el nombre del grupo y el usuario de la cuenta. */
    private static final String SELECT = """
            SELECT p.*, g.nombre AS grupo_nombre, u.username
            FROM potenciales p
            LEFT JOIN grupos_tacticos g ON g.id = p.grupo_id
            LEFT JOIN usuarios u ON u.id = p.usuario_id
            """;

    /**
     * Convierte la fila actual del ResultSet en un objeto Potencial.
     * Se usa como "mapper": {@code Jdbc.query(sql, PotencialDao::map)}.
     */
    static Potencial map(ResultSet rs) throws SQLException {
        return new Potencial(rs.getInt("id"), Jdbc.intOrNull(rs, "usuario_id"), rs.getString("username"),
                rs.getString("alias"), rs.getString("nombre_real"), Jdbc.intOrNull(rs, "edad"),
                rs.getString("habilidad"), rs.getString("descripcion"), rs.getInt("nivel"),
                EstadoPotencial.valueOf(rs.getString("estado")), rs.getString("barrio"),
                rs.getString("ciudad"), rs.getString("pais"), Jdbc.doubleOrNull(rs, "lat"),
                Jdbc.doubleOrNull(rs, "lng"), Jdbc.intOrNull(rs, "grupo_id"), rs.getString("grupo_nombre"),
                rs.getBigDecimal("saldo"), Jdbc.date(rs, "fecha_reclutamiento"));
    }

    /** Todos los potenciales ordenados por alias. */
    public List<Potencial> listar() {
        return Jdbc.query(SELECT + " ORDER BY p.alias", PotencialDao::map);
    }

    /** Busca un potencial por id. */
    public Optional<Potencial> porId(int id) {
        return Jdbc.one(SELECT + " WHERE p.id = ?", PotencialDao::map, id);
    }

    /**
     * Busca un potencial dentro de una transacción y BLOQUEA su fila (FOR UPDATE)
     * hasta el commit, para que nadie lo asigne a la vez a otro contrato.
     */
    public Optional<Potencial> porId(Connection c, int id) throws SQLException {
        return Jdbc.one(c, SELECT + " WHERE p.id = ? FOR UPDATE", PotencialDao::map, id);
    }

    /** Busca la ficha de potencial de una cuenta (para el panel "Mis misiones"). */
    public Optional<Potencial> porUsuario(int usuarioId) {
        return Jdbc.one(SELECT + " WHERE p.usuario_id = ?", PotencialDao::map, usuarioId);
    }

    /** Miembros de un grupo táctico. Si {@code grupoId} es null, devuelve los que no tienen grupo. */
    public List<Potencial> porGrupo(Integer grupoId) {
        if (grupoId == null) return Jdbc.query(SELECT + " WHERE p.grupo_id IS NULL ORDER BY p.alias", PotencialDao::map);
        return Jdbc.query(SELECT + " WHERE p.grupo_id = ? ORDER BY p.alias", PotencialDao::map, grupoId);
    }

    /** Inserta un potencial nuevo (dentro de la transacción de reclutamiento). Devuelve su id. */
    public int crear(Connection c, Potencial p) throws SQLException {
        return Jdbc.insert(c, """
                INSERT INTO potenciales (usuario_id, alias, nombre_real, edad, habilidad, descripcion, nivel, estado,
                                         barrio, ciudad, pais, lat, lng, grupo_id, fecha_reclutamiento)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                p.usuarioId(), p.alias(), p.nombreReal(), p.edad(), p.habilidad(), p.descripcion(), p.nivel(),
                p.estado(), p.barrio(), p.ciudad(), p.pais(), p.lat(), p.lng(), p.grupoId(), p.fechaReclutamiento());
    }

    /**
     * Guarda los cambios de la ficha. No toca el saldo: el saldo solo cambia con movimientos del monedero.
     */
    public void actualizar(Potencial p) {
        Jdbc.update("""
                UPDATE potenciales SET alias = ?, nombre_real = ?, edad = ?, habilidad = ?, descripcion = ?, nivel = ?,
                       estado = ?, barrio = ?, ciudad = ?, pais = ?, lat = ?, lng = ?, grupo_id = ?, fecha_reclutamiento = ?
                WHERE id = ?""",
                p.alias(), p.nombreReal(), p.edad(), p.habilidad(), p.descripcion(), p.nivel(), p.estado(),
                p.barrio(), p.ciudad(), p.pais(), p.lat(), p.lng(), p.grupoId(), p.fechaReclutamiento(), p.id());
    }

    /** Mueve un potencial a otro grupo (null = sin grupo). El trigger de MySQL impide pasar de 6. */
    public void cambiarGrupo(int potencialId, Integer grupoId) {
        Jdbc.update("UPDATE potenciales SET grupo_id = ? WHERE id = ?", grupoId, potencialId);
    }

    /** Cambia el estado (DISPONIBLE, EN_MISION, HERIDO, INACTIVO). */
    public void cambiarEstado(int potencialId, EstadoPotencial estado) {
        Jdbc.update("UPDATE potenciales SET estado = ? WHERE id = ?", estado, potencialId);
    }

    /** Igual que el anterior, pero dentro de una transacción. */
    public void cambiarEstado(Connection c, int potencialId, EstadoPotencial estado) throws SQLException {
        Jdbc.update(c, "UPDATE potenciales SET estado = ? WHERE id = ?", estado, potencialId);
    }

    /** Borra un potencial (sus vínculos y movimientos se borran en cascada). */
    public void borrar(int id) {
        Jdbc.update("DELETE FROM potenciales WHERE id = ?", id);
    }
}
