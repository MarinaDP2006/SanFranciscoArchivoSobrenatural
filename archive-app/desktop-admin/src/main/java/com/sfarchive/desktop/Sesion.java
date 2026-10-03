package com.sfarchive.desktop;

import com.sfarchive.core.model.Rol;
import com.sfarchive.core.model.Usuario;

/** Usuario que ha iniciado sesión en la aplicación. */
public final class Sesion {

    private static Usuario usuario;

    private Sesion() { }

    public static void iniciar(Usuario u) { usuario = u; }

    public static void cerrar() { usuario = null; }

    public static Usuario usuario() { return usuario; }

    public static int id() { return usuario.id(); }

    public static boolean esAdmin() { return usuario != null && usuario.rol() == Rol.ADMIN; }

    /** Id de la ficha de potencial (solo para usuarios con rol POTENCIAL). */
    public static Integer potencialId() { return usuario == null ? null : usuario.potencialId(); }
}
