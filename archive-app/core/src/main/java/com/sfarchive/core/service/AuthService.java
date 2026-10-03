package com.sfarchive.core.service;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.UsuarioDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.Usuario;
import com.sfarchive.core.security.PasswordHasher;

/** Inicio de sesión de la aplicación de gestión (solo administradores y potenciales). */
public class AuthService {

    private final UsuarioDao usuarios = new UsuarioDao();
    private final ActividadDao actividad = new ActividadDao();

    public Usuario login(String username, String password) {
        String user = username == null ? "" : username.trim().toLowerCase();
        String hash = usuarios.hashDe(user).orElse(null);
        // Se verifica siempre un hash (aunque el usuario no exista) para no revelar qué usuarios existen.
        boolean ok = PasswordHasher.verify(password == null ? "" : password,
                hash != null ? hash : "pbkdf2_sha256$65536$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        if (hash == null || !ok) throw new DataException("Usuario o contraseña incorrectos.");
        Usuario u = usuarios.porUsername(user).orElseThrow();
        if (!u.activo()) throw new DataException("La cuenta está desactivada. Contacta con un administrador.");
        usuarios.registrarAcceso(u.id());
        actividad.registrar(u.id(), "LOGIN", "USUARIO", u.id(), "Inicio de sesión en la aplicación");
        return u;
    }

    public void cambiarPassword(int usuarioId, String actual, String nueva) {
        String hash = usuarios.hashDe(usuarioId).orElseThrow(() -> new DataException("Usuario no encontrado."));
        if (!PasswordHasher.verify(actual, hash)) throw new DataException("La contraseña actual no es correcta.");
        String error = PasswordHasher.validarFortaleza(nueva);
        if (error != null) throw new DataException(error);
        usuarios.cambiarHash(usuarioId, PasswordHasher.hash(nueva));
        actividad.registrar(usuarioId, "PASSWORD", "USUARIO", usuarioId, "Cambio de contraseña");
    }

    /** Un administrador restablece la contraseña de otra cuenta. */
    public void restablecerPassword(int adminId, int usuarioId, String nueva) {
        String error = PasswordHasher.validarFortaleza(nueva);
        if (error != null) throw new DataException(error);
        usuarios.cambiarHash(usuarioId, PasswordHasher.hash(nueva));
        actividad.registrar(adminId, "RESET_PASSWORD", "USUARIO", usuarioId, "Contraseña restablecida por un administrador");
    }
}
