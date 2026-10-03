package com.sfarchive.core.model;

/**
 * Tipo de cuenta que puede entrar en la aplicación de gestión.
 * Los ciudadanos NO tienen rol: nunca inician sesión (usan la web pública).
 * <p>
 * Los valores se escriben EXACTAMENTE igual que en el ENUM de la tabla MySQL,
 * así se puede convertir el texto de la BD con {@code Rol.valueOf(texto)}.
 */
public enum Rol {
    /** James, Sarah y Nina: acceso a todas las pantallas. */
    ADMIN,
    /** Agente de la red: solo ve sus misiones, su monedero y sus vínculos. */
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
