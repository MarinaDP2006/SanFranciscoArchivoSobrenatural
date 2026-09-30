package com.archive.dao;

import com.archive.model.ContratoAnomalia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Proporciona operaciones JDBC para los contratos de anomalía.
 */
public class ContratoAnomaliaDAO {

	/**
	 * Inserta un contrato y establece su identificador generado.
	 *
	 * @param contrato contrato que se guarda
	 * @return identificador generado
	 * @throws SQLException si falla la inserción
	 */
	public int crear(ContratoAnomalia contrato) throws SQLException {
		String sql = "INSERT INTO CONTRATO_ANOMALIA (id_incidente, descripcion_detallada, zona_id, "
				+ "ciudad, pais, nivel_peligro, nivel_dificultad, recompensa_dinero, estado, "
				+ "id_usuario_solicitante, id_potencial_asignada) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(
						 sql, Statement.RETURN_GENERATED_KEYS)) {
			sentencia.setInt(1, contrato.getIdIncidente());
			sentencia.setString(2, contrato.getDescripcionDetallada());
			sentencia.setInt(3, contrato.getZonaId());
			sentencia.setString(4, contrato.getCiudad());
			sentencia.setString(5, contrato.getPais());
			sentencia.setInt(6, contrato.getNivelPeligro());
			sentencia.setInt(7, contrato.getNivelDificultad());
			sentencia.setDouble(8, contrato.getRecompensaDinero());
			sentencia.setString(9, contrato.getEstado() == null ? "SOLICITADO" : contrato.getEstado());
			sentencia.setInt(10, contrato.getIdUsuarioSolicitante());
			establecerEnteroOpcional(sentencia, 11, contrato.getIdPotencialAsignada());
			sentencia.executeUpdate();

			try (ResultSet claves = sentencia.getGeneratedKeys()) {
				if (!claves.next()) {
					throw new SQLException("No se generó el identificador del contrato.");
				}
				int idGenerado = claves.getInt(1);
				contrato.setIdContrato(idGenerado);
				return idGenerado;
			}
		}
	}

	/**
	 * Busca un contrato por identificador.
	 *
	 * @param idContrato identificador que se busca
	 * @return contrato encontrado o null si no existe
	 * @throws SQLException si falla la consulta
	 */
	public ContratoAnomalia obtenerPorId(int idContrato) throws SQLException {
		String sql = "SELECT * FROM CONTRATO_ANOMALIA WHERE id_contrato = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idContrato);
			try (ResultSet resultados = sentencia.executeQuery()) {
				return resultados.next() ? mapear(resultados) : null;
			}
		}
	}

	/**
	 * Recupera todos los contratos.
	 *
	 * @return contratos ordenados por identificador
	 * @throws SQLException si falla la consulta
	 */
	public List<ContratoAnomalia> listar() throws SQLException {
		return listarPorConsulta("SELECT * FROM CONTRATO_ANOMALIA ORDER BY id_contrato", null);
	}

	/**
	 * Recupera contratos solicitados por un reporter.
	 *
	 * @param idUsuarioSolicitante identificador del reporter
	 * @return contratos del reporter
	 * @throws SQLException si falla la consulta
	 */
	public List<ContratoAnomalia> listarPorSolicitante(int idUsuarioSolicitante)
			throws SQLException {
		return listarPorConsulta("SELECT * FROM CONTRATO_ANOMALIA "
				+ "WHERE id_usuario_solicitante = ? ORDER BY id_contrato DESC", idUsuarioSolicitante);
	}

	/**
	 * Asigna un potencial a un contrato que aún está solicitado.
	 *
	 * @param idContrato contrato que se asigna
	 * @param idPotencial potencial responsable
	 * @return true si se asignó el potencial
	 * @throws SQLException si falla la actualización
	 */
	public boolean asignarPotencial(int idContrato, int idPotencial) throws SQLException {
		String sql = "UPDATE CONTRATO_ANOMALIA SET id_potencial_asignada = ?, estado = 'ASIGNADO' "
				+ "WHERE id_contrato = ? AND estado = 'SOLICITADO' AND id_potencial_asignada IS NULL";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idPotencial);
			sentencia.setInt(2, idContrato);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Completa un contrato asignado, crea el ingreso y actualiza el saldo atómicamente.
	 *
	 * @param idContrato identificador del contrato
	 * @return true si se completó el contrato
	 * @throws SQLException si el contrato no puede completarse o falla una operación
	 */
	public boolean completarContrato(int idContrato) throws SQLException {
		String seleccionar = "SELECT id_potencial_asignada, recompensa_dinero, estado "
				+ "FROM CONTRATO_ANOMALIA WHERE id_contrato = ? FOR UPDATE";
		String completar = "UPDATE CONTRATO_ANOMALIA SET estado = 'COMPLETADO' "
				+ "WHERE id_contrato = ? AND estado = 'ASIGNADO'";
		String ingreso = "INSERT INTO TRANSACCION_MONEDERO "
				+ "(id_potencial, id_contrato, tipo, cantidad, descripcion) "
				+ "VALUES (?, ?, 'INGRESO_CONTRATO', ?, ?)";
		String actualizarSaldo = "UPDATE POTENCIAL SET saldo_calculado = "
				+ "COALESCE(saldo_calculado, 0) + ? WHERE id_potencial = ?";

		try (Connection conexion = Conexion.get()) {
			conexion.setAutoCommit(false);
			try {
				int idPotencial;
				double recompensa;
				try (PreparedStatement sentencia = conexion.prepareStatement(seleccionar)) {
					sentencia.setInt(1, idContrato);
					try (ResultSet resultados = sentencia.executeQuery()) {
						if (!resultados.next()) {
							throw new SQLException("No existe el contrato indicado.");
						}
						if (!"ASIGNADO".equals(resultados.getString("estado"))) {
							throw new SQLException("Solo se pueden completar contratos asignados.");
						}
						idPotencial = resultados.getInt("id_potencial_asignada");
						if (resultados.wasNull()) {
							throw new SQLException("El contrato no tiene un potencial asignado.");
						}
						recompensa = resultados.getDouble("recompensa_dinero");
					}
				}

				try (PreparedStatement sentencia = conexion.prepareStatement(completar)) {
					sentencia.setInt(1, idContrato);
					if (sentencia.executeUpdate() != 1) {
						throw new SQLException("No se pudo completar el contrato.");
					}
				}
				try (PreparedStatement sentencia = conexion.prepareStatement(ingreso)) {
					sentencia.setInt(1, idPotencial);
					sentencia.setInt(2, idContrato);
					sentencia.setDouble(3, recompensa);
					sentencia.setString(4, "Recompensa por completar el contrato " + idContrato);
					sentencia.executeUpdate();
				}
				try (PreparedStatement sentencia = conexion.prepareStatement(actualizarSaldo)) {
					sentencia.setDouble(1, recompensa);
					sentencia.setInt(2, idPotencial);
					if (sentencia.executeUpdate() != 1) {
						throw new SQLException("No se pudo actualizar el saldo del potencial.");
					}
				}
				conexion.commit();
				return true;
			} catch (SQLException | RuntimeException error) {
				try {
					conexion.rollback();
				} catch (SQLException errorRollback) {
					error.addSuppressed(errorRollback);
				}
				throw error;
			}
		}
	}

	/**
	 * Elimina un contrato por identificador.
	 *
	 * @param idContrato identificador que se elimina
	 * @return true si se eliminó una fila
	 * @throws SQLException si falla la eliminación o existen transacciones asociadas
	 */
	public boolean eliminar(int idContrato) throws SQLException {
		String sql = "DELETE FROM CONTRATO_ANOMALIA WHERE id_contrato = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idContrato);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Ejecuta una consulta de contratos con filtro opcional.
	 *
	 * @param sql consulta definida internamente por el DAO
	 * @param idSolicitante reporter que se filtra, o null para listar todos
	 * @return contratos mapeados desde las filas
	 * @throws SQLException si falla la consulta
	 */
	private List<ContratoAnomalia> listarPorConsulta(String sql, Integer idSolicitante)
			throws SQLException {
		List<ContratoAnomalia> contratos = new ArrayList<>();
		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			if (idSolicitante != null) {
				sentencia.setInt(1, idSolicitante);
			}
			try (ResultSet resultados = sentencia.executeQuery()) {
				while (resultados.next()) {
					contratos.add(mapear(resultados));
				}
			}
		}
		return contratos;
	}

	/**
	 * Asigna un entero nullable al parámetro JDBC.
	 *
	 * @param sentencia sentencia que se está preparando
	 * @param indice posición del parámetro
	 * @param valor identificador nullable
	 * @throws SQLException si falla la asignación
	 */
	private void establecerEnteroOpcional(
			PreparedStatement sentencia, int indice, Integer valor) throws SQLException {
		if (valor == null) {
			sentencia.setNull(indice, Types.INTEGER);
		} else {
			sentencia.setInt(indice, valor);
		}
	}

	/**
	 * Convierte la fila actual del resultado en un contrato.
	 *
	 * @param resultados resultado situado en una fila válida
	 * @return contrato construido desde la fila
	 * @throws SQLException si falla la lectura
	 */
	private ContratoAnomalia mapear(ResultSet resultados) throws SQLException {
		ContratoAnomalia contrato = new ContratoAnomalia();
		contrato.setIdContrato(resultados.getInt("id_contrato"));
		contrato.setIdIncidente(resultados.getInt("id_incidente"));
		contrato.setDescripcionDetallada(resultados.getString("descripcion_detallada"));
		contrato.setZonaId(resultados.getInt("zona_id"));
		contrato.setCiudad(resultados.getString("ciudad"));
		contrato.setPais(resultados.getString("pais"));
		contrato.setNivelPeligro(resultados.getInt("nivel_peligro"));
		contrato.setNivelDificultad(resultados.getInt("nivel_dificultad"));
		contrato.setRecompensaDinero(resultados.getDouble("recompensa_dinero"));
		contrato.setEstado(resultados.getString("estado"));
		contrato.setIdUsuarioSolicitante(resultados.getInt("id_usuario_solicitante"));
		int idPotencial = resultados.getInt("id_potencial_asignada");
		contrato.setIdPotencialAsignada(resultados.wasNull() ? null : idPotencial);
		return contrato;
	}
}
