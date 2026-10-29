package com.tecnomovil.modelo;

/**
 * Cuántos abordajes tuvo una ruta. Cada "entrada" cuenta como uno.
 */
public record UsoRuta(String ruta, long abordajes) {

    public UsoRuta {
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalArgumentException("ruta obligatoria");
        }
        if (abordajes < 0) {
            throw new IllegalArgumentException("abordajes negativo");
        }
    }
}
