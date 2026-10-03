package com.sfarchive.core.model;

/**
 * Quién dio de alta el incidente.
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code OrigenIncidente.valueOf(texto)}.
 */
public enum OrigenIncidente {
    /** Lo creó un administrador desde la app. */
    ARCHIVO,
    /** Viene de un aviso anónimo enviado desde la web. */
    CIUDADANO,
    /** Lo reportó un potencial desde "Reportar incidente". */
    POTENCIAL;

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
