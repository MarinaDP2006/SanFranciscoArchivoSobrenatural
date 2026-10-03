package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Informe final clasificado (Archivo Restringido). */
public record Informe(
        int id, int contratoId, String contratoCodigo, Integer autorId, String autorNombre,
        String titulo, String entidad, Clasificacion clasificacion, String resumen, String contenido,
        int bajasCiviles, LocalDateTime fechaCreacion, LocalDateTime fechaModificacion
) { }
