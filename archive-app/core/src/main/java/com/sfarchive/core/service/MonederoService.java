package com.sfarchive.core.service;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.MonederoDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.TipoTransaccion;

import java.math.BigDecimal;

public class MonederoService {

    private final MonederoDao monedero = new MonederoDao();
    private final ActividadDao actividad = new ActividadDao();

    /**
     * Ajuste manual de saldo por un administrador.
     * BONUS siempre suma, PENALIZACION siempre resta y AJUSTE respeta el signo indicado.
     */
    public BigDecimal ajustar(int potencialId, TipoTransaccion tipo, BigDecimal importe, String concepto, int adminId) {
        if (importe == null || importe.signum() == 0) throw new DataException("El importe no puede ser 0.");
        if (concepto == null || concepto.isBlank()) throw new DataException("Indica el concepto del movimiento.");
        if (tipo == TipoTransaccion.RECOMPENSA || tipo == TipoTransaccion.TRANSPORTE)
            throw new DataException("Las recompensas y el transporte se generan desde los contratos.");
        BigDecimal valor = switch (tipo) {
            case BONUS -> importe.abs();
            case PENALIZACION -> importe.abs().negate();
            default -> importe;
        };
        return Jdbc.inTransaction(c -> {
            BigDecimal saldo = monedero.registrar(c, potencialId, null, tipo, valor, concepto.trim(), adminId);
            actividad.registrar(c, adminId, "MONEDERO", "POTENCIAL", potencialId, tipo + " " + valor + " USD · " + concepto.trim());
            return saldo;
        });
    }
}
