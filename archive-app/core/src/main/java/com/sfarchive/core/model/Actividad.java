package com.sfarchive.core.model;

import java.time.LocalDateTime;

/**
 * Una línea del registro de auditoría (tabla {@code registro_actividad}): quién hizo qué y cuándo.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Actividad(
        /** Clave primaria. */
        int id,
        /** Usuario que hizo la acción (JOIN). */
        String usuario,
        /** Qué hizo (ASIGNAR, PUBLICAR, LOGIN...). */
        String accion,
        /** Sobre qué tipo de cosa (CONTRATO, NOTICIA...). */
        String entidad,
        /** Id de esa cosa. */
        Integer entidadId,
        /** Descripción. */
        String detalle,
        /** Cuándo. */
        LocalDateTime fecha
) { }
