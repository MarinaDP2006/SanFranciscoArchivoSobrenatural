package com.sfarchive.core.model;

/**
 * Punto seguro (hospital, comisaría...) que la web pinta en verde en el mapa (tabla {@code zonas_seguras}).
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record ZonaSegura(
        /** Clave primaria (0 = nueva). */
        int id,
        /** Nombre del lugar. */
        String nombre,
        /** HOSPITAL, POLICIA, BOMBEROS, REFUGIO o TEMPLO. */
        TipoZona tipo,
        /** Dirección. */
        String direccion,
        /** Barrio. */
        String barrio,
        /** Latitud (obligatoria). */
        double lat,
        /** Longitud (obligatoria). */
        double lng,
        /** Teléfono. */
        String telefono,
        /** Horario de apertura. */
        String horario,
        /** Si se muestra en la web. */
        boolean activa
) { }
