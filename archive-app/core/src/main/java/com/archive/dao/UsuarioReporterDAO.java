package com.archive.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.archive.model.UsuarioReporter;

/**
 * Proporciona operaciones JDBC para cuentas reporter.
 */
public class UsuarioReporterDAO {

  /**
   * Busca un reporter por su nombre de usuario.
   *
   * @param username nombre de usuario que se busca
   * @return reporter encontrado o null si no existe
   * @throws SQLException si falla la consulta
   */
  public UsuarioReporter obtenerPorUsername(String username) throws SQLException {
    String sql = "SELECT id_usuario, username, password_hash, email "
        + "FROM USUARIO_REPORTER WHERE username = ?";

    try (Connection conexion = Conexion.get();
         PreparedStatement sentencia = conexion.prepareStatement(sql)) {
      sentencia.setString(1, username);
      try (ResultSet resultados = sentencia.executeQuery()) {
        return resultados.next() ? mapear(resultados) : null;
      }
    }
  }

  /**
   * Busca un reporter por identificador.
   *
   * @param idUsuario identificador del reporter
   * @return reporter encontrado o null si no existe
   * @throws SQLException si falla la consulta
   */
  public UsuarioReporter obtenerPorId(int idUsuario) throws SQLException {
    String sql = "SELECT id_usuario, username, password_hash, email "
        + "FROM USUARIO_REPORTER WHERE id_usuario = ?";

    try (Connection conexion = Conexion.get();
         PreparedStatement sentencia = conexion.prepareStatement(sql)) {
      sentencia.setInt(1, idUsuario);
      try (ResultSet resultados = sentencia.executeQuery()) {
        return resultados.next() ? mapear(resultados) : null;
      }
    }
  }

  /**
   * Registra un reporter y actualiza su identificador generado.
   *
   * @param reporter reporter que se guarda
   * @return identificador generado
   * @throws SQLException si falla la inserción
   */
  public int crear(UsuarioReporter reporter) throws SQLException {
    String sql = "INSERT INTO USUARIO_REPORTER (username, password_hash, email) VALUES (?, ?, ?)";

    try (Connection conexion = Conexion.get();
         PreparedStatement sentencia = conexion.prepareStatement(
             sql, Statement.RETURN_GENERATED_KEYS)) {
      sentencia.setString(1, reporter.getUsername());
      sentencia.setString(2, reporter.getPasswordHash());
      sentencia.setString(3, reporter.getEmail());
      sentencia.executeUpdate();
      try (ResultSet claves = sentencia.getGeneratedKeys()) {
        if (!claves.next()) {
          throw new SQLException("No se generó el identificador del reporter.");
        }
        int idGenerado = claves.getInt(1);
        reporter.setIdUsuario(idGenerado);
        return idGenerado;
      }
    }
  }

  /**
   * Convierte la fila actual del resultado en un reporter.
   *
   * @param resultados resultado situado en una fila válida
   * @return reporter construido desde la fila
   * @throws SQLException si falla la lectura
   */
  private UsuarioReporter mapear(ResultSet resultados) throws SQLException {
    UsuarioReporter reporter = new UsuarioReporter();
    reporter.setIdUsuario(resultados.getInt("id_usuario"));
    reporter.setUsername(resultados.getString("username"));
    reporter.setPasswordHash(resultados.getString("password_hash"));
    reporter.setEmail(resultados.getString("email"));
    return reporter;
  }
}