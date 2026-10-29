package com.tecnomovil.analisis;

import com.tecnomovil.modelo.Ciudad;
import com.tecnomovil.modelo.ConteoEstacion;
import com.tecnomovil.modelo.EstadoRuta;
import com.tecnomovil.modelo.FlujoHorario;
import com.tecnomovil.modelo.RegistroViaje;
import com.tecnomovil.modelo.ResumenTiempos;
import com.tecnomovil.modelo.Tramo;
import com.tecnomovil.modelo.UsoRuta;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Aquí están las seis consultas del día. Ninguna cambia la lista que recibe
 * ni guarda resultados para la siguiente llamada. Por eso también se pueden
 * correr con parallelStream() y el resultado es el mismo.
 */
public final class AnalizadorTransporte {

    private static final Comparator<RegistroViaje> ORDEN_TEMPORAL =
            Comparator.comparing(RegistroViaje::timestamp)
                    .thenComparing(registro -> registro.esEntrada() ? 0 : 1)
                    .thenComparing(RegistroViaje::estacion);

    private AnalizadorTransporte() {
    }

    /**
     * a) Cuenta cuánta gente entró a cada estación, de la más llena a la más vacía.
     * Si alguien viaja dos veces, suma dos: cada "entrada" es un ingreso.
     */
    public static List<ConteoEstacion> afluenciaPorEstacion(List<RegistroViaje> registros) {
        return afluenciaPorEstacion(registros, false);
    }

    /**
     * La misma cuenta de arriba. Si paralelo es true, usa parallelStream().
     * Cambia la forma de calcular, no los números.
     */
    public static List<ConteoEstacion> afluenciaPorEstacion(List<RegistroViaje> registros, boolean paralelo) {
        Map<String, Long> conteo = flujo(congelar(registros), paralelo)
                .filter(RegistroViaje::esEntrada)
                .collect(Collectors.groupingBy(RegistroViaje::estacion, Collectors.counting()));
        return rankearDescendente(conteo, ConteoEstacion::new);
    }

    /**
     * b) Agrupa los registros por hora y marca como pico la de más movimiento.
     * Si dos horas empatan, las dos quedan como pico.
     */
    public static List<FlujoHorario> identificarHorasPico(List<RegistroViaje> registros) {
        Map<Integer, Long> porHora = congelar(registros).stream()
                .collect(Collectors.groupingBy(
                        registro -> registro.timestamp().atZone(Ciudad.ZONA).getHour(),
                        Collectors.counting()));

        long maximo = porHora.values().stream().mapToLong(Long::longValue).max().orElse(0L);

        return porHora.entrySet().stream()
                .sorted(Comparator
                        .comparingLong((Map.Entry<Integer, Long> entrada) -> entrada.getValue())
                        .reversed()
                        .thenComparingInt(Map.Entry::getKey))
                .map(entrada -> new FlujoHorario(
                        entrada.getKey(),
                        entrada.getValue(),
                        maximo > 0 && entrada.getValue() == maximo))
                .toList();
    }

    /**
     * c) Pone las rutas de la más usada a la menos usada, según los abordajes.
     */
    public static List<UsoRuta> rutasMasUtilizadas(List<RegistroViaje> registros) {
        return rutasMasUtilizadas(registros, false);
    }

    public static List<UsoRuta> rutasMasUtilizadas(List<RegistroViaje> registros, boolean paralelo) {
        Map<String, Long> conteo = flujo(congelar(registros), paralelo)
                .filter(RegistroViaje::esEntrada)
                .collect(Collectors.groupingBy(RegistroViaje::ruta, Collectors.counting()));
        return rankearDescendente(conteo, UsoRuta::new);
    }

