package com.sfarchive.core.model;

/**
 * Fase en la que está un incidente dentro del Archivo.
 * En la web se resume como ABIERTO, CERRADO o SIN CONFIRMAR (ver la vista v_incidentes_publicos).
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code EstadoIncidente.valueOf(texto)}.
 */
public enum EstadoIncidente {
    /** Recién llegado (aviso ciudadano o reporte de un potencial). Nadie lo ha comprobado aún. */
    NO_VERIFICADO,
    /** Un administrador ha confirmado que es real. */
    VERIFICADO,
    /** Hay un contrato asignado y un potencial trabajando en él. */
    EN_INVESTIGACION,
    /** El contrato se completó: la anomalía está resuelta. */
    RESUELTO,
    /** Cerrado sin resolver del todo (por ejemplo, tras un contrato fallido). */
    ARCHIVADO;

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
