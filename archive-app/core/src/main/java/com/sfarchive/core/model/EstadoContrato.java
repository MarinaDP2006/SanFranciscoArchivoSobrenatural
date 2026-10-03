package com.sfarchive.core.model;

/**
 * Ciclo de vida de un contrato (en este orden):
 * SOLICITADO → ASIGNADO → EN_CURSO → PENDIENTE_REVISION → COMPLETADO.
 * En cualquier momento puede acabar FALLIDO o CANCELADO.
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code EstadoContrato.valueOf(texto)}.
 */
public enum EstadoContrato {
    /** Creado, esperando a que un administrador elija grupo y potencial. */
    SOLICITADO,
    /** Ya tiene potencial (se cobró el transporte) pero aún no ha empezado. */
    ASIGNADO,
    /** El potencial ha pulsado "Iniciar misión". */
    EN_CURSO,
    /** El potencial ha enviado su informe de campo; falta que un admin lo cierre. */
    PENDIENTE_REVISION,
    /** Éxito: se pagó la recompensa y el incidente queda RESUELTO. */
    COMPLETADO,
    /** La misión salió mal: sin recompensa, el incidente sigue abierto. */
    FALLIDO,
    /** Anulado por un admin (si había transporte, se reembolsa). */
    CANCELADO;

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
