package com.sfarchive.core.model;

/**
 * Situación actual de un potencial. Solo los DISPONIBLES se pueden asignar a contratos.
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code EstadoPotencial.valueOf(texto)}.
 */
public enum EstadoPotencial {
    /** Libre para recibir una misión. */
    DISPONIBLE,
    /** Tiene un contrato asignado o en curso. */
    EN_MISION,
    /** De baja temporal. */
    HERIDO,
    /** Fuera de la red (retirado, suspendido...). */
    INACTIVO;

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
