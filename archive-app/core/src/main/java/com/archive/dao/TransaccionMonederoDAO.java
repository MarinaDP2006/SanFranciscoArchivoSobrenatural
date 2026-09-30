package com.archive.dao;

import com.archive.model.TransaccionMonedero;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Proporciona operaciones para movimientos y saldos del monedero.
 */
public class TransaccionMonederoDAO {

	/**
	 * Registra un gasto válido y lo descuenta del saldo en una transacción.
	 * Los ingresos por contrato se crean desde completarContrato en su DAO.
	 *
	 * @param transaccion gasto que se registra
	 * @return identificador generado
	 * @throws SQLException si el gasto no es válido, falta saldo o falla la operación
	 */
	public int crearGasto(TransaccionMonedero transaccion) throws SQLException {
		boolean tipoValido = "GASTO_TRANSPORTE".equals(transaccion.getTipo())
				|| "GASTO_MEDICO".equals(transaccion.getTipo());
		if (!tipoValido || transaccion.getCantidad() <= 0) {
			throw new SQLException("El gasto debe ser positivo y de transporte o médico.");
		}

		String bloquearSaldo = "SELECT saldo_calculado FROM POTENCIAL "
				+ "WHERE id_potencial = ? FOR UPDATE";
		String descontar = "UPDATE POTENCIAL SET saldo_calculado = "
				+ "COALESCE(saldo_calculado, 0) - ? WHERE id_potencial = ? "
				+ "AND COALESCE(saldo_calculado, 0) >= ?";
		String insertar = "INSERT INTO TRANSACCION_MONEDERO "
				+ "(id_potencial, id_contrato, tipo, cantidad, descripcion) VALUES (?, ?, ?, ?, ?)";

		try (Connection conexion = Conexion.get()) {
			conexion.setAutoCommit(false);
			try {
				double saldo;
				try (PreparedStatement sentencia = conexion.prepareStatement(bloquearSaldo)) {
					sentencia.setInt(1, transaccion.getIdPotencial());
					try (ResultSet resultados = sentencia.executeQuery()) {
						if (!resultados.next()) {
							throw new SQLException("No existe el potencial indicado.");
						}
						saldo = resultados.getDouble("saldo_calculado");
					}
				}
				if (saldo < transaccion.getCantidad()) {
					throw new SQLException("El saldo del potencial es insuficiente.");
				}

				try (PreparedStatement sentencia = conexion.prepareStatement(descontar)) {
					sentencia.setDouble(1, transaccion.getCantidad());
					sentencia.setInt(2, transaccion.getIdPotencial());
					sentencia.setDouble(3, transaccion.getCantidad());
					if (sentencia.executeUpdate() != 1) {
						throw new SQLException("No se pudo descontar el gasto.");
					}
				}
				try (PreparedStatement sentencia = conexion.prepareStatement(
						insertar, Statement.RETURN_GENERATED_KEYS)) {
					sentencia.setInt(1, transaccion.getIdPotencial());
					if (transaccion.getIdContrato() == null) {
						sentencia.setNull(2, Types.INTEGER);
					} else {
						sentencia.setInt(2, transaccion.getIdContrato());
					}
					sentencia.setString(3, transaccion.getTipo());
					sentencia.setDouble(4, transaccion.getCantidad());
					sentencia.setString(5, transaccion.getDescripcion());
					sentencia.executeUpdate();
					try (ResultSet claves = sentencia.getGeneratedKeys()) {
						if (!claves.next()) {
							throw new SQLException("No se generó el identificador de la transacción.");
						}
						int idGenerado = claves.getInt(1);
						transaccion.setIdTransaccion(idGenerado);
						conexion.commit();
						return idGenerado;
					}
				}
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
	 * Recupera los movimientos de un potencial en orden cronológico inverso.
	 *
	 * @param idPotencial identificador del potencial
	 * @return lista de movimientos
	 * @throws SQLException si falla la consulta
	 */
	public List<TransaccionMonedero> listarPorPotencial(int idPotencial) throws SQLException {
		String sql = "SELECT * FROM TRANSACCION_MONEDERO WHERE id_potencial = ? "
				+ "ORDER BY fecha DESC, id_transaccion DESC";
		List<TransaccionMonedero> transacciones = new ArrayList<>();

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idPotencial);
			try (ResultSet resultados = sentencia.executeQuery()) {
				while (resultados.next()) {
					transacciones.add(mapear(resultados));
				}
			}
		}
		return transacciones;
	}

	/**
	 * Recupera el saldo almacenado del potencial.
	 *
	 * @param idPotencial identificador del potencial
	 * @return saldo actual
	 * @throws SQLException si el potencial no existe o falla la consulta
	 */
	public double obtenerSaldo(int idPotencial) throws SQLException {
		String sql = "SELECT saldo_calculado FROM POTENCIAL WHERE id_potencial = ?";

		try (Connection conexion = Conexion.get();
				 PreparedStatement sentencia = conexion.prepareStatement(sql)) {
			sentencia.setInt(1, idPotencial);
			try (ResultSet resultados = sentencia.executeQuery()) {
				if (!resultados.next()) {
					throw new SQLException("No existe el potencial indicado.");
				}
				return resultados.getDouble("saldo_calculado");
			}
		}
	}

	/**
	 * Convierte la fila actual del resultado en un movimiento de monedero.
	 *
	 * @param resultados resultado situado en una fila válida
	 * @return transacción construida desde la fila
	 * @throws SQLException si falla la lectura
	 */
	private TransaccionMonedero mapear(ResultSet resultados) throws SQLException {
		TransaccionMonedero transaccion = new TransaccionMonedero();
		transaccion.setIdTransaccion(resultados.getInt("id_transaccion"));
		transaccion.setIdPotencial(resultados.getInt("id_potencial"));
		int idContrato = resultados.getInt("id_contrato");
		transaccion.setIdContrato(resultados.wasNull() ? null : idContrato);
		transaccion.setTipo(resultados.getString("tipo"));
		transaccion.setCantidad(resultados.getDouble("cantidad"));
		Timestamp fecha = resultados.getTimestamp("fecha");
		transaccion.setFecha(fecha == null ? null : fecha.toLocalDateTime());
		transaccion.setDescripcion(resultados.getString("descripcion"));
		return transaccion;
	}
}
