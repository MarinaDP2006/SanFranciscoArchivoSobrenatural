package com.sfarchive.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculos geográficos y de coste de transporte.
 * Si quieres cambiar las tarifas, cambia TARIFA_BASE y TARIFA_KM.
 */
public final class Geo {

    /** Tarifa fija por desplazamiento (USD). */
    public static final BigDecimal TARIFA_BASE = new BigDecimal("25.00");
    /** Coste por kilómetro recorrido (USD). */
    public static final BigDecimal TARIFA_KM = new BigDecimal("1.80");

    /** Radio medio de la Tierra, necesario para la fórmula de Haversine. */
    private static final double RADIO_TIERRA_KM = 6371.0;

    /** Clase de utilidades: solo métodos static. */
    private Geo() { }

    /**
     * Distancia en km entre dos coordenadas (fórmula de Haversine).
     * Tiene en cuenta que la Tierra es redonda; para distancias de una ciudad el error es mínimo.
     * Ejemplo: Sausalito → Golden Gate ≈ 4,40 km.
     */
    public static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(a));
    }

    /**
     * Coste de transporte = tarifa base + tarifa por km, redondeado a céntimos.
     * Ejemplo: 4,40 km → 25 + 1,80 × 4,40 = 32,92 USD.
     */
    public static BigDecimal costeTransporte(double km) {
        return TARIFA_BASE.add(TARIFA_KM.multiply(BigDecimal.valueOf(km))).setScale(2, RoundingMode.HALF_UP);
    }

    /** Redondea los km a 2 decimales para guardarlos y mostrarlos. */
    public static BigDecimal redondearKm(double km) {
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
    }
}
