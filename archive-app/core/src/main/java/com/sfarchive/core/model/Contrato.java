package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Contrato de anomalía: la única forma de resolver un incidente (tabla {@code contratos}).
 * Incluye también datos del incidente, del grupo y del potencial (vienen de JOIN) para no tener que hacer más consultas en las pantallas.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Contrato(
        /** Clave primaria. */
        int id,
        /** Código visible, p. ej. CTR-2026-007. */
        String codigo,
        /** Incidente que resuelve. */
        int incidenteId,
        /** Código del incidente (JOIN). */
        String incidenteCodigo,
        /** Titular del incidente (JOIN). */
        String incidenteTitulo,
        /** Tipo del incidente (JOIN). */
        TipoIncidente tipo,
        /** Barrio del incidente (JOIN). */
        String barrio,
        /** Latitud del incidente (para calcular el transporte). */
        Double lat,
        /** Longitud del incidente. */
        Double lng,
        /** Aviso ciudadano de origen (puede ser null). */
        Integer solicitudId,
        /** Fase del contrato (ver EstadoContrato). */
        EstadoContrato estado,
        /** Urgencia. */
        Prioridad prioridad,
        /** Grupo táctico asignado (null si aún no se asignó). */
        Integer grupoId,
        /** Nombre del grupo (JOIN). */
        String grupoNombre,
        /** Potencial asignado (null si aún no se asignó). */
        Integer potencialId,
        /** Alias del potencial (JOIN). */
        String potencialAlias,
        /** Dinero que cobra el potencial al completarlo (USD). */
        BigDecimal recompensa,
        /** Gasto de transporte que se le descontó al asignarlo. */
        BigDecimal costeTransporte,
        /** Km entre el potencial y el incidente al asignarlo. */
        BigDecimal distanciaKm,
        /** Informe de campo que escribe el potencial. */
        String notasCampo,
        /** Cuándo se creó. */
        LocalDateTime fechaSolicitud,
        /** Cuándo se asignó. */
        LocalDateTime fechaAsignacion,
        /** Cuándo se completó/falló/canceló. */
        LocalDateTime fechaCierre,
        /** Si ya existe su informe en el Archivo Restringido. */
        boolean tieneInforme
) { }
