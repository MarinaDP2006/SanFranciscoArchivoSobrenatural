package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Movimiento del monedero de un potencial (USD). */
public record Transaccion(
        int id, int potencialId, String potencialAlias, Integer contratoId, String contratoCodigo,
        TipoTransaccion tipo, BigDecimal importe, BigDecimal saldoResultante, String concepto,
        LocalDateTime fecha, String realizadoPor
) { }
