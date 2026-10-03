package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

/** Contadores para el dashboard de la aplicación. */
public class EstadisticasDao {

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

    public Map<String, Long> incidentesPorTipo() {
        Map<String, Long> m = new LinkedHashMap<>();
        Jdbc.query("SELECT tipo, COUNT(*) n FROM incidentes GROUP BY tipo ORDER BY tipo",
                rs -> m.put(rs.getString("tipo"), rs.getLong("n")));
        return m;
    }
}
