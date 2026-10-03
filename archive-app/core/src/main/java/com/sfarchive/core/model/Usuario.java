package com.sfarchive.core.model;

import java.time.LocalDateTime;

/**
 * Cuenta de acceso a la aplicación de gestión (tabla {@code usuarios}).
 * Solo existen cuentas para administradores y potenciales: los ciudadanos nunca se registran.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Usuario(
        /** Clave primaria. */
        int id,
        /** Nombre de usuario para el login (en minúsculas, p. ej. "nina"). */
        String username,
        /** Correo de la cuenta. */
        String email,
        /** ADMIN o POTENCIAL: decide qué menú ve en la app. */
        Rol rol,
        /** Nombre que se muestra en pantalla. */
        String nombreCompleto,
        /** Si es false no puede iniciar sesión. */
        boolean activo,
        /** Fecha del último login (puede ser null). */
        LocalDateTime ultimoAcceso,
        /** Id de su ficha en {@code potenciales} si es POTENCIAL; null si es ADMIN. */
        Integer potencialId
) { }
