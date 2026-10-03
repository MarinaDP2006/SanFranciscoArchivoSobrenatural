package com.sfarchive.core.model;

import java.time.LocalDateTime;

/**
 * Incidente (tabla {@code incidentes}). Tiene DOS caras:
 * la pública ({@code titulo}, {@code descripcionPublica}...) que aparece en la web si {@code publicado} es true,
 * y la privada ({@code anomalia}), que nunca sale de la app.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Incidente(
        /** Clave primaria (0 = nuevo). */
        int id,
        /** Código visible, p. ej. SFA-2026-019 (se genera al guardar). */
        String codigo,
        /** Titular público. */
        String titulo,
        /** SECUESTRO, ASESINATO o DESAPARICION. */
        TipoIncidente tipo,
        /** Texto que lee el público en la web. */
        String descripcionPublica,
        /** Barrio. */
        String barrio,
        /** Calle o referencia. */
        String direccion,
        /** Ciudad. */
        String ciudad,
        /** País. */
        String pais,
        /** Latitud (para el mapa). */
        Double lat,
        /** Longitud. */
        Double lng,
        /** Cuándo ocurrió. */
        LocalDateTime fecha,
        /** Fase en el Archivo (ver EstadoIncidente). */
        EstadoIncidente estado,
        /** true = visible en la web pública. */
        boolean publicado,
        /** La verdad sobrenatural (CLASIFICADO, nunca se publica). */
        String anomalia,
        /** Peligro del 1 al 5. */
        int nivelAmenaza,
        /** Quién lo dio de alta (archivo, ciudadano o potencial). */
        OrigenIncidente origen,
        /** Usuario que lo creó. */
        Integer creadoPor
) { }
