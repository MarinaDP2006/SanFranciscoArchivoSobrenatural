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

public class UsuarioDao {

    private static final String SELECT = """
            SELECT u.*, p.id AS potencial_id
            FROM usuarios u LEFT JOIN potenciales p ON p.usuario_id = u.id
            """;

    static Usuario map(ResultSet rs) throws SQLException {
        return new Usuario(rs.getInt("id"), rs.getString("username"), rs.getString("email"),
                Rol.valueOf(rs.getString("rol")), rs.getString("nombre_completo"),
                rs.getBoolean("activo"), Jdbc.dateTime(rs, "ultimo_acceso"),
                Jdbc.intOrNull(rs, "potencial_id"));
    }

    public List<Usuario> listar() {
        return Jdbc.query(SELECT + " ORDER BY u.rol, u.username", UsuarioDao::map);
    }

    public Optional<Usuario> porId(int id) {
        return Jdbc.one(SELECT + " WHERE u.id = ?", UsuarioDao::map, id);
    }

    public Optional<Usuario> porUsername(String username) {
        return Jdbc.one(SELECT + " WHERE u.username = ?", UsuarioDao::map, username);
    }

    public Optional<String> hashDe(String username) {
        return Jdbc.one("SELECT password_hash FROM usuarios WHERE username = ?", rs -> rs.getString(1), username);
    }

    public Optional<String> hashDe(int id) {
        return Jdbc.one("SELECT password_hash FROM usuarios WHERE id = ?", rs -> rs.getString(1), id);
    }

    public int crear(Connection c, String username, String email, String hash, Rol rol, String nombre) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO usuarios (username, email, password_hash, rol, nombre_completo) VALUES (?,?,?,?,?)",
                username, email, hash, rol, nombre);
    }

    public void actualizar(int id, String email, String nombre, boolean activo) {
        Jdbc.update("UPDATE usuarios SET email = ?, nombre_completo = ?, activo = ? WHERE id = ?", email, nombre, activo, id);
    }

    public void cambiarHash(int id, String hash) {
        Jdbc.update("UPDATE usuarios SET password_hash = ? WHERE id = ?", hash, id);
    }

    public void registrarAcceso(int id) {
        Jdbc.update("UPDATE usuarios SET ultimo_acceso = ? WHERE id = ?", LocalDateTime.now(), id);
    }
}
