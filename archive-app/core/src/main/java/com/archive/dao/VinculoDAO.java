package com.archive.dao;

import com.archive.model.Vinculo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Proporciona operaciones JDBC para los vínculos de potenciales.
 */
public class VinculoDAO {

	/**
	 * Inserta un vínculo y establece su identificador generado.
	 *
	 * @param vinculo vínculo que se guarda
	 * @return identificador generado
	 * @throws SQLException si falla la inserción
	 */
	public int crear(Vinculo vinculo) throws SQLException {
		String sql = "INSERT INTO VINCULO "
				+ "(id_potencial, tipo_vinculo, nombre_persona, ciudad_proteccion) VALUES (?, ?, ?, ?)";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(
						 sql, Statement.RETURN_GENERATED_KEYS)) {
			sentencia.setInt(1, vinculo.getIdPotencial());
			sentencia.setString(2, vinculo.getTipoVinculo());
			sentencia.setString(3, vinculo.getNombrePersona());
			sentencia.setString(4, vinculo.getCiudadProteccion());
			sentencia.executeUpdate();
			try (ResultSet claves = sentencia.getGeneratedKeys()) {
				if (!claves.next()) {
					throw new SQLException("No se generó el identificador del vínculo.");
				}
				int idGenerado = claves.getInt(1);
				vinculo.setIdVinculo(idGenerado);
				return idGenerado;
			}
		}
	}

	/**
	 * Busca un vínculo por identificador.
	 *
	 * @param idVinculo identificador que se busca
	 * @return vínculo encontrado o null si no existe
	 * @throws SQLException si falla la consulta
	 */
	public Vinculo obtenerPorId(int idVinculo) throws SQLException {
		String sql = "SELECT * FROM VINCULO WHERE id_vinculo = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idVinculo);
			try (ResultSet resultados = sentencia.executeQuery()) {
				return resultados.next() ? mapear(resultados) : null;
			}
		}
	}

	/**
	 * Recupera los vínculos de un potencial.
	 *
	 * @param idPotencial identificador del potencial
	 * @return vínculos ordenados por identificador
	 * @throws SQLException si falla la consulta
	 */
	public List<Vinculo> listarPorPotencial(int idPotencial) throws SQLException {
		String sql = "SELECT * FROM VINCULO WHERE id_potencial = ? ORDER BY id_vinculo";
		List<Vinculo> vinculos = new ArrayList<>();

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idPotencial);
			try (ResultSet resultados = sentencia.executeQuery()) {
				while (resultados.next()) {
					vinculos.add(mapear(resultados));
				}
			}
		}
		return vinculos;
	}

	/**
	 * Actualiza un vínculo.
	 *
	 * @param vinculo vínculo con los nuevos datos
	 * @return true si se actualizó una fila
	 * @throws SQLException si falla la actualización
	 */
	public boolean actualizar(Vinculo vinculo) throws SQLException {
		String sql = "UPDATE VINCULO SET id_potencial = ?, tipo_vinculo = ?, nombre_persona = ?, "
				+ "ciudad_proteccion = ? WHERE id_vinculo = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, vinculo.getIdPotencial());
			sentencia.setString(2, vinculo.getTipoVinculo());
			sentencia.setString(3, vinculo.getNombrePersona());
			sentencia.setString(4, vinculo.getCiudadProteccion());
			sentencia.setInt(5, vinculo.getIdVinculo());
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Elimina un vínculo por identificador.
	 *
	 * @param idVinculo identificador del vínculo
	 * @return true si se eliminó una fila
	 * @throws SQLException si falla la eliminación
	 */
	public boolean eliminar(int idVinculo) throws SQLException {
		String sql = "DELETE FROM VINCULO WHERE id_vinculo = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idVinculo);
			return sentencia.executeUpdate() == 1;
		}
	}

	/**
	 * Convierte la fila actual del resultado en un vínculo.
	 *
	 * @param resultados resultado situado en una fila válida
	 * @return vínculo construido desde la fila
	 * @throws SQLException si falla la lectura
	 */
	private Vinculo mapear(ResultSet resultados) throws SQLException {
		Vinculo vinculo = new Vinculo();
		vinculo.setIdVinculo(resultados.getInt("id_vinculo"));
		vinculo.setIdPotencial(resultados.getInt("id_potencial"));
		vinculo.setTipoVinculo(resultados.getString("tipo_vinculo"));
		vinculo.setNombrePersona(resultados.getString("nombre_persona"));
		vinculo.setCiudadProteccion(resultados.getString("ciudad_proteccion"));
		return vinculo;
	}
}
