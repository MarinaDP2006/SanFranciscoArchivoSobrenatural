package com.sfarchive.core.model;

/**
 * Familiar o allegado de un potencial (tabla {@code vinculos_familiares}). El Archivo protege a las familias.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Vinculo(
        /** Clave primaria (0 = todavía no guardado). */
        int id,
        /** Potencial al que pertenece. */
        int potencialId,
        /** Nombre del familiar. */
        String nombre,
        /** Madre, hijo, esposa... */
        String parentesco,
        /** Edad (puede ser null). */
        Integer edad,
        /** Dónde vive. */
        String ciudad,
        /** Si sabe que su familiar es un potencial. */
        boolean conoceSecreto,
        /** Si necesita protección. */
        boolean enRiesgo,
        /** Observaciones. */
        String notas
) { }
