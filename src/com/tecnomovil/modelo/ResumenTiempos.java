package com.tecnomovil.modelo;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * El promedio de todo el día y el detalle de cada par de estaciones.
 * La lista de tramos queda fija.
 */
public record ResumenTiempos(long viajesMedidos, Duration promedioGlobal, List<Tramo> tramos) {

    public ResumenTiempos {
        Objects.requireNonNull(promedioGlobal, "promedioGlobal");
        tramos = List.copyOf(tramos);
        if (viajesMedidos < 0) {
            throw new IllegalArgumentException("viajesMedidos negativo");
        }
        if (promedioGlobal.isNegative()) {
            throw new IllegalArgumentException("promedioGlobal negativo");
        }
    }
}
