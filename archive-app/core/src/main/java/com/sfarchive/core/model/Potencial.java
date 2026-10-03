package com.sfarchive.core.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agente de la red con habilidades sobrenaturales (tabla {@code potenciales}).
 * Su saldo es el monedero, y cambia con cada movimiento de {@code transacciones_monedero}.
 * <p>
 * Es un {@code record}: Java genera solo el constructor, los "getters" (sin get: {@code x.alias()}),
 * equals, hashCode y toString. Es inmutable: para "cambiarlo" se crea uno nuevo.
 */
public record Potencial(
        /** Clave primaria. */
        int id,
        /** Cuenta con la que entra en la app (puede ser null). */
        Integer usuarioId,
        /** Usuario de esa cuenta (viene de un JOIN). */
        String username,
        /** Nombre en clave (Niebla, Faro, Sombra...). */
        String alias,
        /** Nombre civil. */
        String nombreReal,
        /** Edad (null si no se sabe). */
        Integer edad,
        /** Habilidad principal en pocas palabras. */
        String habilidad,
        /** Explicación larga de la habilidad. */
        String descripcion,
        /** Poder del 1 al 5. */
        int nivel,
        /** DISPONIBLE, EN_MISION, HERIDO o INACTIVO. */
        EstadoPotencial estado,
        /** Barrio donde vive/opera. */
        String barrio,
        /** Ciudad. */
        String ciudad,
        /** País. */
        String pais,
        /** Latitud de su base (para calcular el transporte y pintarlo en el mapa). */
        Double lat,
        /** Longitud de su base. */
        Double lng,
        /** Grupo táctico al que pertenece (null = sin grupo). */
        Integer grupoId,
        /** Nombre del grupo (viene de un JOIN). */
        String grupoNombre,
        /** Dinero del monedero en USD (BigDecimal: nunca double para dinero). */
        BigDecimal saldo,
        /** Cuándo entró en la red. */
        LocalDate fechaReclutamiento
) { }
