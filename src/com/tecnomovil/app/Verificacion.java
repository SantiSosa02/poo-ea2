package com.tecnomovil.app;

import com.tecnomovil.analisis.AnalizadorTransporte;
import com.tecnomovil.datos.GeneradorDatos;
import com.tecnomovil.modelo.ConteoEstacion;
import com.tecnomovil.modelo.EstadoRuta;
import com.tecnomovil.modelo.FlujoHorario;
import com.tecnomovil.modelo.RegistroViaje;
import com.tecnomovil.modelo.ResumenTiempos;
import com.tecnomovil.modelo.Tramo;
import com.tecnomovil.modelo.UsoRuta;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pruebas sin JUnit. Usamos un día chiquito que calculamos a mano, y también
 * revisamos que las listas no cambien y que el paralelo dé lo mismo que el secuencial.
 */
public final class Verificacion {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static int superadas;

    private Verificacion() {
    }

    public static void ejecutar() {
        superadas = 0;
        System.out.println("Verificación funcional (conjunto controlado)");
        comprobarCasoManual();
        comprobarBordesEInmutabilidad();
        comprobarGeneradorYParalelismo();
        System.out.println("  Resultado: " + superadas + " comprobaciones superadas.");
        System.out.println();
    }

    private static void comprobarCasoManual() {
        List<RegistroViaje> datos = List.of(
                evento("U1", "R1-Centro", "Estación Central", RegistroViaje.ENTRADA, 8, 0),
                evento("U1", "R1-Centro", "Estación Norte", RegistroViaje.SALIDA, 8, 20),
                evento("U1", "R2-Norte", "Estación Norte", RegistroViaje.ENTRADA, 8, 25),
                evento("U1", "R2-Norte", "Estación Universidad", RegistroViaje.SALIDA, 8, 40),
                evento("U2", "R1-Centro", "Estación Central", RegistroViaje.ENTRADA, 8, 5),
                evento("U2", "R1-Centro", "Estación Mercado", RegistroViaje.SALIDA, 8, 50),
                evento("U2", "R1-Centro", "Estación Central", RegistroViaje.ENTRADA, 18, 0),
                evento("U2", "R1-Centro", "Estación Parque", RegistroViaje.SALIDA, 18, 10)
        );

        comprobar("afluencia por estación",
                AnalizadorTransporte.afluenciaPorEstacion(datos).equals(List.of(
                        new ConteoEstacion("Estación Central", 3),
                        new ConteoEstacion("Estación Norte", 1))));

        List<FlujoHorario> horas = AnalizadorTransporte.identificarHorasPico(datos);
        comprobar("hora pico 08:00", horas.get(0).equals(new FlujoHorario(8, 6, true)));
        comprobar("hora valle 18:00", horas.get(1).equals(new FlujoHorario(18, 2, false)));

        comprobar("rutas más utilizadas",
                AnalizadorTransporte.rutasMasUtilizadas(datos).equals(List.of(
                        new UsoRuta("R1-Centro", 3),
                        new UsoRuta("R2-Norte", 1))));

        Map<String, List<String>> patrones = AnalizadorTransporte.patronesPorUsuario(datos);
        comprobar("patrón de U1", patrones.get("U1").equals(List.of(
                "Estación Central", "Estación Norte", "Estación Norte", "Estación Universidad")));
        comprobar("patrón de U2", patrones.get("U2").equals(List.of(
                "Estación Central", "Estación Mercado", "Estación Central", "Estación Parque")));

        ResumenTiempos tiempos = AnalizadorTransporte.tiempoPromedioEntreEstaciones(datos);
        comprobar("cuatro tramos medidos", tiempos.viajesMedidos() == 4);
        comprobar("promedio global de 22 min 30 s",
                tiempos.promedioGlobal().equals(Duration.ofMinutes(22).plusSeconds(30)));
        comprobar("tramo más lento Central → Mercado",
                tiempos.tramos().get(0).equals(new Tramo(
                        "Estación Central", "Estación Mercado", 1, Duration.ofMinutes(45))));

        List<EstadoRuta> rutas = AnalizadorTransporte.detectarSobrecarga(datos, 1);
        comprobar("R1 crítica al superar el umbral 1",
                rutas.get(0).equals(new EstadoRuta("R1-Centro", 3, 2, true)));
        comprobar("R2 permanece normal",
                rutas.get(1).equals(new EstadoRuta("R2-Norte", 1, 1, false)));
        comprobar("umbral igual a la ocupación no marca crítica",
                AnalizadorTransporte.detectarSobrecarga(datos, 2).stream().noneMatch(EstadoRuta::critica));
    }

