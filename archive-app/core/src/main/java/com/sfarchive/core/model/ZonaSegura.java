package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Punto seguro mostrado en verde en el mapa público. */
public record ZonaSegura(
        int id, String nombre, TipoZona tipo, String direccion, String barrio, double lat, double lng,
        String telefono, String horario, boolean activa
) { }
