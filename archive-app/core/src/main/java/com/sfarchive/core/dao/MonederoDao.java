package com.sfarchive.core.dao;

import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.TipoTransaccion;
import com.sfarchive.core.model.Transaccion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class MonederoDao {

    private static final String SELECT = """
            SELECT t.*, p.alias, c.codigo AS contrato_codigo, u.nombre_completo AS realizado_nombre
            FROM transacciones_monedero t
            JOIN potenciales p ON p.id = t.potencial_id
            LEFT JOIN contratos c ON c.id = t.contrato_id
            LEFT JOIN usuarios u ON u.id = t.realizado_por
            """;

    static Transaccion map(ResultSet rs) throws SQLException {
        return new Transaccion(rs.getInt("id"), rs.getInt("potencial_id"), rs.getString("alias"),
                Jdbc.intOrNull(rs, "contrato_id"), rs.getString("contrato_codigo"),
                TipoTransaccion.valueOf(rs.getString("tipo")), rs.getBigDecimal("importe"),
                rs.getBigDecimal("saldo_resultante"), rs.getString("concepto"), Jdbc.dateTime(rs, "fecha"),
                rs.getString("realizado_nombre"));
    }

    public List<Transaccion> historial(int potencialId) {
        return Jdbc.query(SELECT + " WHERE t.potencial_id = ? ORDER BY t.fecha DESC, t.id DESC", MonederoDao::map, potencialId);
    }

    public List<Transaccion> recientes(int limite) {
        return Jdbc.query(SELECT + " ORDER BY t.fecha DESC, t.id DESC LIMIT ?", MonederoDao::map, limite);
    }

    /**
     * Registra un movimiento y actualiza el saldo dentro de la transacción recibida.
     * Bloquea la fila del potencial (FOR UPDATE) para evitar saldos inconsistentes.
     *
     * @return saldo resultante
     */
    public BigDecimal registrar(Connection c, int potencialId, Integer contratoId, TipoTransaccion tipo,
                                BigDecimal importe, String concepto, Integer usuarioId) throws SQLException {
        BigDecimal saldo = Jdbc.one(c, "SELECT saldo FROM potenciales WHERE id = ? FOR UPDATE",
                        rs -> rs.getBigDecimal(1), potencialId)
                .orElseThrow(() -> new DataException("El potencial no existe."));
        BigDecimal nuevo = saldo.add(importe);
        Jdbc.update(c, "UPDATE potenciales SET saldo = ? WHERE id = ?", nuevo, potencialId);
        Jdbc.insert(c, """
                INSERT INTO transacciones_monedero (potencial_id, contrato_id, tipo, importe, saldo_resultante, concepto, fecha, realizado_por)
                VALUES (?,?,?,?,?,?,?,?)""", potencialId, contratoId, tipo, importe, nuevo, concepto,
                LocalDateTime.now(), usuarioId);
        return nuevo;
    }

    public BigDecimal totalEnCirculacion() {
        return Jdbc.one("SELECT COALESCE(SUM(saldo),0) FROM potenciales", rs -> rs.getBigDecimal(1)).orElse(BigDecimal.ZERO);
    }
}
