package com.sfarchive.core.model;

/**
 * Los 3 tipos de incidente que el público ve como "noticia normal".
 * Detrás de cada uno se esconde una anomalía sobrenatural.
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code TipoIncidente.valueOf(texto)}.
 */
public enum TipoIncidente {
    /** Alguien se ha llevado a una persona (color ámbar en los mapas). */
    SECUESTRO,
    /** Muerte violenta o inexplicable (color rojo). */
    ASESINATO,
    /** Persona desaparecida sin explicación (color turquesa). */
    DESAPARICION;

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
