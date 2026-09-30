package com.archive.dao;

import com.archive.model.GrupoTactico;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Proporciona operaciones JDBC para grupos tácticos con un máximo de seis potenciales.
 */
public class GrupoTacticoDAO {

	/**
	 * Crea un grupo y asigna al modelo el identificador generado.
	 *
	 * @param grupo grupo táctico que se guarda
	 * @return identificador generado
	 * @throws SQLException si falla la inserción
	 */
	public int crear(GrupoTactico grupo) throws SQLException {
		String sql = "INSERT INTO GRUPO_TACTICO (pais, id_admin_creador) VALUES (?, ?)";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(
						 sql, Statement.RETURN_GENERATED_KEYS)) {
			sentencia.setString(1, grupo.getPais());
			establecerAdmin(sentencia, 2, grupo.getIdAdminCreador());
			sentencia.executeUpdate();

			try (ResultSet claves = sentencia.getGeneratedKeys()) {
				if (!claves.next()) {
					throw new SQLException("No se generó el identificador del grupo.");
				}
				int idGenerado = claves.getInt(1);
				grupo.setIdGrupo(idGenerado);
				return idGenerado;
			}
		}
	}

	/**
	 * Busca un grupo y calcula sus integrantes actuales.
	 *
	 * @param idGrupo identificador del grupo
	 * @return grupo encontrado o null si no existe
	 * @throws SQLException si falla la consulta
	 */
	public GrupoTactico obtenerPorId(int idGrupo) throws SQLException {
		String sql = "SELECT g.id_grupo, g.pais, g.id_admin_creador, "
				+ "COUNT(p.id_potencial) AS integrantes_actuales FROM GRUPO_TACTICO g "
				+ "LEFT JOIN POTENCIAL p ON p.id_grupo = g.id_grupo "
				+ "WHERE g.id_grupo = ? GROUP BY g.id_grupo, g.pais, g.id_admin_creador";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idGrupo);
			try (ResultSet resultados = sentencia.executeQuery()) {
				return resultados.next() ? mapear(resultados) : null;
			}
		}
	}

	/**
	 * Recupera todos los grupos junto a su número de integrantes.
	 *
	 * @return lista de grupos tácticos
	 * @throws SQLException si falla la consulta
	 */
	public List<GrupoTactico> listar() throws SQLException {
		String sql = "SELECT g.id_grupo, g.pais, g.id_admin_creador, "
				+ "COUNT(p.id_potencial) AS integrantes_actuales FROM GRUPO_TACTICO g "
				+ "LEFT JOIN POTENCIAL p ON p.id_grupo = g.id_grupo "
				+ "GROUP BY g.id_grupo, g.pais, g.id_admin_creador ORDER BY g.id_grupo";
		List<GrupoTactico> grupos = new ArrayList<>();

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql);
				 ResultSet resultados = sentencia.executeQuery()) {
			while (resultados.next()) {
				grupos.add(mapear(resultados));
			}
		}
		return grupos;
	}

	/**
	 * Asigna un potencial si el grupo tiene menos de seis integrantes.
	 * La fila del grupo se bloquea para serializar asignaciones concurrentes.
	 *
	 * @param idGrupo grupo de destino
	 * @param idPotencial potencial que se asigna
	 * @return true si el potencial quedó asignado
	 * @throws SQLException si no existen las filas, el grupo está lleno o falla la transacción
	 */
	public boolean asignarPotencial(int idGrupo, int idPotencial) throws SQLException {
		String bloquearGrupo = "SELECT id_grupo FROM GRUPO_TACTICO WHERE id_grupo = ? FOR UPDATE";
		String bloquearPotencial = "SELECT id_grupo FROM POTENCIAL "
				+ "WHERE id_potencial = ? FOR UPDATE";
		String contar = "SELECT COUNT(*) FROM POTENCIAL WHERE id_grupo = ?";
		String actualizar = "UPDATE POTENCIAL SET id_grupo = ? WHERE id_potencial = ?";

		try (Connection conexion = Conexion.get()) {
			conexion.setAutoCommit(false);
			try {
				try (PreparedStatement sentencia = conexion.prepareStatement(bloquearGrupo)) {
					sentencia.setInt(1, idGrupo);
					try (ResultSet resultados = sentencia.executeQuery()) {
						if (!resultados.next()) {
							throw new SQLException("No existe el grupo indicado.");
						}
					}
				}

				Integer grupoActual;
				try (PreparedStatement sentencia = conexion.prepareStatement(bloquearPotencial)) {
					sentencia.setInt(1, idPotencial);
					try (ResultSet resultados = sentencia.executeQuery()) {
						if (!resultados.next()) {
							throw new SQLException("No existe el potencial indicado.");
						}
						int valor = resultados.getInt("id_grupo");
						grupoActual = resultados.wasNull() ? null : valor;
					}
				}

				if (grupoActual != null && grupoActual == idGrupo) {
					conexion.commit();
					return true;
				}

				try (PreparedStatement sentencia = conexion.prepareStatement(contar)) {
					sentencia.setInt(1, idGrupo);
					try (ResultSet resultados = sentencia.executeQuery()) {
						resultados.next();
						if (resultados.getInt(1) >= GrupoTactico.MAX_INTEGRANTES) {
							throw new SQLException("El grupo ya tiene seis potenciales.");
						}
					}
				}

				try (PreparedStatement sentencia = conexion.prepareStatement(actualizar)) {
					sentencia.setInt(1, idGrupo);
					sentencia.setInt(2, idPotencial);
					if (sentencia.executeUpdate() != 1) {
						throw new SQLException("No se pudo asignar el potencial.");
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
	 * Retira un potencial de un grupo.
	 *
	 * @param idGrupo grupo del que se retira
	 * @param idPotencial potencial que se retira
	 * @return true si se retiró una fila
	 * @throws SQLException si falla la actualización
	 */
	public boolean quitarPotencial(int idGrupo, int idPotencial) throws SQLException {
		String sql = "UPDATE POTENCIAL SET id_grupo = NULL "
				+ "WHERE id_grupo = ? AND id_potencial = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idGrupo);
			sentencia.setInt(2, idPotencial);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Actualiza país y administrador creador del grupo.
	 *
	 * @param grupo grupo con los valores nuevos
	 * @return true si se actualizó una fila
	 * @throws SQLException si falla la actualización
	 */
	public boolean actualizar(GrupoTactico grupo) throws SQLException {
		String sql = "UPDATE GRUPO_TACTICO SET pais = ?, id_admin_creador = ? WHERE id_grupo = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setString(1, grupo.getPais());
			establecerAdmin(sentencia, 2, grupo.getIdAdminCreador());
			sentencia.setInt(3, grupo.getIdGrupo());
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Elimina un grupo por identificador.
	 *
	 * @param idGrupo identificador del grupo que se elimina
	 * @return true si se eliminó una fila
	 * @throws SQLException si falla la eliminación o el grupo tiene potenciales
	 */
	public boolean eliminar(int idGrupo) throws SQLException {
		String sql = "DELETE FROM GRUPO_TACTICO WHERE id_grupo = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idGrupo);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Asigna el administrador nullable al parámetro JDBC.
	 *
	 * @param sentencia sentencia que se está preparando
	 * @param indice posición del parámetro
	 * @param idAdmin identificador; cero o negativo representa SQL NULL
	 * @throws SQLException si falla la asignación
	 */
	private void establecerAdmin(PreparedStatement sentencia, int indice, int idAdmin)
			throws SQLException {
		if (idAdmin > 0) {
			sentencia.setInt(indice, idAdmin);
		} else {
			sentencia.setNull(indice, Types.INTEGER);
		}
	}

	/**
	 * Convierte la fila actual del resultado en un grupo táctico.
	 *
	 * @param resultados resultado situado en una fila válida
	 * @return grupo construido desde la fila
	 * @throws SQLException si falla la lectura
	 */
	private GrupoTactico mapear(ResultSet resultados) throws SQLException {
		GrupoTactico grupo = new GrupoTactico();
		grupo.setIdGrupo(resultados.getInt("id_grupo"));
		grupo.setPais(resultados.getString("pais"));
		grupo.setIdAdminCreador(resultados.getInt("id_admin_creador"));
		grupo.setIntegrantesActuales(resultados.getInt("integrantes_actuales"));
		return grupo;
	}
}
