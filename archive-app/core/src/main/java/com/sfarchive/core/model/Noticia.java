package com.sfarchive.core.model;

import java.time.LocalDateTime;

/**
 * Noticia de la web pública (tabla {@code noticias}). Solo se ve en la web si {@code publicada} es true.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Noticia(
        /** Clave primaria (0 = nueva). */
        int id,
        /** Texto para la URL (noticia.html?slug=...). Se genera del título. */
        String slug,
        /** Titular. */
        String titulo,
        /** Entradilla. */
        String resumen,
        /** Texto completo (párrafos separados por una línea en blanco). */
        String contenido,
        /** Sección. */
        CategoriaNoticia categoria,
        /** URL de imagen opcional. */
        String imagenUrl,
        /** Barrio relacionado. */
        String barrio,
        /** Incidente relacionado (opcional). */
        Integer incidenteId,
        /** true = visible en la web. */
        boolean publicada,
        /** true = sale grande en portada. */
        boolean destacada,
        /** Fecha de publicación. */
        LocalDateTime fechaPublicacion,
        /** Usuario que la escribió. */
        Integer autorId,
        /** Nombre del autor (JOIN). */
        String autorNombre
) { }
