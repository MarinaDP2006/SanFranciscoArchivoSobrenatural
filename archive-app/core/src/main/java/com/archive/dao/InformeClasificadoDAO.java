package com.archive.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.archive.model.InformeClasificado;

/**
 * Proporciona acceso a informes clasificados exclusivamente para el rol ADMIN.
 */
public class InformeClasificadoDAO {

	/**
	 * Inserta un informe clasificado.
	 *
	 * @param informe informe que se guarda
	 * @param rol rol autenticado de quien solicita la operación
	 * @return identificador generado
	 * @throws SQLException si el rol no es ADMIN o falla la inserción
	 */
	public int crear(InformeClasificado informe, String rol) throws SQLException {
		exigirAdmin(rol);
		String sql = "INSERT INTO INFORME_CLASIFICADO "
				+ "(id_contrato, id_admin_autor, titulo, contenido_sin_censura, nivel_acceso) "
				+ "VALUES (?, ?, ?, ?, ?)";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(
						 sql, Statement.RETURN_GENERATED_KEYS)) {
			sentencia.setInt(1, informe.getIdContrato());
			sentencia.setInt(2, informe.getIdAdminAutor());
			sentencia.setString(3, informe.getTitulo());
			sentencia.setString(4, informe.getContenidoSinCensura());
			sentencia.setString(5, informe.getNivelAcceso() == null
					? "ADMIN_ONLY" : informe.getNivelAcceso());
			sentencia.executeUpdate();
			try (ResultSet claves = sentencia.getGeneratedKeys()) {
				if (!claves.next()) {
					throw new SQLException("No se generó el identificador del informe.");
				}
				int idGenerado = claves.getInt(1);
				informe.setIdInforme(idGenerado);
				return idGenerado;
			}
		}
	}

	/**
	 * Recupera un informe por identificador.
	 *
	 * @param idInforme identificador del informe
	 * @param rol rol autenticado de quien solicita la operación
	 * @return informe encontrado o null si no existe
	 * @throws SQLException si el rol no es ADMIN o falla la consulta
	 */
	public InformeClasificado obtenerPorId(int idInforme, String rol) throws SQLException {
		exigirAdmin(rol);
		String sql = "SELECT * FROM INFORME_CLASIFICADO WHERE id_informe = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idInforme);
			try (ResultSet resultados = sentencia.executeQuery()) {
				return resultados.next() ? mapear(resultados) : null;
			}
		}
	}

	/**
	 * Recupera todos los informes clasificados.
	 *
	 * @param rol rol autenticado de quien solicita la operación
	 * @return lista de informes
	 * @throws SQLException si el rol no es ADMIN o falla la consulta
	 */
	public List<InformeClasificado> listar(String rol) throws SQLException {
		exigirAdmin(rol);
		String sql = "SELECT * FROM INFORME_CLASIFICADO ORDER BY fecha DESC, id_informe DESC";
		List<InformeClasificado> informes = new ArrayList<>();

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql);
				 ResultSet resultados = sentencia.executeQuery()) {
			while (resultados.next()) {
				informes.add(mapear(resultados));
			}
		}
		return informes;
	}

	/**
	 * Actualiza el contenido de un informe.
	 *
	 * @param informe informe con los datos nuevos
	 * @param rol rol autenticado de quien solicita la operación
	 * @return true si se actualizó una fila
	 * @throws SQLException si el rol no es ADMIN o falla la actualización
	 */
	public boolean actualizar(InformeClasificado informe, String rol) throws SQLException {
		exigirAdmin(rol);
		String sql = "UPDATE INFORME_CLASIFICADO SET id_contrato = ?, id_admin_autor = ?, "
				+ "titulo = ?, contenido_sin_censura = ?, nivel_acceso = ? WHERE id_informe = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, informe.getIdContrato());
			sentencia.setInt(2, informe.getIdAdminAutor());
			sentencia.setString(3, informe.getTitulo());
			sentencia.setString(4, informe.getContenidoSinCensura());
			sentencia.setString(5, informe.getNivelAcceso());
			sentencia.setInt(6, informe.getIdInforme());
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Elimina un informe por identificador.
	 *
	 * @param idInforme identificador del informe
	 * @param rol rol autenticado de quien solicita la operación
	 * @return true si se eliminó una fila
	 * @throws SQLException si el rol no es ADMIN o falla la eliminación
	 */
	public boolean eliminar(int idInforme, String rol) throws SQLException {
		exigirAdmin(rol);
		String sql = "DELETE FROM INFORME_CLASIFICADO WHERE id_informe = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idInforme);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Rechaza el acceso si el rol no es ADMIN.
	 *
	 * @param rol rol que se valida
	 * @throws SQLException con estado de autorización denegada
	 */
	private void exigirAdmin(String rol) throws SQLException {
		if (!"ADMIN".equalsIgnoreCase(rol)) {
			throw new SQLException("ACCESS DENIED", "28000");
		}
	}

	/**
	 * Convierte la fila actual del resultado en un informe clasificado.
	 *
	 * @param resultados resultado situado en una fila válida
	 * @return informe construido desde la fila
	 * @throws SQLException si falla la lectura
	 */
	private InformeClasificado mapear(ResultSet resultados) throws SQLException {
		InformeClasificado informe = new InformeClasificado();
		informe.setIdInforme(resultados.getInt("id_informe"));
		informe.setIdContrato(resultados.getInt("id_contrato"));
		informe.setIdAdminAutor(resultados.getInt("id_admin_autor"));
		informe.setTitulo(resultados.getString("titulo"));
		informe.setContenidoSinCensura(resultados.getString("contenido_sin_censura"));
		informe.setNivelAcceso(resultados.getString("nivel_acceso"));
		Timestamp fecha = resultados.getTimestamp("fecha");
		informe.setFecha(fecha == null ? null : fecha.toLocalDateTime());
		return informe;
	}
}
