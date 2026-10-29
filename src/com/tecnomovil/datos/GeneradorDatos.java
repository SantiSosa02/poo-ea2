package com.tecnomovil.datos;

import com.tecnomovil.modelo.Ciudad;
import com.tecnomovil.modelo.RegistroViaje;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Arma el día de prueba. Con la misma semilla salen siempre los mismos viajes,
 * para poder correr el programa otra vez y ver las mismas cifras.
 * La lista que devolvemos ya no se puede modificar.
 */
public final class GeneradorDatos {

    public static final long SEMILLA_DEMO = 42L;

    public static final List<String> ESTACIONES = List.of(
            "Estación Central",
            "Estación Norte",
            "Estación Sur",
            "Estación Universidad",
            "Estación Mercado",
            "Estación Terminal",
            "Estación Parque",
            "Estación Hospital",
            "Estación Industrial",
            "Estación Aeropuerto"
    );

    public static final List<String> RUTAS = List.of(
            "R1-Centro",
            "R2-Norte",
            "R3-Sur",
            "R4-Universidad",
            "R5-Perimetral",
            "A1-Alimentadora Mercado",
            "A2-Alimentadora Hospital"
    );

    /** Qué tan usada es cada ruta. Los siete números suman 100. */
    private static final int[] PESOS_RUTA = {35, 25, 15, 10, 8, 5, 2};

    private GeneradorDatos() {
    }

    public static List<RegistroViaje> generarDia(int usuarios, long semilla) {
        if (usuarios < 1) {
            throw new IllegalArgumentException("usuarios debe ser positivo");
        }
        Random random = new Random(semilla);
        List<RegistroViaje> registros = new ArrayList<>(usuarios * 4);
        for (int consecutivo = 1; consecutivo <= usuarios; consecutivo++) {
            registros.addAll(viajesDe(identificador(consecutivo), random));
        }
        return List.copyOf(registros);
    }

    private static List<RegistroViaje> viajesDe(String idUsuario, Random random) {
        int viajes = 1 + random.nextInt(3);
        int cursor = minutoSesgado(random);
        List<RegistroViaje> eventos = new ArrayList<>(viajes * 2);

        for (int viaje = 0; viaje < viajes; viaje++) {
            if (cursor > 22 * 60) {
                break;
            }
            String origen = estacionAleatoria(random);
            String destino = otraEstacion(random, origen);
            String ruta = rutaPonderada(random);
            int duracion = 6 + random.nextInt(30);
            if (cursor + duracion > 23 * 60 + 30) {
                break;
            }
            eventos.add(new RegistroViaje(idUsuario, ruta, origen, RegistroViaje.ENTRADA, instante(cursor)));
            eventos.add(new RegistroViaje(idUsuario, ruta, destino, RegistroViaje.SALIDA, instante(cursor + duracion)));
            cursor = cursor + duracion + 20 + random.nextInt(71);
        }
        return List.copyOf(eventos);
    }

    /**
     * Ponemos buena parte de los viajes entre 7 y 10 de la mañana y entre 5 y 8
     * de la noche, para que el día de prueba sí tenga horas pico.
     */
    private static int minutoSesgado(Random random) {
        double sorteo = random.nextDouble();
        if (sorteo < 0.34d) {
            return entre(random, 7 * 60, 10 * 60);
        }
        if (sorteo < 0.62d) {
            return entre(random, 17 * 60, 20 * 60);
        }
        return entre(random, 5 * 60, 22 * 60);
    }

    private static int entre(Random random, int desdeInclusive, int hastaExclusive) {
        return desdeInclusive + random.nextInt(hastaExclusive - desdeInclusive);
    }

    private static String rutaPonderada(Random random) {
        int ticket = random.nextInt(100);
        int acumulado = 0;
        for (int indice = 0; indice < RUTAS.size(); indice++) {
            acumulado += PESOS_RUTA[indice];
            if (ticket < acumulado) {
                return RUTAS.get(indice);
            }
        }
        return RUTAS.get(RUTAS.size() - 1);
    }

    private static String estacionAleatoria(Random random) {
        return ESTACIONES.get(random.nextInt(ESTACIONES.size()));
    }

    private static String otraEstacion(Random random, String origen) {
        String destino;
        do {
            destino = estacionAleatoria(random);
        } while (destino.equals(origen));
        return destino;
    }

    private static Instant instante(int minutoDelDia) {
        return Ciudad.DIA_REFERENCIA.atStartOfDay(Ciudad.ZONA).plusMinutes(minutoDelDia).toInstant();
    }

    private static String identificador(int consecutivo) {
        return String.format("U%06d", consecutivo);
    }
}
