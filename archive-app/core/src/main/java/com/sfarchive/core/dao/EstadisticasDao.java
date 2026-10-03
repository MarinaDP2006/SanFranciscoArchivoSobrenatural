package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contadores para las tarjetas del Dashboard de la aplicación.
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class EstadisticasDao {

    /**
     * Devuelve un mapa nombre → número (incidentes, sin verificar, contratos solicitados...).
     * LinkedHashMap mantiene el orden en que se añaden.
     */
    public Map<String, Long> resumen() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("incidentes", Jdbc.count("SELECT COUNT(*) FROM incidentes"));
        m.put("sinVerificar", Jdbc.count("SELECT COUNT(*) FROM incidentes WHERE estado = 'NO_VERIFICADO'"));
        m.put("publicados", Jdbc.count("SELECT COUNT(*) FROM incidentes WHERE publicado = 1"));
        m.put("contratosSolicitados", Jdbc.count("SELECT COUNT(*) FROM contratos WHERE estado = 'SOLICITADO'"));
        m.put("contratosActivos", Jdbc.count("SELECT COUNT(*) FROM contratos WHERE estado IN ('ASIGNADO','EN_CURSO','PENDIENTE_REVISION')"));
        m.put("contratosCompletados", Jdbc.count("SELECT COUNT(*) FROM contratos WHERE estado = 'COMPLETADO'"));
        m.put("potencialesDisponibles", Jdbc.count("SELECT COUNT(*) FROM potenciales WHERE estado = 'DISPONIBLE'"));
        m.put("potenciales", Jdbc.count("SELECT COUNT(*) FROM potenciales"));
        m.put("solicitudesPendientes", Jdbc.count("SELECT COUNT(*) FROM solicitudes_ayuda WHERE estado = 'PENDIENTE'"));
        return m;
    }

    /** Número de incidentes de cada tipo. */
    public Map<String, Long> incidentesPorTipo() {
        Map<String, Long> m = new LinkedHashMap<>();
        Jdbc.query("SELECT tipo, COUNT(*) n FROM incidentes GROUP BY tipo ORDER BY tipo",
                rs -> m.put(rs.getString("tipo"), rs.getLong("n")));
        return m;
    }
}
