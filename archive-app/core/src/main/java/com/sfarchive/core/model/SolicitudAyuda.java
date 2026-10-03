package com.sfarchive.core.model;

import java.time.LocalDateTime;

/**
 * Aviso anónimo que envía un ciudadano desde el formulario "Pedir ayuda" de la web (tabla {@code solicitudes_ayuda}).
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record SolicitudAyuda(
        /** Clave primaria. */
        int id,
        /** Código de seguimiento que recibe el ciudadano (SF-XXXXXX). */
        String codigo,
        /** Qué cree que ha pasado. */
        TipoIncidente tipo,
        /** Lo que cuenta el ciudadano. */
        String descripcion,
        /** Barrio elegido en el formulario. */
        String barrio,
        /** Calle o referencia. */
        String ubicacion,
        /** Latitud si marcó el punto en el mapa (si no, null). */
        Double lat,
        /** Longitud. */
        Double lng,
        /** Email/teléfono opcional. */
        String contacto,
        /** PENDIENTE, EN_REVISION, ATENDIDA o DESCARTADA. */
        EstadoSolicitud estado,
        /** Incidente creado a partir del aviso (si se convirtió). */
        Integer incidenteId,
        /** Mensaje que verá el ciudadano al consultar su código. */
        String respuestaPublica,
        /** Fecha de envío. */
        LocalDateTime recibidaEn
) { }
