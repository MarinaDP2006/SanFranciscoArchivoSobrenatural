package com.sfarchive.core.model;

/**
 * Grupo táctico de potenciales por país y zona (tabla {@code grupos_tacticos}). Máximo 6 miembros.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record GrupoTactico(
        /** Clave primaria. */
        int id,
        /** Nombre del grupo (único). */
        String nombre,
        /** País que cubre. */
        String pais,
        /** Ciudad base. */
        String ciudad,
        /** Barrios que cubre. */
        String zona,
        /** Latitud de la base del grupo. */
        Double lat,
        /** Longitud de la base. */
        Double lng,
        /** Notas del grupo. */
        String descripcion,
        /** Si está operativo. */
        boolean activo,
        /** Cuántos potenciales tiene ahora (se calcula con un COUNT en la consulta). */
        int miembros
) { }