    private static void comprobarBordesEInmutabilidad() {
        comprobar("día vacío sin afluencia", AnalizadorTransporte.afluenciaPorEstacion(List.of()).isEmpty());
        comprobar("día vacío sin horas", AnalizadorTransporte.identificarHorasPico(List.of()).isEmpty());
        comprobar("día vacío con promedio cero",
                AnalizadorTransporte.tiempoPromedioEntreEstaciones(List.of()).viajesMedidos() == 0);

        List<RegistroViaje> base = List.of(
                evento("U1", "R1-Centro", "Estación Central", RegistroViaje.ENTRADA, 9, 0),
                evento("U1", "R1-Centro", "Estación Sur", RegistroViaje.SALIDA, 9, 15)
        );
        ArrayList<RegistroViaje> mutable = new ArrayList<>(base);
        List<RegistroViaje> fotografia = List.copyOf(mutable);

        AnalizadorTransporte.afluenciaPorEstacion(mutable, true);
        AnalizadorTransporte.identificarHorasPico(mutable);
        AnalizadorTransporte.rutasMasUtilizadas(mutable, true);
        Map<String, List<String>> patrones = AnalizadorTransporte.patronesPorUsuario(mutable);
        AnalizadorTransporte.tiempoPromedioEntreEstaciones(mutable);
        List<EstadoRuta> estados = AnalizadorTransporte.detectarSobrecarga(mutable, 10);

        comprobar("la lista original conserva su contenido", mutable.equals(fotografia));
        comprobar("la afluencia devuelta es inmodificable", noAdmiteAlta(AnalizadorTransporte.afluenciaPorEstacion(base)));
        comprobar("el patrón devuelto es inmodificable", noAdmiteAlta(patrones.get("U1")));
        comprobar("el mapa de patrones es inmodificable", noAdmiteInsercion(patrones));
        comprobar("la sobrecarga devuelta es inmodificable", noAdmiteAlta(estados));

        comprobar("rechaza una acción desconocida", accionInvalidaEsRechazada());
        comprobar("rechaza un umbral negativo", umbralNegativoEsRechazado());
    }

    private static void comprobarGeneradorYParalelismo() {
        List<RegistroViaje> primera = GeneradorDatos.generarDia(120, 99L);
        List<RegistroViaje> segunda = GeneradorDatos.generarDia(120, 99L);
        comprobar("la misma semilla reproduce el día", primera.equals(segunda));
        comprobar("otra semilla produce otro día", !primera.equals(GeneradorDatos.generarDia(120, 100L)));

        List<RegistroViaje> muestra = GeneradorDatos.generarDia(2_000, 7L);
        comprobar("la afluencia paralela coincide con la secuencial",
                AnalizadorTransporte.afluenciaPorEstacion(muestra, false)
                        .equals(AnalizadorTransporte.afluenciaPorEstacion(muestra, true)));
        comprobar("el ranking de rutas paralelo coincide con el secuencial",
                AnalizadorTransporte.rutasMasUtilizadas(muestra, false)
                        .equals(AnalizadorTransporte.rutasMasUtilizadas(muestra, true)));
    }

    private static RegistroViaje evento(String usuario, String ruta, String estacion, String accion, int hora, int minuto) {
        return new RegistroViaje(
                usuario,
                ruta,
                estacion,
                accion,
                LocalDateTime.of(2026, 3, 18, hora, minuto).atZone(ZONA).toInstant());
    }

    private static boolean accionInvalidaEsRechazada() {
        try {
            new RegistroViaje("U1", "R1-Centro", "Estación Central", "espera",
                    LocalDateTime.of(2026, 3, 18, 8, 0).atZone(ZONA).toInstant());
            return false;
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    private static boolean umbralNegativoEsRechazado() {
        try {
            AnalizadorTransporte.detectarSobrecarga(List.of(), -1);
            return false;
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    private static boolean noAdmiteAlta(List<?> lista) {
        try {
            lista.add(null);
            return false;
        } catch (UnsupportedOperationException ex) {
            return true;
        }
    }

    private static boolean noAdmiteInsercion(Map<String, List<String>> patrones) {
        try {
            patrones.put("UX", List.of());
            return false;
        } catch (UnsupportedOperationException ex) {
            return true;
        }
    }

    private static void comprobar(String nombre, boolean condicion) {
        if (!condicion) {
            throw new AssertionError("Falló la comprobación: " + nombre);
        }
        superadas++;
        System.out.println("  [OK] " + nombre);
    }
}
