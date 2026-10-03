package com.sfarchive.desktop;

import com.sfarchive.core.model.Rol;
import com.sfarchive.core.model.Usuario;

/**
 * Guarda quién ha iniciado sesión en la aplicación.
 * <p>
 * Todo es static porque solo hay un usuario a la vez en la app de escritorio.
 * Las pantallas lo usan para saber el rol ({@code Sesion.esAdmin()}) o el potencial ({@code Sesion.potencialId()})
 * y para apuntar en la auditoría quién hace cada cosa ({@code Sesion.id()}).
 */
public final class Sesion {

    /** Usuario conectado (null si nadie ha iniciado sesión). */
    private static Usuario usuario;

    /** Clase de utilidades: solo métodos static. */
    private Sesion() { }

    /** Se llama tras un login correcto. */
    public static void iniciar(Usuario u) { usuario = u; }

    /** Se llama al cerrar sesión. */
    public static void cerrar() { usuario = null; }

    /** El usuario conectado. */
    public static Usuario usuario() { return usuario; }

    /** Id del usuario conectado. */
    public static int id() { return usuario.id(); }

    /** true si es administrador (James, Sarah o Nina). */
    public static boolean esAdmin() { return usuario != null && usuario.rol() == Rol.ADMIN; }

    /** Id de la ficha de potencial (solo para usuarios con rol POTENCIAL). */
    public static Integer potencialId() { return usuario == null ? null : usuario.potencialId(); }
}
