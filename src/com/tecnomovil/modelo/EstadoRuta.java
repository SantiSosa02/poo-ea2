package com.tecnomovil.modelo;

/**
 * Lo más llena que estuvo una ruta al mismo tiempo. Si ese número pasa
 * el umbral, la marcamos como crítica.
 */
public record EstadoRuta(String ruta, long abordajes, int ocupacionMaxima, boolean critica) {

    public EstadoRuta {
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalArgumentException("ruta obligatoria");
        }
        if (abordajes < 0 || ocupacionMaxima < 0) {
            throw new IllegalArgumentException("los conteos no pueden ser negativos");
        }
    }

    public String clasificacion() {
        return critica ? "CRÍTICA" : "normal";
    }
}
