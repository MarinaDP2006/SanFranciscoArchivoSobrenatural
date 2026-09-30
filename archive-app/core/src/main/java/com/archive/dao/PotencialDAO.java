package com.archive.dao;

import com.archive.model.Potencial;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Proporciona operaciones JDBC para los potenciales.
 */
public class PotencialDAO {

	/**
	 * Inserta un potencial y establece el identificador generado.
	 *
	 * @param potencial potencial que se guarda
	 * @return identificador generado
	 * @throws SQLException si falla la inserción
	 */
	public int crear(Potencial potencial) throws SQLException {
		String sql = "INSERT INTO POTENCIAL (nombre, edad, ciudad_origen, pais_actual, fuerza, "
				+ "velocidad, estado, id_admin_asignado, id_grupo, saldo_calculado) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(
						 sql, Statement.RETURN_GENERATED_KEYS)) {
			sentencia.setString(1, potencial.getNombre());
			sentencia.setInt(2, potencial.getEdad());
			sentencia.setString(3, potencial.getCiudadOrigen());
			sentencia.setString(4, potencial.getPaisActual());
			sentencia.setInt(5, potencial.getFuerza());
			sentencia.setInt(6, potencial.getVelocidad());
			sentencia.setString(7, potencial.getEstado() == null ? "EN_BASE" : potencial.getEstado());
			establecerEnteroOpcional(sentencia, 8, potencial.getIdAdminAsignado());
			establecerEnteroOpcional(sentencia, 9, potencial.getIdGrupo());
			sentencia.setDouble(10, potencial.getSaldoCalculado());
			sentencia.executeUpdate();

			try (ResultSet claves = sentencia.getGeneratedKeys()) {
				if (!claves.next()) {
					throw new SQLException("No se generó el identificador del potencial.");
				}
				int idGenerado = claves.getInt(1);
				potencial.setIdPotencial(idGenerado);
				return idGenerado;
			}
		}
	}

	/**
	 * Busca un potencial por identificador.
	 *
	 * @param idPotencial identificador que se busca
	 * @return potencial encontrado o null si no existe
	 * @throws SQLException si falla la consulta
	 */
	public Potencial obtenerPorId(int idPotencial) throws SQLException {
		String sql = "SELECT * FROM POTENCIAL WHERE id_potencial = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idPotencial);
			try (ResultSet resultados = sentencia.executeQuery()) {
				return resultados.next() ? mapear(resultados) : null;
			}
		}
	}

	/**
	 * Recupera todos los potenciales.
	 *
	 * @return lista ordenada por identificador
	 * @throws SQLException si falla la consulta
	 */
	public List<Potencial> listar() throws SQLException {
		String sql = "SELECT * FROM POTENCIAL ORDER BY id_potencial";
		List<Potencial> potenciales = new ArrayList<>();

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql);
				 ResultSet resultados = sentencia.executeQuery()) {
			while (resultados.next()) {
				potenciales.add(mapear(resultados));
			}
		}
		return potenciales;
	}

	/**
	 * Recupera los potenciales que pertenecen a un grupo.
	 *
	 * @param idGrupo identificador del grupo táctico
	 * @return lista de potenciales del grupo
	 * @throws SQLException si falla la consulta
	 */
	public List<Potencial> listarPorGrupo(int idGrupo) throws SQLException {
		String sql = "SELECT * FROM POTENCIAL WHERE id_grupo = ? ORDER BY id_potencial";
		List<Potencial> potenciales = new ArrayList<>();

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idGrupo);
			try (ResultSet resultados = sentencia.executeQuery()) {
				while (resultados.next()) {
					potenciales.add(mapear(resultados));
				}
			}
		}
		return potenciales;
	}

	/**
	 * Actualiza todos los campos persistidos de un potencial.
	 *
	 * @param potencial potencial con sus nuevos valores
	 * @return true si se actualizó una fila
	 * @throws SQLException si falla la actualización
	 */
	public boolean actualizar(Potencial potencial) throws SQLException {
		String sql = "UPDATE POTENCIAL SET nombre = ?, edad = ?, ciudad_origen = ?, "
				+ "pais_actual = ?, fuerza = ?, velocidad = ?, estado = ?, "
				+ "id_admin_asignado = ?, id_grupo = ?, saldo_calculado = ? "
				+ "WHERE id_potencial = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setString(1, potencial.getNombre());
			sentencia.setInt(2, potencial.getEdad());
			sentencia.setString(3, potencial.getCiudadOrigen());
			sentencia.setString(4, potencial.getPaisActual());
			sentencia.setInt(5, potencial.getFuerza());
			sentencia.setInt(6, potencial.getVelocidad());
			sentencia.setString(7, potencial.getEstado());
			establecerEnteroOpcional(sentencia, 8, potencial.getIdAdminAsignado());
			establecerEnteroOpcional(sentencia, 9, potencial.getIdGrupo());
			sentencia.setDouble(10, potencial.getSaldoCalculado());
			sentencia.setInt(11, potencial.getIdPotencial());
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Elimina un potencial por identificador.
	 *
	 * @param idPotencial identificador que se elimina
	 * @return true si se eliminó una fila
	 * @throws SQLException si falla la eliminación o existen referencias
	 */
	public boolean eliminar(int idPotencial) throws SQLException {
		String sql = "DELETE FROM POTENCIAL WHERE id_potencial = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idPotencial);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Asigna un identificador nullable al parámetro SQL.
	 *
	 * @param sentencia sentencia que se está preparando
	 * @param indice posición del parámetro
	 * @param valor identificador; cero o negativo se convierte en SQL NULL
	 * @throws SQLException si falla la asignación
	 */
	private void establecerEnteroOpcional(PreparedStatement sentencia, int indice, int valor)
			throws SQLException {
		if (valor > 0) {
			sentencia.setInt(indice, valor);
		} else {
			sentencia.setNull(indice, Types.INTEGER);
		}
	}

	/**
	 * Convierte la fila actual del resultado en un potencial.
	 *
	 * @param resultados resultado situado en una fila válida
	 * @return potencial construido desde la fila
	 * @throws SQLException si falla la lectura
	 */
	private Potencial mapear(ResultSet resultados) throws SQLException {
		Potencial potencial = new Potencial();
		potencial.setIdPotencial(resultados.getInt("id_potencial"));
		potencial.setNombre(resultados.getString("nombre"));
		potencial.setEdad(resultados.getInt("edad"));
		potencial.setCiudadOrigen(resultados.getString("ciudad_origen"));
		potencial.setPaisActual(resultados.getString("pais_actual"));
		potencial.setFuerza(resultados.getInt("fuerza"));
		potencial.setVelocidad(resultados.getInt("velocidad"));
		potencial.setEstado(resultados.getString("estado"));
		potencial.setIdAdminAsignado(resultados.getInt("id_admin_asignado"));
		potencial.setIdGrupo(resultados.getInt("id_grupo"));
		potencial.setSaldoCalculado(resultados.getDouble("saldo_calculado"));
		return potencial;
	}
}
