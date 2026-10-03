package com.sfarchive.core.service;

import com.sfarchive.core.dao.ActividadDao;
import com.sfarchive.core.dao.MonederoDao;
import com.sfarchive.core.db.DataException;
import com.sfarchive.core.db.Jdbc;
import com.sfarchive.core.model.TipoTransaccion;

import java.math.BigDecimal;

/**
 * Ajustes manuales del monedero que hace un administrador (pantalla Monederos).
 * Las recompensas y el transporte NO se hacen aquí: los genera ContratoService automáticamente.
 * <p>
 * Es un servicio: contiene las REGLAS (qué se puede hacer y qué no) y usa los DAO para guardar.
 * Si algo no está permitido lanza {@code DataException} con el mensaje que verá el usuario.
 */
public class MonederoService {

    // DAOs que necesita este servicio (cada uno habla con su tabla)
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
        // Decidimos el signo según el tipo, así da igual si el admin escribe 100 o -100
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
