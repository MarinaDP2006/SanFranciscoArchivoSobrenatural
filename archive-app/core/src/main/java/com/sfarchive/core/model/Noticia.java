package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Noticia publicada (o en borrador) para la web pública. */
public record Noticia(
        int id, String slug, String titulo, String resumen, String contenido,
        CategoriaNoticia categoria, String imagenUrl, String barrio, Integer incidenteId,
        boolean publicada, boolean destacada, LocalDateTime fechaPublicacion,
        Integer autorId, String autorNombre
) { }
