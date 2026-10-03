package com.sfarchive.core.model;

/**
 * Tipos de zona segura (pins verdes del mapa público).
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code TipoZona.valueOf(texto)}.
 */
public enum TipoZona {
    /** Hospital o urgencias. */
    HOSPITAL,
    /** Comisaría. */
    POLICIA,
    /** Parque de bomberos. */
    BOMBEROS,
    /** Refugio o centro comunitario. */
    REFUGIO,
    /** Iglesia o templo (suelo consagrado). */
    TEMPLO;

    /**
     * Texto bonito para mostrar en pantalla.
     * Ejemplo: EN_CURSO → "En curso".
     */
    public String etiqueta() {
        String s = name().replace('_', ' ').toLowerCase();       // "EN_CURSO" → "en curso"
        return Character.toUpperCase(s.charAt(0)) + s.substring(1); // → "En curso"
    }

    /** Los ComboBox y las tablas usan toString(): así muestran la etiqueta. */
    @Override
    public String toString() {
        return etiqueta();
    }
}
