package com.tecnomovil.modelo;

/**
 * Cuántas personas entraron a una estación durante el día.
 */
public record ConteoEstacion(String estacion, long ingresos) {

    public ConteoEstacion {
        if (estacion == null || estacion.isBlank()) {
            throw new IllegalArgumentException("estacion obligatoria");
        }
        if (ingresos < 0) {
            throw new IllegalArgumentException("ingresos negativo");
        }
    }
}
