package com.archive.dao;

import com.archive.model.Admin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Proporciona operaciones JDBC para los administradores.
 */
public class AdminDAO {

	/**
	 * Crea un administrador y actualiza su identificador generado.
	 *
	 * @param admin administrador que se guarda
	 * @return identificador asignado por MySQL
	 * @throws SQLException si falla la inserción
	 */
	public int crear(Admin admin) throws SQLException {
		String sql = "INSERT INTO ADMIN (nombre, username, password_hash, edad_real, "
				+ "edad_sobrenatural, ciudad_residencia) VALUES (?, ?, ?, ?, ?, ?)";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(
						 sql, Statement.RETURN_GENERATED_KEYS)) {
			sentencia.setString(1, admin.getNombre());
			sentencia.setString(2, admin.getUsername());
			sentencia.setString(3, admin.getPasswordHash());
			sentencia.setInt(4, admin.getEdadReal());
			sentencia.setInt(5, admin.getEdadSobrenatural());
			sentencia.setString(6, admin.getCiudadResidencia());
			sentencia.executeUpdate();

			try (ResultSet claves = sentencia.getGeneratedKeys()) {
				if (!claves.next()) {
					throw new SQLException("No se generó el identificador del administrador.");
				}
				int idGenerado = claves.getInt(1);
				admin.setIdAdmin(idGenerado);
				return idGenerado;
			}
		}
	}

	/**
	 * Busca un administrador por su identificador.
	 *
	 * @param idAdmin identificador que se busca
	 * @return administrador encontrado o null si no existe
	 * @throws SQLException si falla la consulta
	 */
	public Admin obtenerPorId(int idAdmin) throws SQLException {
		String sql = "SELECT id_admin, nombre, username, password_hash, edad_real, "
				+ "edad_sobrenatural, ciudad_residencia FROM ADMIN WHERE id_admin = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idAdmin);
			try (ResultSet resultados = sentencia.executeQuery()) {
				return resultados.next() ? mapear(resultados) : null;
			}
		}
	}

	/**
	 * Busca un administrador por su nombre de usuario.
	 *
	 * @param username nombre de usuario que se busca
	 * @return administrador encontrado o null si no existe
	 * @throws SQLException si falla la consulta
	 */
	public Admin obtenerPorUsername(String username) throws SQLException {
		String sql = "SELECT id_admin, nombre, username, password_hash, edad_real, "
				+ "edad_sobrenatural, ciudad_residencia FROM ADMIN WHERE username = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setString(1, username);
			try (ResultSet resultados = sentencia.executeQuery()) {
				return resultados.next() ? mapear(resultados) : null;
			}
		}
	}

	/**
	 * Recupera todos los administradores ordenados por identificador.
	 *
	 * @return lista de administradores
	 * @throws SQLException si falla la consulta
	 */
	public List<Admin> listar() throws SQLException {
		String sql = "SELECT id_admin, nombre, username, password_hash, edad_real, "
				+ "edad_sobrenatural, ciudad_residencia FROM ADMIN ORDER BY id_admin";
		List<Admin> administradores = new ArrayList<>();

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql);
				 ResultSet resultados = sentencia.executeQuery()) {
			while (resultados.next()) {
				administradores.add(mapear(resultados));
			}
		}
		return administradores;
	}

	/**
	 * Actualiza los datos de un administrador.
	 *
	 * @param admin administrador con los datos nuevos
	 * @return true si se actualizó una fila
	 * @throws SQLException si falla la actualización
	 */
	public boolean actualizar(Admin admin) throws SQLException {
		String sql = "UPDATE ADMIN SET nombre = ?, username = ?, password_hash = ?, "
				+ "edad_real = ?, edad_sobrenatural = ?, ciudad_residencia = ? "
				+ "WHERE id_admin = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setString(1, admin.getNombre());
			sentencia.setString(2, admin.getUsername());
			sentencia.setString(3, admin.getPasswordHash());
			sentencia.setInt(4, admin.getEdadReal());
			sentencia.setInt(5, admin.getEdadSobrenatural());
			sentencia.setString(6, admin.getCiudadResidencia());
			sentencia.setInt(7, admin.getIdAdmin());
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Elimina un administrador por identificador.
	 *
	 * @param idAdmin identificador del administrador que se elimina
	 * @return true si se eliminó una fila
	 * @throws SQLException si falla la eliminación o existen referencias asociadas
	 */
	public boolean eliminar(int idAdmin) throws SQLException {
		String sql = "DELETE FROM ADMIN WHERE id_admin = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idAdmin);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Convierte la fila actual del resultado en un administrador.
	 *
	 * @param resultados resultado situado en una fila válida
	 * @return administrador construido desde la fila
	 * @throws SQLException si falla la lectura de columnas
	 */
	private Admin mapear(ResultSet resultados) throws SQLException {
		Admin admin = new Admin();
		admin.setIdAdmin(resultados.getInt("id_admin"));
		admin.setNombre(resultados.getString("nombre"));
		admin.setUsername(resultados.getString("username"));
		admin.setPasswordHash(resultados.getString("password_hash"));
		admin.setEdadReal(resultados.getInt("edad_real"));
		admin.setEdadSobrenatural(resultados.getInt("edad_sobrenatural"));
		admin.setCiudadResidencia(resultados.getString("ciudad_residencia"));
		return admin;
	}
}
