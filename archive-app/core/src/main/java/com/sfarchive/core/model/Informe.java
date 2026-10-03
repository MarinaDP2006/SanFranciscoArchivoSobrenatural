package com.sfarchive.core.model;

import java.time.LocalDateTime;

/**
 * Informe final clasificado de un contrato cerrado (tabla {@code informes_clasificados}). Solo lo ven los administradores.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Informe(
        /** Clave primaria. */
        int id,
        /** Contrato al que pertenece (uno por contrato). */
        int contratoId,
        /** Código del contrato (JOIN). */
        String contratoCodigo,
        /** Admin que lo escribió. */
        Integer autorId,
        /** Nombre del autor (JOIN). */
        String autorNombre,
        /** Título del informe. */
        String titulo,
        /** Nombre de la entidad anómala. */
        String entidad,
        /** Nivel de secreto. */
        Clasificacion clasificacion,
        /** Resumen en una línea. */
        String resumen,
        /** Texto completo del informe. */
        String contenido,
        /** Número de víctimas civiles. */
        int bajasCiviles,
        /** Cuándo se creó. */
        LocalDateTime fechaCreacion,
        /** Última modificación. */
        LocalDateTime fechaModificacion
) { }
