package com.sfarchive.core.model;

/**
 * Secciones de las noticias de la web (filtro de la página Noticias).
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code CategoriaNoticia.valueOf(texto)}.
 */
public enum CategoriaNoticia {
    /** Crónica de incidentes. */
    SUCESOS,
    /** Información general de San Francisco. */
    CIUDAD,
    /** Avisos de seguridad (cierres, recomendaciones). */
    AVISO,
    /** Vecinos, ayuda ciudadana. */
    COMUNIDAD,
    /** Historia de la ciudad (1906, Alcatraz...). */
    HISTORIA;

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
