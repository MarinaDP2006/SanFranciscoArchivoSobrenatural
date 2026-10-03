package com.sfarchive.core.model;

/**
 * Estado de un aviso ciudadano. El ciudadano lo consulta en la web con su código.
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code EstadoSolicitud.valueOf(texto)}.
 */
public enum EstadoSolicitud {
    /** Recibido, nadie lo ha mirado. */
    PENDIENTE,
    /** Un admin lo está estudiando. */
    EN_REVISION,
    /** Convertido en incidente + contrato. */
    ATENDIDA,
    /** No había incidente real (bromas, errores...). */
    DESCARTADA;

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
