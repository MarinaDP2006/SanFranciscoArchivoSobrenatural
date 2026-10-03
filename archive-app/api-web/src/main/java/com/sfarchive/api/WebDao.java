package com.sfarchive.api;

import com.sfarchive.core.db.Jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Consultas de solo lectura de la web. Únicamente usa las vistas públicas
 * (v_incidentes_publicos, v_noticias_publicas): la anomalía clasificada nunca sale de aquí.
 */
public class WebDao {

    public record IncidentePublico(int id, String codigo, String titulo, String tipo, String descripcion,
                                   String barrio, String direccion, String ciudad, String pais,
                                   Double lat, Double lng, LocalDateTime fecha, String estado,
                                   LocalDateTime actualizado) { }

    public record NoticiaPublica(int id, String slug, String titulo, String resumen, String contenido,
                                 String categoria, String imagenUrl, String barrio, Integer incidenteId,
                                 boolean destacada, LocalDateTime fecha, String autor) { }

    public record ZonaPublica(int id, String nombre, String tipo, String direccion, String barrio,
                              double lat, double lng, String telefono, String horario) { }

    public record EstadoAyuda(String codigo, String tipo, String estado, LocalDateTime recibida, String respuesta) { }

    private static IncidentePublico incidente(ResultSet rs) throws SQLException {
        return new IncidentePublico(rs.getInt("id"), rs.getString("codigo"), rs.getString("titulo"),
                rs.getString("tipo"), rs.getString("descripcion_publica"), rs.getString("barrio"),
                rs.getString("direccion"), rs.getString("ciudad"), rs.getString("pais"),
                Jdbc.doubleOrNull(rs, "lat"), Jdbc.doubleOrNull(rs, "lng"), Jdbc.dateTime(rs, "fecha_incidente"),
                rs.getString("estado_publico"), Jdbc.dateTime(rs, "actualizado_en"));
    }

    private static NoticiaPublica noticia(ResultSet rs) throws SQLException {
        return new NoticiaPublica(rs.getInt("id"), rs.getString("slug"), rs.getString("titulo"),
                rs.getString("resumen"), rs.getString("contenido"), rs.getString("categoria"),
                rs.getString("imagen_url"), rs.getString("barrio"), Jdbc.intOrNull(rs, "incidente_id"),
                rs.getBoolean("destacada"), Jdbc.dateTime(rs, "fecha_publicacion"), rs.getString("autor"));
    }

    public List<IncidentePublico> incidentes(String tipo, String barrio, String texto) {
        StringBuilder sql = new StringBuilder("SELECT * FROM v_incidentes_publicos WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (tipo != null && !tipo.isBlank()) { sql.append(" AND tipo = ?"); params.add(tipo.toUpperCase()); }
        if (barrio != null && !barrio.isBlank()) { sql.append(" AND barrio = ?"); params.add(barrio); }
        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (titulo LIKE ? OR descripcion_publica LIKE ? OR barrio LIKE ?)");
            String like = "%" + texto.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        sql.append(" ORDER BY fecha_incidente DESC LIMIT 200");
        return Jdbc.query(sql.toString(), WebDao::incidente, params.toArray());
    }

    public Optional<IncidentePublico> incidente(int id) {
        return Jdbc.one("SELECT * FROM v_incidentes_publicos WHERE id = ?", WebDao::incidente, id);
    }

    public List<NoticiaPublica> noticias(String categoria, int limite) {
        if (categoria != null && !categoria.isBlank()) {
            return Jdbc.query("SELECT * FROM v_noticias_publicas WHERE categoria = ? ORDER BY destacada DESC, fecha_publicacion DESC LIMIT ?",
                    WebDao::noticia, categoria.toUpperCase(), limite);
        }
        return Jdbc.query("SELECT * FROM v_noticias_publicas ORDER BY fecha_publicacion DESC LIMIT ?", WebDao::noticia, limite);
    }

    public Optional<NoticiaPublica> noticia(String slug) {
        return Jdbc.one("SELECT * FROM v_noticias_publicas WHERE slug = ?", WebDao::noticia, slug);
    }

    public List<NoticiaPublica> noticiasDeIncidente(int incidenteId) {
        return Jdbc.query("SELECT * FROM v_noticias_publicas WHERE incidente_id = ? ORDER BY fecha_publicacion DESC",
                WebDao::noticia, incidenteId);
    }

    public List<ZonaPublica> zonasSeguras() {
        return Jdbc.query("SELECT * FROM zonas_seguras WHERE activa = 1 ORDER BY tipo, nombre",
                rs -> new ZonaPublica(rs.getInt("id"), rs.getString("nombre"), rs.getString("tipo"),
                        rs.getString("direccion"), rs.getString("barrio"), rs.getDouble("lat"), rs.getDouble("lng"),
                        rs.getString("telefono"), rs.getString("horario")));
    }

    public Map<String, Object> estadisticas() {
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Long> porTipo = new LinkedHashMap<>();
        porTipo.put("SECUESTRO", 0L);
        porTipo.put("ASESINATO", 0L);
        porTipo.put("DESAPARICION", 0L);
        Jdbc.query("SELECT tipo, COUNT(*) n FROM v_incidentes_publicos GROUP BY tipo",
                rs -> porTipo.put(rs.getString("tipo"), rs.getLong("n")));
        Map<String, Long> porEstado = new LinkedHashMap<>();
        Jdbc.query("SELECT estado_publico, COUNT(*) n FROM v_incidentes_publicos GROUP BY estado_publico",
                rs -> porEstado.put(rs.getString(1), rs.getLong(2)));
        List<Map<String, Object>> barrios = Jdbc.query("""
                SELECT barrio, COUNT(*) n FROM v_incidentes_publicos WHERE barrio IS NOT NULL
                GROUP BY barrio ORDER BY n DESC, barrio LIMIT 8""", rs -> {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("barrio", rs.getString("barrio"));
            b.put("total", rs.getLong("n"));
            return b;
        });
        m.put("total", porTipo.values().stream().mapToLong(Long::longValue).sum());
        m.put("porTipo", porTipo);
        m.put("porEstado", porEstado);
        m.put("barrios", barrios);
        m.put("ultimaActualizacion", Jdbc.one("SELECT MAX(actualizado_en) AS u FROM v_incidentes_publicos",
                rs -> Jdbc.dateTime(rs, "u")).orElse(null));
        return m;
    }

    public Optional<EstadoAyuda> estadoAyuda(String codigo) {
        return Jdbc.one("SELECT codigo_seguimiento, tipo, estado, recibida_en, respuesta_publica FROM solicitudes_ayuda WHERE codigo_seguimiento = ?",
                rs -> new EstadoAyuda(rs.getString(1), rs.getString(2), rs.getString(3), Jdbc.dateTime(rs, "recibida_en"),
                        rs.getString(5)), codigo);
    }
}
