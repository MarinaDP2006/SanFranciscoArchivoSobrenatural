package com.sfarchive.core.model;

public enum EstadoPotencial {
    DISPONIBLE, EN_MISION, HERIDO, INACTIVO;

    /** Texto legible para mostrar en pantallas (SOLO_MAYUS → Solo mayus). */
    public String etiqueta() {
        String s = name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public String toString() {
        return etiqueta();
    }
}
