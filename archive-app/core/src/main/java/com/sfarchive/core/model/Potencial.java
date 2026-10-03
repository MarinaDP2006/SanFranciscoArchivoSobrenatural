package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Agente de la red con habilidades sobrenaturales. Su saldo es el monedero. */
public record Potencial(
        int id, Integer usuarioId, String username, String alias, String nombreReal, Integer edad,
        String habilidad, String descripcion, int nivel, EstadoPotencial estado,
        String barrio, String ciudad, String pais, Double lat, Double lng,
        Integer grupoId, String grupoNombre, BigDecimal saldo, LocalDate fechaReclutamiento
) { }
