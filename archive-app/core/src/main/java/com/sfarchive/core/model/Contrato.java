package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Contrato de anomalía: la única forma de resolver un incidente. */
public record Contrato(
        int id, String codigo, int incidenteId, String incidenteCodigo, String incidenteTitulo,
        TipoIncidente tipo, String barrio, Double lat, Double lng, Integer solicitudId,
        EstadoContrato estado, Prioridad prioridad, Integer grupoId, String grupoNombre,
        Integer potencialId, String potencialAlias, BigDecimal recompensa,
        BigDecimal costeTransporte, BigDecimal distanciaKm, String notasCampo,
        LocalDateTime fechaSolicitud, LocalDateTime fechaAsignacion, LocalDateTime fechaCierre,
        boolean tieneInforme
) { }
