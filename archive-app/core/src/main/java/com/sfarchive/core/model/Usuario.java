package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cuenta con acceso a la aplicación de gestión (solo administradores y potenciales). */
public record Usuario(
        int id, String username, String email, Rol rol, String nombreCompleto,
        boolean activo, LocalDateTime ultimoAcceso, Integer potencialId
) { }
