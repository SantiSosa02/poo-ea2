package com.tecnomovil.app;

import com.tecnomovil.analisis.AnalizadorTransporte;
import com.tecnomovil.datos.GeneradorDatos;
import com.tecnomovil.modelo.Ciudad;
import com.tecnomovil.modelo.ConteoEstacion;
import com.tecnomovil.modelo.EstadoRuta;
import com.tecnomovil.modelo.FlujoHorario;
import com.tecnomovil.modelo.RegistroViaje;
import com.tecnomovil.modelo.ResumenTiempos;
import com.tecnomovil.modelo.Tramo;
import com.tecnomovil.modelo.UsoRuta;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arranque del programa. Genera el día, llama al analizador e imprime.
 * El analizador no escribe en pantalla: eso lo hacemos solo aquí.
 */
public final class Main {

    private static final int USUARIOS = 50_000;
    private static final int UMBRAL_OCUPACION = 1_500;
    private static final int MUESTRA_PATRONES = 5;
    private static final int TRAMOS_VISIBLES = 8;

    private Main() {
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));

        int usuarios = args.length > 0 ? Integer.parseInt(args[0]) : USUARIOS;
        int umbral = args.length > 1 ? Integer.parseInt(args[1]) : UMBRAL_OCUPACION;

        System.out.println("TecnoMovil Data — procesamiento funcional de la movilidad urbana");
        System.out.println("Ciudad de referencia : " + Ciudad.NOMBRE);
        System.out.println("Día analizado        : " + Ciudad.DIA_REFERENCIA + " (" + Ciudad.ZONA + ")");
        System.out.println("Procesadores lógicos : " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        Verificacion.ejecutar();

        long inicioGeneracion = System.nanoTime();
        List<RegistroViaje> dia = GeneradorDatos.generarDia(usuarios, GeneradorDatos.SEMILLA_DEMO);
        long finGeneracion = System.nanoTime();

        System.out.println("Día simulado");
        System.out.println("  Usuarios           : " + formato(usuarios));
        System.out.println("  Registros          : " + formato(dia.size()));
        System.out.println("  Semilla            : " + GeneradorDatos.SEMILLA_DEMO);
        System.out.println("  Generación         : " + millis(inicioGeneracion, finGeneracion));
        System.out.println("  Umbral de ocupación: " + formato(umbral) + " pasajeros simultáneos");
        System.out.println();

        List<ConteoEstacion> afluencia = medir("a) Afluencia por estación",
                () -> AnalizadorTransporte.afluenciaPorEstacion(dia));
        imprimirAfluencia(afluencia);
        compararParalelismo(dia);

        List<FlujoHorario> horas = medir("b) Horas pico",
                () -> AnalizadorTransporte.identificarHorasPico(dia));
        imprimirHoras(horas);

        List<UsoRuta> rutas = medir("c) Rutas más utilizadas",
                () -> AnalizadorTransporte.rutasMasUtilizadas(dia));
        imprimirRutas(rutas);

        Map<String, List<String>> patrones = medir("d) Patrones de viaje por usuario",
                () -> AnalizadorTransporte.patronesPorUsuario(dia));
        imprimirPatrones(patrones);

        ResumenTiempos tiempos = medir("e) Tiempo promedio entre estaciones",
                () -> AnalizadorTransporte.tiempoPromedioEntreEstaciones(dia));
        imprimirTiempos(tiempos);

        List<EstadoRuta> sobrecarga = medir("f) Detección de sobrecarga",
                () -> AnalizadorTransporte.detectarSobrecarga(dia, umbral));
        imprimirSobrecarga(sobrecarga, umbral);
    }

    private static void imprimirAfluencia(List<ConteoEstacion> afluencia) {
        long total = afluencia.stream().mapToLong(ConteoEstacion::ingresos).sum();
        System.out.println("  Ingresos del día   : " + formato(total));
        System.out.printf("  %-28s %12s %10s%n", "Estación", "Ingresos", "Participación");
        for (ConteoEstacion conteo : afluencia) {
            double participacion = total == 0 ? 0d : (100d * conteo.ingresos()) / total;
            System.out.printf("  %-28s %12s %9.1f%%%n",
                    conteo.estacion(), formato(conteo.ingresos()), participacion);
        }
        System.out.println();
    }

    private static void imprimirHoras(List<FlujoHorario> horas) {
        System.out.printf("  %-16s %12s %8s%n", "Franja", "Registros", "Marca");
        for (FlujoHorario flujo : horas) {
            System.out.printf("  %-16s %12s %8s%n",
                    flujo.etiqueta(),
                    formato(flujo.registros()),
                    flujo.pico() ? "PICO" : "");
        }
        System.out.println();
    }

    private static void imprimirRutas(List<UsoRuta> rutas) {
        long total = rutas.stream().mapToLong(UsoRuta::abordajes).sum();
        System.out.printf("  %-28s %12s %10s%n", "Ruta", "Abordajes", "Participación");
        for (UsoRuta uso : rutas) {
            double participacion = total == 0 ? 0d : (100d * uso.abordajes()) / total;
            System.out.printf("  %-28s %12s %9.1f%%%n",
                    uso.ruta(), formato(uso.abordajes()), participacion);
        }
        System.out.println();
    }

    private static void imprimirPatrones(Map<String, List<String>> patrones) {
        System.out.println("  Usuarios con patrón: " + formato(patrones.size()));
        System.out.println("  Muestra de los primeros " + MUESTRA_PATRONES + " identificadores:");
        patrones.entrySet().stream()
                .limit(MUESTRA_PATRONES)
                .forEach(entrada -> System.out.println("  " + entrada.getKey() + ": "
                        + String.join(" → ", entrada.getValue())));
        System.out.println();
    }

    private static void imprimirTiempos(ResumenTiempos tiempos) {
        System.out.println("  Viajes medidos     : " + formato(tiempos.viajesMedidos()));
        System.out.println("  Promedio global    : " + formato(tiempos.promedioGlobal()));
        System.out.println("  Tramos distintos   : " + formato(tiempos.tramos().size()));
        System.out.println("  Tramos más lentos:");
        tiempos.tramos().stream().limit(TRAMOS_VISIBLES).forEach(tramo ->
                System.out.printf("  %-52s %8s  (%s viajes)%n",
                        tramo.etiqueta(), formato(tramo.promedio()), formato(tramo.viajes())));
        System.out.println();
    }

    private static void imprimirSobrecarga(List<EstadoRuta> rutas, int umbral) {
        long criticas = rutas.stream().filter(EstadoRuta::critica).count();
        System.out.println("  Rutas críticas     : " + formato(criticas) + " de " + formato(rutas.size()));
        System.out.println("  Criterio           : ocupación máxima > " + formato(umbral));
        System.out.printf("  %-28s %12s %14s %10s%n", "Ruta", "Abordajes", "Ocupación máx.", "Estado");
        for (EstadoRuta estado : rutas) {
            System.out.printf("  %-28s %12s %14s %10s%n",
                    estado.ruta(),
                    formato(estado.abordajes()),
                    formato(estado.ocupacionMaxima()),
                    estado.clasificacion());
        }
        System.out.println();
    }

    private static void compararParalelismo(List<RegistroViaje> dia) {
        AnalizadorTransporte.afluenciaPorEstacion(dia, true);
        long t0 = System.nanoTime();
        List<ConteoEstacion> secuencial = AnalizadorTransporte.afluenciaPorEstacion(dia, false);
        long t1 = System.nanoTime();
        List<ConteoEstacion> paralelo = AnalizadorTransporte.afluenciaPorEstacion(dia, true);
        long t2 = System.nanoTime();

        System.out.println("  Equivalencia secuencial/paralela: " + (secuencial.equals(paralelo) ? "coinciden" : "difieren"));
        System.out.println("  Tiempo secuencial  : " + millis(t0, t1));
        System.out.println("  Tiempo paralelo    : " + millis(t1, t2));
        System.out.println();
    }

    private static <T> T medir(String titulo, java.util.function.Supplier<T> operacion) {
        System.out.println(titulo);
        long inicio = System.nanoTime();
        T resultado = operacion.get();
        long fin = System.nanoTime();
        System.out.println("  Tiempo de cálculo  : " + millis(inicio, fin));
        return resultado;
    }

    private static String formato(long valor) {
        return String.format(Locale.forLanguageTag("es-CO"), "%,d", valor);
    }

    private static String formato(Duration duracion) {
        long segundosTotales = duracion.getSeconds();
        long horas = segundosTotales / 3_600;
        long minutos = (segundosTotales % 3_600) / 60;
        long segundos = segundosTotales % 60;
        if (horas > 0) {
            return horas + " h " + minutos + " min " + segundos + " s";
        }
        return minutos + " min " + segundos + " s";
    }

    private static String millis(long inicio, long fin) {
        return String.format(Locale.forLanguageTag("es-CO"), "%.1f ms", (fin - inicio) / 1_000_000d);
    }
}
