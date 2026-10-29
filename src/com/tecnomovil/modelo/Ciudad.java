package com.tecnomovil.modelo;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * El día y la hora de TecnoValle. Las horas pico se calculan con esta zona,
 * no con la del computador donde corre el programa.
 */
public final class Ciudad {

    public static final String NOMBRE = "TecnoValle";
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    public static final LocalDate DIA_REFERENCIA = LocalDate.of(2026, 3, 18);

    private Ciudad() {
    }
}
