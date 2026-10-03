package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento del monedero de un potencial (tabla {@code transacciones_monedero}). Importe positivo = entra dinero; negativo = sale.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Transaccion(
        /** Clave primaria. */
        int id,
        /** Dueño del monedero. */
        int potencialId,
        /** Alias (JOIN). */
        String potencialAlias,
        /** Contrato relacionado (si lo hay). */
        Integer contratoId,
        /** Código del contrato (JOIN). */
        String contratoCodigo,
        /** RECOMPENSA, TRANSPORTE, AJUSTE, BONUS o PENALIZACION. */
        TipoTransaccion tipo,
        /** Cantidad en USD (con signo). */
        BigDecimal importe,
        /** Saldo del monedero justo después de este movimiento. */
        BigDecimal saldoResultante,
        /** Explicación del movimiento. */
        String concepto,
        /** Cuándo se hizo. */
        LocalDateTime fecha,
        /** Nombre de quien lo registró (JOIN). */
        String realizadoPor
) { }
