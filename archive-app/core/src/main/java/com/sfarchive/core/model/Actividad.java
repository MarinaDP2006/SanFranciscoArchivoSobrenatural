package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Entrada del registro de auditoría. */
public record Actividad(
        int id, String usuario, String accion, String entidad, Integer entidadId, String detalle,
        LocalDateTime fecha
) { }
