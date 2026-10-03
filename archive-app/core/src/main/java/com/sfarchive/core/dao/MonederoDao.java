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

/**
 * Acceso a la tabla {@code transacciones_monedero} y al saldo de los potenciales.
 * <p>
 * Es un DAO (Data Access Object): SOLO lee y escribe en la base de datos.
 * No decide reglas de negocio (eso lo hacen las clases de {@code service}).
 * Todo el SQL usa {@code ?} (PreparedStatement) a través de la clase {@code Jdbc}.
 */
public class MonederoDao {

    /** Consulta base con JOIN para traer el alias, el código del contrato y quién hizo el movimiento. */
    private static final String SELECT = """
            SELECT t.*, p.alias, c.codigo AS contrato_codigo, u.nombre_completo AS realizado_nombre
            FROM transacciones_monedero t
            JOIN potenciales p ON p.id = t.potencial_id
            LEFT JOIN contratos c ON c.id = t.contrato_id
            LEFT JOIN usuarios u ON u.id = t.realizado_por
            """;

    /**
     * Convierte la fila actual del ResultSet en un objeto Transaccion.
     * Se usa como "mapper": {@code Jdbc.query(sql, MonederoDao::map)}.
     */
    static Transaccion map(ResultSet rs) throws SQLException {
        return new Transaccion(rs.getInt("id"), rs.getInt("potencial_id"), rs.getString("alias"),
                Jdbc.intOrNull(rs, "contrato_id"), rs.getString("contrato_codigo"),
                TipoTransaccion.valueOf(rs.getString("tipo")), rs.getBigDecimal("importe"),
                rs.getBigDecimal("saldo_resultante"), rs.getString("concepto"), Jdbc.dateTime(rs, "fecha"),
                rs.getString("realizado_nombre"));
    }

    /** Movimientos de un potencial, del más reciente al más antiguo. */
    public List<Transaccion> historial(int potencialId) {
        return Jdbc.query(SELECT + " WHERE t.potencial_id = ? ORDER BY t.fecha DESC, t.id DESC", MonederoDao::map, potencialId);
    }

    /** Los últimos movimientos de todos los potenciales. */
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
        // 1) Leemos el saldo actual y BLOQUEAMOS la fila (FOR UPDATE): si otro admin hace un movimiento
        //    a la vez, tiene que esperar a que acabemos. Así nunca se pierde dinero por el camino.
        BigDecimal saldo = Jdbc.one(c, "SELECT saldo FROM potenciales WHERE id = ? FOR UPDATE",
                        rs -> rs.getBigDecimal(1), potencialId)
                .orElseThrow(() -> new DataException("El potencial no existe."));
        // 2) Calculamos el nuevo saldo (importe positivo suma, negativo resta)
        BigDecimal nuevo = saldo.add(importe);
        // 3) Guardamos el saldo nuevo en la ficha del potencial
        Jdbc.update(c, "UPDATE potenciales SET saldo = ? WHERE id = ?", nuevo, potencialId);
        // 4) Y apuntamos el movimiento en el historial, con el saldo que queda
        Jdbc.insert(c, """
                INSERT INTO transacciones_monedero (potencial_id, contrato_id, tipo, importe, saldo_resultante, concepto, fecha, realizado_por)
                VALUES (?,?,?,?,?,?,?,?)""", potencialId, contratoId, tipo, importe, nuevo, concepto,
                LocalDateTime.now(), usuarioId);
        return nuevo;
    }

    /** Suma de los saldos de todos los potenciales. */
    public BigDecimal totalEnCirculacion() {
        return Jdbc.one("SELECT COALESCE(SUM(saldo),0) FROM potenciales", rs -> rs.getBigDecimal(1)).orElse(BigDecimal.ZERO);
    }
}
