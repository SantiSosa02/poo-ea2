package com.tecnomovil.modelo;

/**
 * Cuántos registros cayeron en una hora. pico queda en true cuando esa hora
 * fue la de mayor movimiento, o empató con ella.
 */
public record FlujoHorario(int hora, long registros, boolean pico) {

    public FlujoHorario {
        if (hora < 0 || hora > 23) {
            throw new IllegalArgumentException("hora fuera de 0..23");
        }
        if (registros < 0) {
            throw new IllegalArgumentException("registros negativo");
        }
    }

    public String etiqueta() {
        return String.format("%02d:00–%02d:59", hora, hora);
    }
}
