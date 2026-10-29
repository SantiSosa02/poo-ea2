package com.tecnomovil.modelo;

import java.time.Duration;
import java.util.Objects;

/**
 * El tiempo promedio que se demoran los usuarios yendo de una estación a otra.
 */
public record Tramo(String origen, String destino, long viajes, Duration promedio) {

    public Tramo {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        Objects.requireNonNull(promedio, "promedio");
        if (viajes < 1) {
            throw new IllegalArgumentException("viajes debe ser positivo");
        }
        if (promedio.isNegative()) {
            throw new IllegalArgumentException("promedio negativo");
        }
    }

    public String etiqueta() {
        return origen + " → " + destino;
    }
}
