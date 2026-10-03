package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Aviso anónimo enviado por un ciudadano desde la web. */
public record SolicitudAyuda(
        int id, String codigo, TipoIncidente tipo, String descripcion, String barrio, String ubicacion,
        Double lat, Double lng, String contacto, EstadoSolicitud estado, Integer incidenteId,
        String respuestaPublica, LocalDateTime recibidaEn
) { }
