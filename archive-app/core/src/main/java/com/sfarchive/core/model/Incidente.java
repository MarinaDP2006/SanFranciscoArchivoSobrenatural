package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Incidente: la parte pública es la noticia; la anomalía es clasificada. */
public record Incidente(
        int id, String codigo, String titulo, TipoIncidente tipo, String descripcionPublica,
        String barrio, String direccion, String ciudad, String pais, Double lat, Double lng,
        LocalDateTime fecha, EstadoIncidente estado, boolean publicado, String anomalia,
        int nivelAmenaza, OrigenIncidente origen, Integer creadoPor
) { }
