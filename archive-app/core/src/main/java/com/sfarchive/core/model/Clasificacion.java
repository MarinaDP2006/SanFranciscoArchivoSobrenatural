package com.sfarchive.core.model;

/**
 * Nivel de secreto de un informe del Archivo Restringido (de menos a más).
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code Clasificacion.valueOf(texto)}.
 */
public enum Clasificacion {
    /** Uso interno. */
    CONFIDENCIAL,
    /** Solo administradores. */
    SECRETO,
    /** Información muy sensible. */
    ALTO_SECRETO,
    /** Lo más peligroso: entidades que siguen activas. */
    OMEGA;

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
