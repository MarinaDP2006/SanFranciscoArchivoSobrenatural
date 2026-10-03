package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.Contrato;
import com.sfarchive.core.model.EstadoContrato;
import com.sfarchive.core.model.Prioridad;
import com.sfarchive.core.model.TipoIncidente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.List;
import java.util.Optional;

public class ContratoDao {

    private static final String SELECT = """
            SELECT c.*, i.codigo AS inc_codigo, i.titulo AS inc_titulo, i.tipo AS inc_tipo, i.barrio AS inc_barrio,
                   i.lat AS inc_lat, i.lng AS inc_lng, g.nombre AS grupo_nombre, p.alias AS potencial_alias,
                   EXISTS (SELECT 1 FROM informes_clasificados f WHERE f.contrato_id = c.id) AS tiene_informe
            FROM contratos c
            JOIN incidentes i ON i.id = c.incidente_id
            LEFT JOIN grupos_tacticos g ON g.id = c.grupo_id
            LEFT JOIN potenciales p ON p.id = c.potencial_id
            """;

    // El espacio inicial es necesario: los bloques de texto eliminan la sangría común.
    private static final String ORDEN = " ORDER BY FIELD(c.estado,'SOLICITADO','PENDIENTE_REVISION','ASIGNADO','EN_CURSO','COMPLETADO','FALLIDO','CANCELADO'),"
            + " FIELD(c.prioridad,'CRITICA','ALTA','MEDIA','BAJA'), c.fecha_solicitud DESC";

    static Contrato map(ResultSet rs) throws SQLException {
        return new Contrato(rs.getInt("id"), rs.getString("codigo"), rs.getInt("incidente_id"),
                rs.getString("inc_codigo"), rs.getString("inc_titulo"), TipoIncidente.valueOf(rs.getString("inc_tipo")),
                rs.getString("inc_barrio"), Jdbc.doubleOrNull(rs, "inc_lat"), Jdbc.doubleOrNull(rs, "inc_lng"),
                Jdbc.intOrNull(rs, "solicitud_id"), EstadoContrato.valueOf(rs.getString("estado")),
                Prioridad.valueOf(rs.getString("prioridad")), Jdbc.intOrNull(rs, "grupo_id"),
                rs.getString("grupo_nombre"), Jdbc.intOrNull(rs, "potencial_id"), rs.getString("potencial_alias"),
                rs.getBigDecimal("recompensa"), rs.getBigDecimal("coste_transporte"), rs.getBigDecimal("distancia_km"),
                rs.getString("notas_campo"), Jdbc.dateTime(rs, "fecha_solicitud"), Jdbc.dateTime(rs, "fecha_asignacion"),
                Jdbc.dateTime(rs, "fecha_cierre"), rs.getBoolean("tiene_informe"));
    }

    public List<Contrato> listar() {
        return Jdbc.query(SELECT + ORDEN, ContratoDao::map);
    }

    public List<Contrato> porEstado(EstadoContrato estado) {
        return Jdbc.query(SELECT + " WHERE c.estado = ?" + ORDEN, ContratoDao::map, estado);
    }

    public List<Contrato> porPotencial(int potencialId) {
        return Jdbc.query(SELECT + " WHERE c.potencial_id = ?" + ORDEN, ContratoDao::map, potencialId);
    }

    /** Contratos cerrados (completados o fallidos) para el Archivo Restringido. */
    public List<Contrato> cerrados() {
        return Jdbc.query(SELECT + " WHERE c.estado IN ('COMPLETADO','FALLIDO') ORDER BY c.fecha_cierre DESC", ContratoDao::map);
    }

    public Optional<Contrato> porId(int id) {
        return Jdbc.one(SELECT + " WHERE c.id = ?", ContratoDao::map, id);
    }

    public Optional<Contrato> porId(Connection c, int id) throws SQLException {
        return Jdbc.one(c, SELECT + " WHERE c.id = ?", ContratoDao::map, id);
    }

    public int crear(Connection c, int incidenteId, Integer solicitudId, Prioridad prioridad, BigDecimal recompensa)
            throws SQLException {
        String prefijo = "CTR-" + Year.now().getValue() + "-";
        int n = Jdbc.one(c, "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 10) AS UNSIGNED)), 0) FROM contratos WHERE codigo LIKE ?",
                rs -> rs.getInt(1), prefijo + "%").orElse(0);
        return Jdbc.insert(c, """
                INSERT INTO contratos (codigo, incidente_id, solicitud_id, prioridad, recompensa) VALUES (?,?,?,?,?)""",
                prefijo + String.format("%03d", n + 1), incidenteId, solicitudId, prioridad, recompensa);
    }

    public void actualizarDatos(int id, Prioridad prioridad, BigDecimal recompensa) {
        Jdbc.update("UPDATE contratos SET prioridad = ?, recompensa = ? WHERE id = ?", prioridad, recompensa, id);
    }

    public long contar(EstadoContrato estado) {
        return Jdbc.count("SELECT COUNT(*) FROM contratos WHERE estado = ?", estado);
    }
}
