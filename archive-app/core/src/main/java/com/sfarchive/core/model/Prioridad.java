package com.sfarchive.core.model;

/**
 * Urgencia de un contrato. Las tablas se ordenan de CRITICA a BAJA.
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code Prioridad.valueOf(texto)}.
 */
public enum Prioridad {
    /** Puede esperar. */
    BAJA,
    /** Prioridad normal. */
    MEDIA,
    /** Atender cuanto antes. */
    ALTA,
    /** Hay vidas en peligro inmediato. */
    CRITICA;

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
