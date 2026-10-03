package com.sfarchive.core.service;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.ContratoDao;
import com.sfarchive.core.dao.IncidenteDao;
import com.sfarchive.core.dao.SolicitudDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.EstadoIncidente;
import com.sfarchive.core.model.EstadoSolicitud;
import com.sfarchive.core.model.Incidente;
import com.sfarchive.core.model.OrigenIncidente;
import com.sfarchive.core.model.Prioridad;
import com.sfarchive.core.model.SolicitudAyuda;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Gestión de los avisos anónimos que llegan desde la web ("Pedir ayuda").
 * <p>
 * Es un servicio: contiene las REGLAS (qué se puede hacer y qué no) y usa los DAO para guardar.
 * Si algo no está permitido lanza {@code DataException} con el mensaje que verá el usuario.
 */
public class SolicitudService {

    // DAOs que necesita este servicio (cada uno habla con su tabla)
    private final SolicitudDao solicitudes = new SolicitudDao();
    private final IncidenteDao incidentes = new IncidenteDao();
    private final ContratoDao contratos = new ContratoDao();
    private final ActividadDao actividad = new ActividadDao();

    /**
     * Convierte el aviso en un incidente VERIFICADO (publicado en la web si {@code publicar} es true)
     * y crea un contrato SOLICITADO para poder asignarlo. El ciudadano verá "Atendido" con su código.
     *
     * @return id del contrato creado
     */
    public int convertir(int solicitudId, String titulo, boolean publicar, Prioridad prioridad,
                         BigDecimal recompensa, int adminId) {
        if (titulo == null || titulo.isBlank()) throw new DataException("Indica un titular para el incidente.");
        return Jdbc.inTransaction(c -> {
            SolicitudAyuda s = solicitudes.porId(solicitudId).orElseThrow(() -> new DataException("Solicitud no encontrada."));
            if (s.estado() == EstadoSolicitud.ATENDIDA || s.estado() == EstadoSolicitud.DESCARTADA)
                throw new DataException("La solicitud ya fue procesada.");
            Incidente nuevo = new Incidente(0, null, titulo.trim(), s.tipo(), s.descripcion(), s.barrio(), s.ubicacion(),
                    "San Francisco", "Estados Unidos", s.lat(), s.lng(), s.recibidaEn() != null ? s.recibidaEn() : LocalDateTime.now(),
                    EstadoIncidente.VERIFICADO, publicar, null, 2, OrigenIncidente.CIUDADANO, adminId);
            int incidenteId = incidentes.crear(c, nuevo);
            int contratoId = contratos.crear(c, incidenteId, solicitudId, prioridad, recompensa);
            solicitudes.actualizarEstado(c, solicitudId, EstadoSolicitud.ATENDIDA, incidenteId,
                    "Tu aviso ha sido atendido. Un equipo está trabajando en el caso.", adminId);
            actividad.registrar(c, adminId, "CONVERTIR", "SOLICITUD", solicitudId, s.codigo() + " → incidente " + incidenteId);
            return contratoId;
        });
    }

    /** Marca el aviso como "en revisión" (el ciudadano lo verá al consultar su código). */
    public void marcarEnRevision(int solicitudId, int adminId) {
        Jdbc.inTransaction(c -> {
            solicitudes.actualizarEstado(c, solicitudId, EstadoSolicitud.EN_REVISION, null,
                    "Estamos revisando tu aviso.", adminId);
            return null;
        });
    }

    /** Descarta el aviso (no había incidente real) con una respuesta para el ciudadano. */
    public void descartar(int solicitudId, String respuesta, int adminId) {
        Jdbc.inTransaction(c -> {
            solicitudes.actualizarEstado(c, solicitudId, EstadoSolicitud.DESCARTADA, null,
                    respuesta == null || respuesta.isBlank()
                            ? "Gracias por tu aviso. No hemos encontrado indicios de un incidente." : respuesta.trim(), adminId);
            actividad.registrar(c, adminId, "DESCARTAR", "SOLICITUD", solicitudId, null);
            return null;
        });
    }
}
