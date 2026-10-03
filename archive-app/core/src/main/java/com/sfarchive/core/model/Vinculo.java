package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Familiar o allegado de un potencial. */
public record Vinculo(
        int id, int potencialId, String nombre, String parentesco, Integer edad, String ciudad,
        boolean conoceSecreto, boolean enRiesgo, String notas
) { }
