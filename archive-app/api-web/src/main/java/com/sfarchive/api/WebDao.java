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

    /** Incidente tal como lo ve el público: SIN anomalía clasificada. Se envía a la web como JSON. */
    public record IncidentePublico(int id, String codigo, String titulo, String tipo, String descripcion,
                                   String barrio, String direccion, String ciudad, String pais,
                                   Double lat, Double lng, LocalDateTime fecha, String estado,
                                   LocalDateTime actualizado) { }

    /** Noticia publicada, tal como se envía a la web. */
    public record NoticiaPublica(int id, String slug, String titulo, String resumen, String contenido,
                                 String categoria, String imagenUrl, String barrio, Integer incidenteId,
                                 boolean destacada, LocalDateTime fecha, String autor) { }

    /** Zona segura activa (pin verde del mapa). */
    public record ZonaPublica(int id, String nombre, String tipo, String direccion, String barrio,
                              double lat, double lng, String telefono, String horario) { }

    /** Lo que ve el ciudadano al consultar su código: estado y respuesta, nada más. */
    public record EstadoAyuda(String codigo, String tipo, String estado, LocalDateTime recibida, String respuesta) { }

    /** Convierte una fila de la vista v_incidentes_publicos en un IncidentePublico. */
    private static IncidentePublico incidente(ResultSet rs) throws SQLException {
        return new IncidentePublico(rs.getInt("id"), rs.getString("codigo"), rs.getString("titulo"),
                rs.getString("tipo"), rs.getString("descripcion_publica"), rs.getString("barrio"),
                rs.getString("direccion"), rs.getString("ciudad"), rs.getString("pais"),
                Jdbc.doubleOrNull(rs, "lat"), Jdbc.doubleOrNull(rs, "lng"), Jdbc.dateTime(rs, "fecha_incidente"),
                rs.getString("estado_publico"), Jdbc.dateTime(rs, "actualizado_en"));
    }

    /** Convierte una fila de la vista v_noticias_publicas en una NoticiaPublica. */
    private static NoticiaPublica noticia(ResultSet rs) throws SQLException {
        return new NoticiaPublica(rs.getInt("id"), rs.getString("slug"), rs.getString("titulo"),
                rs.getString("resumen"), rs.getString("contenido"), rs.getString("categoria"),
                rs.getString("imagen_url"), rs.getString("barrio"), Jdbc.intOrNull(rs, "incidente_id"),
                rs.getBoolean("destacada"), Jdbc.dateTime(rs, "fecha_publicacion"), rs.getString("autor"));
    }

    /**
     * Incidentes publicados con filtros opcionales (tipo, barrio y texto libre).
     * El SQL se construye añadiendo condiciones solo para los filtros que llegan (siempre con ?).
     */
    public List<IncidentePublico> incidentes(String tipo, String barrio, String texto) {
        // "WHERE 1=1" es un truco: así cada filtro puede añadirse siempre con " AND ..."
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

    /** Un incidente publicado por id (vacío si no existe o no es público). */
    public Optional<IncidentePublico> incidente(int id) {
        return Jdbc.one("SELECT * FROM v_incidentes_publicos WHERE id = ?", WebDao::incidente, id);
    }

    /** Noticias publicadas (opcionalmente de una categoría), como mucho {@code limite}. */
    public List<NoticiaPublica> noticias(String categoria, int limite) {
        if (categoria != null && !categoria.isBlank()) {
            return Jdbc.query("SELECT * FROM v_noticias_publicas WHERE categoria = ? ORDER BY destacada DESC, fecha_publicacion DESC LIMIT ?",
                    WebDao::noticia, categoria.toUpperCase(), limite);
        }
        return Jdbc.query("SELECT * FROM v_noticias_publicas ORDER BY fecha_publicacion DESC LIMIT ?", WebDao::noticia, limite);
    }

    /** Una noticia por su slug (el texto de la URL). */
    public Optional<NoticiaPublica> noticia(String slug) {
        return Jdbc.one("SELECT * FROM v_noticias_publicas WHERE slug = ?", WebDao::noticia, slug);
    }

    /** Noticias relacionadas con un incidente (para la página del expediente). */
    public List<NoticiaPublica> noticiasDeIncidente(int incidenteId) {
        return Jdbc.query("SELECT * FROM v_noticias_publicas WHERE incidente_id = ? ORDER BY fecha_publicacion DESC",
                WebDao::noticia, incidenteId);
    }

    /** Zonas seguras activas. */
    public List<ZonaPublica> zonasSeguras() {
        return Jdbc.query("SELECT * FROM zonas_seguras WHERE activa = 1 ORDER BY tipo, nombre",
                rs -> new ZonaPublica(rs.getInt("id"), rs.getString("nombre"), rs.getString("tipo"),
                        rs.getString("direccion"), rs.getString("barrio"), rs.getDouble("lat"), rs.getDouble("lng"),
                        rs.getString("telefono"), rs.getString("horario")));
    }

    /**
     * Contadores para la portada de la web: total, por tipo, por estado y los barrios con más casos.
     * Se devuelve un Map que Jackson convierte en un objeto JSON.
     */
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

    /** Estado de un aviso ciudadano a partir de su código. */
    public Optional<EstadoAyuda> estadoAyuda(String codigo) {
        return Jdbc.one("SELECT codigo_seguimiento, tipo, estado, recibida_en, respuesta_publica FROM solicitudes_ayuda WHERE codigo_seguimiento = ?",
                rs -> new EstadoAyuda(rs.getString(1), rs.getString(2), rs.getString(3), Jdbc.dateTime(rs, "recibida_en"),
                        rs.getString(5)), codigo);
    }
}