    /**
     * d) Para cada usuario, las estaciones en el orden en que las visitó.
     * El mapa y cada recorrido quedan cerrados: después no se les puede agregar nada.
     */
    public static Map<String, List<String>> patronesPorUsuario(List<RegistroViaje> registros) {
        Map<String, List<String>> patrones = congelar(registros).stream()
                .collect(Collectors.groupingBy(RegistroViaje::idUsuario))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entrada -> entrada.getValue().stream()
                                .sorted(ORDEN_TEMPORAL)
                                .map(RegistroViaje::estacion)
                                .toList(),
                        (izquierda, derecha) -> izquierda,
                        TreeMap::new));
        return Collections.unmodifiableMap(patrones);
    }

    /**
     * e) Calcula cuánto se demora la gente entre una estación y la siguiente.
     * Solo contamos una entrada seguida de la salida del mismo usuario y la misma ruta.
     * El tiempo que espera para tomar el próximo bus no entra en el promedio.
     */
    public static ResumenTiempos tiempoPromedioEntreEstaciones(List<RegistroViaje> registros) {
        List<ParViaje> viajes = extraerTramos(congelar(registros));
        if (viajes.isEmpty()) {
            return new ResumenTiempos(0, Duration.ZERO, List.of());
        }

        Duration promedioGlobal = promedio(viajes.stream().map(ParViaje::duracion));

        List<Tramo> tramos = viajes.stream()
                .collect(Collectors.groupingBy(viaje -> new TramoClave(viaje.origen(), viaje.destino())))
                .entrySet().stream()
                .map(entrada -> new Tramo(
                        entrada.getKey().origen(),
                        entrada.getKey().destino(),
                        entrada.getValue().size(),
                        promedio(entrada.getValue().stream().map(ParViaje::duracion))))
                .sorted(Comparator.comparing(Tramo::promedio).reversed()
                        .thenComparing(Tramo::origen)
                        .thenComparing(Tramo::destino))
                .toList();

        return new ResumenTiempos(viajes.size(), promedioGlobal, tramos);
    }

    /**
     * f) Mira cuánta gente iba al mismo tiempo en cada ruta.
     * Entrar suma uno y salir resta uno. Si el máximo pasa el umbral, la ruta queda crítica.
     * Ese conteo tiene que ir en orden de hora, así que no lo partimos en paralelo.
     * Agrupar los eventos por ruta sí se hace con stream.
     */
    public static List<EstadoRuta> detectarSobrecarga(List<RegistroViaje> registros, int umbralOcupacion) {
        if (umbralOcupacion < 0) {
            throw new IllegalArgumentException("el umbral no puede ser negativo");
        }

        return congelar(registros).stream()
                .collect(Collectors.groupingBy(RegistroViaje::ruta))
                .entrySet().stream()
                .map(entrada -> resumirRuta(entrada.getKey(), entrada.getValue(), umbralOcupacion))
                .sorted(Comparator.comparingInt(EstadoRuta::ocupacionMaxima).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }

    private static EstadoRuta resumirRuta(String ruta, List<RegistroViaje> eventos, int umbral) {
        long abordajes = eventos.stream().filter(RegistroViaje::esEntrada).count();
        int ocupacionMaxima = ocupacionMaxima(eventos);
        return new EstadoRuta(ruta, abordajes, ocupacionMaxima, ocupacionMaxima > umbral);
    }

    /**
     * Recorre los eventos ya ordenados por hora. actual y maximo solo existen
     * dentro de este método, y la lista que nos pasaron no se toca.
     */
    private static int ocupacionMaxima(List<RegistroViaje> eventos) {
        List<RegistroViaje> ordenados = eventos.stream().sorted(ORDEN_TEMPORAL).toList();
        int actual = 0;
        int maximo = 0;
        for (RegistroViaje registro : ordenados) {
            int delta = registro.esEntrada() ? 1 : -1;
            actual = Math.max(0, actual + delta);
            maximo = Math.max(maximo, actual);
        }
        return maximo;
    }

    private static List<ParViaje> extraerTramos(List<RegistroViaje> registros) {
        return registros.stream()
                .collect(Collectors.groupingBy(RegistroViaje::idUsuario))
                .values().stream()
                .flatMap(eventos -> tramosDeUsuario(eventos).stream())
                .toList();
    }

    private static List<ParViaje> tramosDeUsuario(List<RegistroViaje> eventos) {
        List<RegistroViaje> ordenados = eventos.stream().sorted(ORDEN_TEMPORAL).toList();
        return IntStream.range(0, ordenados.size() - 1)
                .mapToObj(indice -> new Pareja(ordenados.get(indice), ordenados.get(indice + 1)))
                .filter(pareja -> pareja.primero().esEntrada() && pareja.segundo().esSalida())
                .filter(pareja -> pareja.primero().ruta().equals(pareja.segundo().ruta()))
                .filter(pareja -> !pareja.primero().estacion().equals(pareja.segundo().estacion()))
                .map(pareja -> new ParViaje(
                        pareja.primero().estacion(),
                        pareja.segundo().estacion(),
                        Duration.between(pareja.primero().timestamp(), pareja.segundo().timestamp())))
                .filter(viaje -> !viaje.duracion().isNegative() && !viaje.duracion().isZero())
                .toList();
    }

    private static Duration promedio(Stream<Duration> duraciones) {
        double millis = duraciones.mapToLong(Duration::toMillis).average().orElse(0d);
        return Duration.ofMillis(Math.round(millis));
    }

    private static List<RegistroViaje> congelar(List<RegistroViaje> registros) {
        Objects.requireNonNull(registros, "registros");
        return List.copyOf(registros);
    }

    private static Stream<RegistroViaje> flujo(List<RegistroViaje> registros, boolean paralelo) {
        return paralelo ? registros.parallelStream() : registros.stream();
    }

    /**
     * Ordena un conteo de mayor a menor. Le pasamos la función que arma cada fila,
     * y así la usamos tanto para estaciones como para rutas.
     */
    private static <T> List<T> rankearDescendente(Map<String, Long> conteo, BiFunction<String, Long, T> mapeo) {
        return conteo.entrySet().stream()
                .sorted(Comparator
                        .comparingLong((Map.Entry<String, Long> entrada) -> entrada.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entrada -> mapeo.apply(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    private record Pareja(RegistroViaje primero, RegistroViaje segundo) {
    }

    private record ParViaje(String origen, String destino, Duration duracion) {
    }

    private record TramoClave(String origen, String destino) {
    }
}
