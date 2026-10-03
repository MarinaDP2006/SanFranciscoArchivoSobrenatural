package com.sfarchive.core.dao;

import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.Rol;
import com.sfarchive.core.model.Usuario;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code usuarios} (cuentas de la app).
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class UsuarioDao {

    /**
     * Consulta base: el usuario + el id de su ficha de potencial (si la tiene).
     * Los métodos le añaden WHERE / ORDER BY.
     */
    private static final String SELECT = """
            SELECT u.*, p.id AS potencial_id
            FROM usuarios u LEFT JOIN potenciales p ON p.usuario_id = u.id
            """;

    /**
     * Convierte la fila actual del ResultSet en un objeto Usuario.
     * Se usa como "mapper": {@code Jdbc.query(sql, UsuarioDao::map)}.
     */
    static Usuario map(ResultSet rs) throws SQLException {
        return new Usuario(rs.getInt("id"), rs.getString("username"), rs.getString("email"),
                Rol.valueOf(rs.getString("rol")), rs.getString("nombre_completo"),
                rs.getBoolean("activo"), Jdbc.dateTime(rs, "ultimo_acceso"),
                Jdbc.intOrNull(rs, "potencial_id"));
    }

    /** Todas las cuentas, primero admins y luego potenciales. */
    public List<Usuario> listar() {
        return Jdbc.query(SELECT + " ORDER BY u.rol, u.username", UsuarioDao::map);
    }

    /** Busca una cuenta por su id. */
    public Optional<Usuario> porId(int id) {
        return Jdbc.one(SELECT + " WHERE u.id = ?", UsuarioDao::map, id);
    }

    /** Busca una cuenta por su nombre de usuario (para el login). */
    public Optional<Usuario> porUsername(String username) {
        return Jdbc.one(SELECT + " WHERE u.username = ?", UsuarioDao::map, username);
    }

    /** Devuelve el hash de la contraseña de un usuario (para comprobarla en el login). */
    public Optional<String> hashDe(String username) {
        return Jdbc.one("SELECT password_hash FROM usuarios WHERE username = ?", rs -> rs.getString(1), username);
    }

    /** Devuelve el hash de la contraseña por id (para cambiar la contraseña). */
    public Optional<String> hashDe(int id) {
        return Jdbc.one("SELECT password_hash FROM usuarios WHERE id = ?", rs -> rs.getString(1), id);
    }

    /**
     * Crea una cuenta nueva dentro de una transacción. Devuelve el id generado.
     * Recibe el hash, NUNCA la contraseña en claro.
     */
    public int crear(Connection c, String username, String email, String hash, Rol rol, String nombre) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO usuarios (username, email, password_hash, rol, nombre_completo) VALUES (?,?,?,?,?)",
                username, email, hash, rol, nombre);
    }

    /** Cambia email, nombre y si la cuenta está activa. */
    public void actualizar(int id, String email, String nombre, boolean activo) {
        Jdbc.update("UPDATE usuarios SET email = ?, nombre_completo = ?, activo = ? WHERE id = ?", email, nombre, activo, id);
    }

    /** Guarda una contraseña nueva (ya convertida en hash). */
    public void cambiarHash(int id, String hash) {
        Jdbc.update("UPDATE usuarios SET password_hash = ? WHERE id = ?", hash, id);
    }

    /** Apunta la fecha y hora del último inicio de sesión. */
    public void registrarAcceso(int id) {
        Jdbc.update("UPDATE usuarios SET ultimo_acceso = ? WHERE id = ?", LocalDateTime.now(), id);
    }
}
