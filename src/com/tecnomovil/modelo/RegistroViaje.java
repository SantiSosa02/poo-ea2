package com.tecnomovil.modelo;

import java.time.Instant;
import java.util.Objects;

/**
 * Una validación del sistema. Usamos record para que no se pueda modificar
 * después de creada. Si dos registros traen los mismos datos, son el mismo viaje.
 */
public record RegistroViaje(
        String idUsuario,
        String ruta,
        String estacion,
        String accion,
        Instant timestamp
) {

    public static final String ENTRADA = "entrada";
    public static final String SALIDA = "salida";

    public RegistroViaje {
        idUsuario = textoObligatorio(idUsuario, "idUsuario");
        ruta = textoObligatorio(ruta, "ruta");
        estacion = textoObligatorio(estacion, "estacion");
        accion = accionValida(accion);
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public boolean esEntrada() {
        return ENTRADA.equals(accion);
    }

    public boolean esSalida() {
        return SALIDA.equals(accion);
    }

    private static String textoObligatorio(String valor, String nombre) {
        Objects.requireNonNull(valor, nombre);
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException(nombre + " no puede estar vacío");
        }
        return limpio;
    }

    private static String accionValida(String accion) {
        String limpia = textoObligatorio(accion, "accion");
        if (!ENTRADA.equals(limpia) && !SALIDA.equals(limpia)) {
            throw new IllegalArgumentException("accion debe ser 'entrada' o 'salida'");
        }
        return limpia;
    }
}
