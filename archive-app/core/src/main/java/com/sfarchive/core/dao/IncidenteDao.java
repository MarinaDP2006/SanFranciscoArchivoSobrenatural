package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.EstadoIncidente;
import com.sfarchive.core.model.Incidente;
import com.sfarchive.core.model.OrigenIncidente;
import com.sfarchive.core.model.TipoIncidente;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.List;
import java.util.Optional;

public class IncidenteDao {

    static Incidente map(ResultSet rs) throws SQLException {
        return new Incidente(rs.getInt("id"), rs.getString("codigo"), rs.getString("titulo"),
                TipoIncidente.valueOf(rs.getString("tipo")), rs.getString("descripcion_publica"),
                rs.getString("barrio"), rs.getString("direccion"), rs.getString("ciudad"), rs.getString("pais"),
                Jdbc.doubleOrNull(rs, "lat"), Jdbc.doubleOrNull(rs, "lng"), Jdbc.dateTime(rs, "fecha_incidente"),
                EstadoIncidente.valueOf(rs.getString("estado")), rs.getBoolean("publicado"),
                rs.getString("anomalia_clasificada"), rs.getInt("nivel_amenaza"),
                OrigenIncidente.valueOf(rs.getString("origen")), Jdbc.intOrNull(rs, "creado_por"));
    }

    public List<Incidente> listar() {
        return Jdbc.query("SELECT * FROM incidentes ORDER BY fecha_incidente DESC", IncidenteDao::map);
    }

    public Optional<Incidente> porId(int id) {
        return Jdbc.one("SELECT * FROM incidentes WHERE id = ?", IncidenteDao::map, id);
    }

    /** Incidentes que aún no tienen ningún contrato abierto (para crear uno nuevo). */
    public List<Incidente> sinContratoActivo() {
        return Jdbc.query("""
                SELECT i.* FROM incidentes i
                WHERE i.estado NOT IN ('RESUELTO','ARCHIVADO')
                  AND NOT EXISTS (SELECT 1 FROM contratos c WHERE c.incidente_id = i.id
                                  AND c.estado NOT IN ('COMPLETADO','FALLIDO','CANCELADO'))
                ORDER BY i.fecha_incidente DESC""", IncidenteDao::map);
    }

    public String siguienteCodigo(Connection c) throws SQLException {
        String prefijo = "SFA-" + Year.now().getValue() + "-";
        int n = Jdbc.one(c, "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 10) AS UNSIGNED)), 0) FROM incidentes WHERE codigo LIKE ?",
                rs -> rs.getInt(1), prefijo + "%").orElse(0);
        return prefijo + String.format("%03d", n + 1);
    }

    public int crear(Incidente i) {
        return Jdbc.inTransaction(c -> crear(c, i));
    }

    public int crear(Connection c, Incidente i) throws SQLException {
        return Jdbc.insert(c, """
                INSERT INTO incidentes (codigo, titulo, tipo, descripcion_publica, barrio, direccion, ciudad, pais, lat, lng,
                                        fecha_incidente, estado, publicado, anomalia_clasificada, nivel_amenaza, origen, creado_por)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                siguienteCodigo(c), i.titulo(), i.tipo(), i.descripcionPublica(), i.barrio(), i.direccion(),
                i.ciudad(), i.pais(), i.lat(), i.lng(), i.fecha(), i.estado(), i.publicado(), i.anomalia(),
                i.nivelAmenaza(), i.origen(), i.creadoPor());
    }

    public void actualizar(Incidente i) {
        Jdbc.update("""
                UPDATE incidentes SET titulo = ?, tipo = ?, descripcion_publica = ?, barrio = ?, direccion = ?, ciudad = ?,
                       pais = ?, lat = ?, lng = ?, fecha_incidente = ?, estado = ?, publicado = ?, anomalia_clasificada = ?,
                       nivel_amenaza = ?
                WHERE id = ?""",
                i.titulo(), i.tipo(), i.descripcionPublica(), i.barrio(), i.direccion(), i.ciudad(), i.pais(),
                i.lat(), i.lng(), i.fecha(), i.estado(), i.publicado(), i.anomalia(), i.nivelAmenaza(), i.id());
    }

    public void setPublicado(int id, boolean publicado) {
        Jdbc.update("UPDATE incidentes SET publicado = ? WHERE id = ?", publicado, id);
    }

    public void setEstado(Connection c, int id, EstadoIncidente estado) throws SQLException {
        Jdbc.update(c, "UPDATE incidentes SET estado = ? WHERE id = ?", estado, id);
    }

    public void borrar(int id) {
        Jdbc.update("DELETE FROM incidentes WHERE id = ?", id);
    }
}
