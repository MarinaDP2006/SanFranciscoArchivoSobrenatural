package com.sfarchive.core.model;

/**
 * Tipos de movimiento del monedero de un potencial.
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code TipoTransaccion.valueOf(texto)}.
 */
public enum TipoTransaccion {
    /** Pago automático al completar un contrato (suma). */
    RECOMPENSA,
    /** Gasto automático al asignar un contrato (resta). */
    TRANSPORTE,
    /** Corrección manual de un admin (suma o resta según el signo). */
    AJUSTE,
    /** Premio manual (siempre suma). */
    BONUS,
    /** Sanción manual (siempre resta). */
    PENALIZACION;

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
