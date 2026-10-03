package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Grupo táctico por país/zona (máximo 6 potenciales). */
public record GrupoTactico(
        int id, String nombre, String pais, String ciudad, String zona, Double lat, Double lng,
        String descripcion, boolean activo, int miembros
) { }
