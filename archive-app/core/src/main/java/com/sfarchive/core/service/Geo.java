package com.sfarchive.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos geográficos y de coste de transporte. */
public final class Geo {

    /** Tarifa fija por desplazamiento (USD). */
    public static final BigDecimal TARIFA_BASE = new BigDecimal("25.00");
    /** Coste por kilómetro recorrido (USD). */
    public static final BigDecimal TARIFA_KM = new BigDecimal("1.80");

    private static final double RADIO_TIERRA_KM = 6371.0;

    private Geo() { }

    /** Distancia en km entre dos coordenadas (fórmula de Haversine). */
    public static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(a));
    }

    /** Coste de transporte = tarifa base + tarifa por km. */
    public static BigDecimal costeTransporte(double km) {
        return TARIFA_BASE.add(TARIFA_KM.multiply(BigDecimal.valueOf(km))).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal redondearKm(double km) {
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
    }
}
