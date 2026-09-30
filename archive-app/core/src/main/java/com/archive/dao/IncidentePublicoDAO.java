package com.archive.dao;

import com.archive.model.IncidentePublico;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Proporciona operaciones JDBC para incidentes públicos.
 */
public class IncidentePublicoDAO {

	/**
	 * Inserta un incidente y establece el identificador generado.
	 *
	 * @param incidente incidente que se guarda
	 * @return identificador generado
	 * @throws SQLException si falla la inserción
	 */
	public int crear(IncidentePublico incidente) throws SQLException {
		String sql = "INSERT INTO INCIDENTE_PUBLICO (tipo, descripcion_corta, ciudad, pais, lat, "
				+ "lon, fecha_reporte, estado_verificacion, id_usuario_reporter, es_visible_sin_login) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(
						 sql, Statement.RETURN_GENERATED_KEYS)) {
			sentencia.setString(1, incidente.getTipo());
			sentencia.setString(2, incidente.getDescripcionCorta());
			sentencia.setString(3, incidente.getCiudad());
			sentencia.setString(4, incidente.getPais());
			sentencia.setDouble(5, incidente.getLat());
			sentencia.setDouble(6, incidente.getLon());
			establecerFecha(sentencia, 7, incidente.getFechaReporte());
			sentencia.setString(8, incidente.getEstadoVerificacion() == null
					? "NO_VERIFICADO" : incidente.getEstadoVerificacion());
			if (incidente.getIdUsuarioReporter() == null) {
				sentencia.setNull(9, Types.INTEGER);
			} else {
				sentencia.setInt(9, incidente.getIdUsuarioReporter());
			}
			sentencia.setBoolean(10, incidente.isEsVisibleSinLogin());
			sentencia.executeUpdate();

			try (ResultSet claves = sentencia.getGeneratedKeys()) {
				if (!claves.next()) {
					throw new SQLException("No se generó el identificador del incidente.");
				}
				int idGenerado = claves.getInt(1);
				incidente.setIdIncidente(idGenerado);
				return idGenerado;
			}
		}
	}

	/**
	 * Busca un incidente por identificador.
	 *
	 * @param idIncidente identificador que se busca
	 * @return incidente encontrado o null si no existe
	 * @throws SQLException si falla la consulta
	 */
	public IncidentePublico obtenerPorId(int idIncidente) throws SQLException {
		String sql = "SELECT * FROM INCIDENTE_PUBLICO WHERE id_incidente = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idIncidente);
			try (ResultSet resultados = sentencia.executeQuery()) {
				return resultados.next() ? mapear(resultados) : null;
			}
		}
	}

	/**
	 * Recupera los incidentes que puede consultar un ciudadano sin autenticarse.
	 *
	 * @return incidentes visibles ordenados del más reciente al más antiguo
	 * @throws SQLException si falla la consulta
	 */
	public List<IncidentePublico> listarVisiblesSinLogin() throws SQLException {
		String sql = "SELECT * FROM INCIDENTE_PUBLICO WHERE es_visible_sin_login = TRUE "
				+ "ORDER BY fecha_reporte DESC, id_incidente DESC";
		return listarConConsulta(sql, null);
	}

	/**
	 * Recupera todos los incidentes.
	 *
	 * @return incidentes ordenados del más reciente al más antiguo
	 * @throws SQLException si falla la consulta
	 */
	public List<IncidentePublico> listar() throws SQLException {
		String sql = "SELECT * FROM INCIDENTE_PUBLICO "
				+ "ORDER BY fecha_reporte DESC, id_incidente DESC";
		return listarConConsulta(sql, null);
	}

	/**
	 * Actualiza el estado de verificación.
	 *
	 * @param idIncidente identificador del incidente
	 * @param estado estado de verificación permitido por el esquema
	 * @return true si se actualizó una fila
	 * @throws SQLException si falla la actualización
	 */
	public boolean actualizarEstadoVerificacion(int idIncidente, String estado)
			throws SQLException {
		String sql = "UPDATE INCIDENTE_PUBLICO SET estado_verificacion = ? WHERE id_incidente = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setString(1, estado);
			sentencia.setInt(2, idIncidente);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Actualiza los datos de un incidente.
	 *
	 * @param incidente incidente con los valores nuevos
	 * @return true si se actualizó una fila
	 * @throws SQLException si falla la actualización
	 */
	public boolean actualizar(IncidentePublico incidente) throws SQLException {
		String sql = "UPDATE INCIDENTE_PUBLICO SET tipo = ?, descripcion_corta = ?, ciudad = ?, "
				+ "pais = ?, lat = ?, lon = ?, estado_verificacion = ?, id_usuario_reporter = ?, "
				+ "es_visible_sin_login = ? WHERE id_incidente = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setString(1, incidente.getTipo());
			sentencia.setString(2, incidente.getDescripcionCorta());
			sentencia.setString(3, incidente.getCiudad());
			sentencia.setString(4, incidente.getPais());
			sentencia.setDouble(5, incidente.getLat());
			sentencia.setDouble(6, incidente.getLon());
			sentencia.setString(7, incidente.getEstadoVerificacion());
			if (incidente.getIdUsuarioReporter() == null) {
				sentencia.setNull(8, Types.INTEGER);
			} else {
				sentencia.setInt(8, incidente.getIdUsuarioReporter());
			}
			sentencia.setBoolean(9, incidente.isEsVisibleSinLogin());
			sentencia.setInt(10, incidente.getIdIncidente());
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Elimina un incidente por identificador.
	 *
	 * @param idIncidente identificador que se elimina
	 * @return true si se eliminó una fila
	 * @throws SQLException si falla la eliminación o existen contratos asociados
	 */
	public boolean eliminar(int idIncidente) throws SQLException {
		String sql = "DELETE FROM INCIDENTE_PUBLICO WHERE id_incidente = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idIncidente);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Ejecuta una consulta de incidentes y mapea las filas devueltas.
	 *
	 * @param sql consulta SQL interna del DAO
	 * @param idFiltro filtro opcional de entero; es null cuando la consulta no lleva parámetros
	 * @return filas convertidas a incidentes
	 * @throws SQLException si falla la consulta o el mapeo
	 */
	private List<IncidentePublico> listarConConsulta(String sql, Integer idFiltro)
			throws SQLException {
		List<IncidentePublico> incidentes = new ArrayList<>();
		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			if (idFiltro != null) {
				sentencia.setInt(1, idFiltro);
			}
			try (ResultSet resultados = sentencia.executeQuery()) {
				while (resultados.next()) {
					incidentes.add(mapear(resultados));
				}
			}
		}
		return incidentes;
	}

	/**
	 * Asigna una fecha al parámetro o SQL NULL si no se proporcionó.
	 *
	 * @param sentencia sentencia que se está preparando
	 * @param indice posición del parámetro
	 * @param fecha fecha local que se guarda
	 * @throws SQLException si falla la asignación
	 */
	private void establecerFecha(PreparedStatement sentencia, int indice, LocalDateTime fecha)
			throws SQLException {
		if (fecha == null) {
			sentencia.setNull(indice, Types.TIMESTAMP);
		} else {
			sentencia.setTimestamp(indice, Timestamp.valueOf(fecha));
		}
	}

	/**
	 * Convierte la fila actual del resultado en un incidente público.
	 *
	 * @param resultados resultado situado en una fila válida
	 * @return incidente construido desde la fila
	 * @throws SQLException si falla la lectura
	 */
	private IncidentePublico mapear(ResultSet resultados) throws SQLException {
		IncidentePublico incidente = new IncidentePublico();
		incidente.setIdIncidente(resultados.getInt("id_incidente"));
		incidente.setTipo(resultados.getString("tipo"));
		incidente.setDescripcionCorta(resultados.getString("descripcion_corta"));
		incidente.setCiudad(resultados.getString("ciudad"));
		incidente.setPais(resultados.getString("pais"));
		incidente.setLat(resultados.getDouble("lat"));
		incidente.setLon(resultados.getDouble("lon"));
		Timestamp fecha = resultados.getTimestamp("fecha_reporte");
		incidente.setFechaReporte(fecha == null ? null : fecha.toLocalDateTime());
		incidente.setEstadoVerificacion(resultados.getString("estado_verificacion"));
		int idReporter = resultados.getInt("id_usuario_reporter");
		incidente.setIdUsuarioReporter(resultados.wasNull() ? null : idReporter);
		incidente.setEsVisibleSinLogin(resultados.getBoolean("es_visible_sin_login"));
		return incidente;
	}
}
