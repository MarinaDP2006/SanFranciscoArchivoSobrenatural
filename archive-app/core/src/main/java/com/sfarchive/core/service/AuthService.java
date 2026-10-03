package com.sfarchive.core.service;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.UsuarioDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.model.Usuario;
import com.sfarchive.core.security.PasswordHasher;

/**
 * Inicio de sesión y contraseñas de la aplicación de gestión (solo administradores y potenciales).
 * <p>
 * Es un servicio: contiene las REGLAS (qué se puede hacer y qué no) y usa los DAO para guardar.
 * Si algo no está permitido lanza {@code DataException} con el mensaje que verá el usuario.
 */
public class AuthService {

    // DAOs que necesita este servicio (cada uno habla con su tabla)
    private final UsuarioDao usuarios = new UsuarioDao();
    private final ActividadDao actividad = new ActividadDao();

    /**
     * Comprueba usuario y contraseña. Si son correctos devuelve el usuario y apunta el acceso.
     * El mensaje de error es el mismo si falla el usuario o la contraseña: así no se da pistas a un intruso.
     */
    public Usuario login(String username, String password) {
        // Normalizamos: sin espacios y en minúsculas ("Nina " → "nina")
        String user = username == null ? "" : username.trim().toLowerCase();
        String hash = usuarios.hashDe(user).orElse(null);
        // Se verifica siempre un hash (aunque el usuario no exista) para no revelar qué usuarios existen.
        boolean ok = PasswordHasher.verify(password == null ? "" : password,
                hash != null ? hash : "pbkdf2_sha256$65536$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        // Usuario inexistente o contraseña incorrecta → mismo mensaje
        if (hash == null || !ok) throw new DataException("Usuario o contraseña incorrectos.");
        Usuario u = usuarios.porUsername(user).orElseThrow();
        // Una cuenta desactivada desde "Usuarios y actividad" no puede entrar
        if (!u.activo()) throw new DataException("La cuenta está desactivada. Contacta con un administrador.");
        // Todo correcto: guardamos la hora del acceso y lo apuntamos en la auditoría
        usuarios.registrarAcceso(u.id());
        actividad.registrar(u.id(), "LOGIN", "USUARIO", u.id(), "Inicio de sesión en la aplicación");
        return u;
    }

    /** El propio usuario cambia su contraseña (pantalla Mi perfil): debe saber la actual. */
    public void cambiarPassword(int usuarioId, String actual, String nueva) {
        String hash = usuarios.hashDe(usuarioId).orElseThrow(() -> new DataException("Usuario no encontrado."));
        if (!PasswordHasher.verify(actual, hash)) throw new DataException("La contraseña actual no es correcta.");
        String error = PasswordHasher.validarFortaleza(nueva);
        if (error != null) throw new DataException(error);
        usuarios.cambiarHash(usuarioId, PasswordHasher.hash(nueva));
        actividad.registrar(usuarioId, "PASSWORD", "USUARIO", usuarioId, "Cambio de contraseña");
    }

    /** Un administrador pone una contraseña nueva a otra cuenta (sin saber la anterior). */
    public void restablecerPassword(int adminId, int usuarioId, String nueva) {
        String error = PasswordHasher.validarFortaleza(nueva);
        if (error != null) throw new DataException(error);
        usuarios.cambiarHash(usuarioId, PasswordHasher.hash(nueva));
        actividad.registrar(adminId, "RESET_PASSWORD", "USUARIO", usuarioId, "Contraseña restablecida por un administrador");
    }
}
