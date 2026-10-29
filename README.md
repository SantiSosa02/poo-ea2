# TecnoMovil Data

Módulo funcional, en Java estándar, para el procesamiento diario de la movilidad urbana de TecnoValle. No usa frameworks ni librerías externas: solo `java.base` (records, streams, lambdas y colecciones inmutables).

## Requisitos

- JDK 17 o superior (el proyecto se compila con `--release 17`).

## Ejecutar

Desde la raíz del proyecto, en PowerShell:

```powershell
.\ejecutar.ps1
```

Equivale a:

```powershell
javac --release 17 -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src).FullName
java -cp out com.tecnomovil.app.Main
```

Argumentos opcionales: cantidad de usuarios simulados y umbral de ocupación simultánea.

```powershell
java -cp out com.tecnomovil.app.Main 50000 1500
```

## Qué resuelve

| Requerimiento | Método |
| --- | --- |
| a) Afluencia por estación | `AnalizadorTransporte.afluenciaPorEstacion` |
| b) Horas pico | `AnalizadorTransporte.identificarHorasPico` |
| c) Rutas más utilizadas | `AnalizadorTransporte.rutasMasUtilizadas` |
| d) Patrones de viaje | `AnalizadorTransporte.patronesPorUsuario` |
| e) Tiempo promedio entre estaciones | `AnalizadorTransporte.tiempoPromedioEntreEstaciones` |
| f) Sobrecarga de rutas | `AnalizadorTransporte.detectarSobrecarga` |

El analizador no modifica la lista recibida. `Main` es el único borde con entrada/salida. La semilla `42` reproduce siempre el mismo día.

## Informe

Completar `datos_grupo.json` (universidad, docente, integrantes y enlaces) y regenerar el PDF:

```powershell
python herramientas\generar_informe.py
```

Ese script solo arma el documento. El módulo de transporte no depende de él.

## Estructura

```
src/com/tecnomovil/modelo       registros y resultados inmutables
src/com/tecnomovil/analisis     funciones puras de agregación
src/com/tecnomovil/datos        día simulado determinista
src/com/tecnomovil/app          verificación y programa principal
```
