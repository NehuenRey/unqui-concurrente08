# TP Programación Concurrente 2026 — Estadísticas IEI sobre PokerDVS

## 1. El problema

- **Cámaras de eventos**: cada evento es `(t, x, y, p)` → timestamp en µs, posición del píxel, polaridad.
- **Dataset PokerDVS**: archivos `.csv` separados por símbolo, con formato `timestamp,x,y,polarity` y una línea de encabezado.
  - `club` (trébol): 30 archivos · `diamond`: 43 · `heart`: 23 · `spade`: 35 → **131 archivos**.
  - Algunos nombres terminan en `i` (símbolo invertido): **se ignora**, cuenta igual que su clase.
- **Qué calcular**: media y desvío estándar **poblacional** del **IEI** (inter-event interval) por símbolo.

### Reglas del IEI
- Se calcula **dentro de cada archivo**, entre líneas consecutivas: `IEI_i = t_{i+1} - t_i`.
- Nunca se combinan timestamps de archivos distintos.
- Archivo con `n` eventos → `n - 1` intervalos.
- Los IEI = 0 **se cuentan**.
- Se garantiza encabezado + al menos 2 eventos por archivo.

### Fórmulas (lo que hace posible paralelizar)
Cada worker solo necesita acumular, por símbolo:

| acumulador | tipo |
|---|---|
| `archivos` (cantidad) | `long` |
| `N` (cantidad de intervalos) | `long` |
| `Σx` (suma de IEI) | `double` |
| `Σx²` (suma de cuadrados) | `double` |

Al final, combinando todos los parciales:

```
μ = Σx / N
σ = sqrt( Σx² / N  -  μ² )
```

Los parciales se **suman** (no se promedian medias): por eso no importa que cada lote tenga distinta cantidad de archivos.

### Dataset real (verificado)
- 131 archivos sueltos en un solo directorio (sin subcarpetas), ~333k líneas en total.
- Nombres: `x<palo><nn>[i].csv` → ej. `xclub01.csv`, `xclub02i.csv`, `xdiamond23i.csv`. El palo se saca del prefijo `x<palo>`.
- Encabezado `timestamp,x,y,polarity`, timestamps enteros (µs) y crecientes, fin de línea LF.
- Tamaño por archivo muy desigual: de ~370 líneas (`xclub22i`) a ~5400 (`xspade02`).
- **Valores esperados** (calculados con awk sobre el dataset completo, sirven para validar):

```
clase,archivos,intervalos,media_us,std_us
club,30,68615,5.836013,11.557427
diamond,43,97473,7.580930,12.723686
heart,23,56085,4.603869,13.593044
spade,35,110406,6.149847,14.042695
```

## 2. Salida: `resultados.csv`

```
clase,archivos,intervalos,media_us,std_us
club,30,...,x.xxxxxx,x.xxxxxx
diamond,43,...
heart,23,...
spade,35,...
```
- Números finales con **6 decimales** (ojo con `Locale`: usar `Locale.US` para que salga `.` y no `,`).
- Valores de referencia aproximados (figura 3): media ≈ club 6, diamond 7.5, heart 4.7, spade 6.2; std ≈ 11.5, 12.7, 13.5, 13.9.

## 3. Ejecución

```
java Main <directorio_entrada> <tamaño_buffer> <cant_workers> <tamaño_lote>
```
- Medir tiempo con `System.nanoTime()`: **justo antes de iniciar los workers** hasta **que Main tiene el resultado final** (sin incluir la escritura del archivo).
- Sin mensajes de debug en la ejecución por defecto.

## 4. Clases obligatorias

| Clase | Rol | Notas |
|---|---|---|
| `Main` | Entrada. Lee el directorio, arma los lotes, crea Buffer/ThreadPool/WorkerCounter, encola `StatsTask`s y luego `PoisonPill`s, espera, combina parciales, escribe CSV, informa tiempo. | Solo termina cuando se generó el archivo. |
| `Buffer` | Cola FIFO **acotada**, monitor con `synchronized` + `wait/notifyAll`. `push` bloquea si está llena, `pop` bloquea si está vacía. | Igual al ej. 8 de la práctica 6. Capacidad configurable. |
| `WorkerCounter` | Monitor que hace que `Main` espere hasta que todos los workers terminen. | Ej: contador de workers vivos; `workerTermino()` decrementa y notifica; `esperarFin()` hace `while (vivos > 0) wait()`. Similar a la barrera (ej. 4). |
| `Task` (abstracta) | `implements Runnable`. Superclase de lo que va al buffer. | |
| `StatsTask extends Task` | `run()` procesa su lote de archivos: lee cada CSV, calcula IEIs y acumula por símbolo; publica el parcial en la estructura compartida. | Calcular localmente y publicar **una vez** por lote (minimiza contención). |
| `StatsWorker extends Thread` | Loop: `buffer.pop()` → `task.run()`. Termina al tomar una `PoisonPill`. Al salir avisa al `WorkerCounter`. | Ej. 11 de la práctica. |
| `PoisonPill extends Task` | `run()` lanza `PoisonException` → el worker sale del loop. | Main encola **una por worker**, después de todas las tasks. |
| `ThreadPool` | Instancia e inicia los N `StatsWorker` con el mismo buffer. | Puede exponer `launch(task)` y `stop()` como en el ej. 11. |
| Otras (sugeridas) | `PoisonException`, `Resultados` / `Acumulador` (monitor con los parciales por símbolo), `StatsParciales` (struct de los 4 acumuladores). | |

### Restricción importante
**No se pueden usar** clases de `java.util.concurrent` (ni `ConcurrentHashMap`, `BlockingQueue`, `AtomicLong`, `CountDownLatch`, etc.). Todo con monitores propios (`synchronized`, `wait`, `notifyAll`). Usar `double` / `long`.

## 5. Flujo general

```
Main
 ├─ listar .csv del directorio, detectar símbolo por nombre de archivo
 ├─ partir la lista en lotes de tamaño L
 ├─ t0 = nanoTime()
 ├─ ThreadPool arranca W StatsWorkers (comparten Buffer)
 ├─ for lote: buffer.push(new StatsTask(lote, resultados))
 ├─ for i in 1..W: buffer.push(new PoisonPill())
 ├─ workerCounter.esperarFin()
 ├─ combinar parciales → μ, σ por clase
 ├─ t1 = nanoTime(); imprimir (t1 - t0)
 └─ escribir resultados.csv

StatsWorker (x W)
 loop:
   task = buffer.pop()
   task.run()          // StatsTask: procesa lote y publica parcial
                       // PoisonPill: lanza PoisonException → salir
 finally: workerCounter.workerTermino()
```

## 6. Informe (PDF, obligatorio)

1. **Autoría**: nombre, e-mail, legajo/DNI (3 personas).
2. **Introducción**: dominio, diseño, clases y cómo interactúan, cómo se reparten los archivos, dónde viven los parciales, cómo se sincronizan; instrucciones de uso.
3. **Evaluación**: hardware (CPU, RAM, SO), **hipótesis**, tabla de tiempos y gráfico tiempo vs. threads (una curva por tamaño de lote).
   - Lotes: **1, 16, 32, 64, 130** · Threads: **1, 4, 8, 16** · buffer fijo.
   - Recomendado: muchas corridas por combinación, descartar las primeras 10, promediar. Documentar el procesamiento.
4. **Análisis**: mínimo y máximo por tamaño, ¿más threads siempre mejora?, ¿cambia con el lote?, por qué; contrastar con la hipótesis.
- Incluir los dos gráficos de barras (media e std por símbolo) replicando la figura 3.

## 7. Entrega

- `.zip` con código + archivos usados + PDF, por Discord (o repo git **privado** con los profes agregados).
- **Fecha**: antes del **domingo 8/11/2026 23:59**. Reentrega: 29/11. Coloquio: 14/11 y 18/11 (25 min por grupo).
- ⚠️ El código debe ser **original**: si se detecta que fue enteramente generado por un LLM, se desaprueba sin reentrega. Además en el coloquio hay que explicarlo → el código lo escribimos nosotros, Claude guía y revisa.

## 8. Plan de desarrollo sugerido (orden)

1. `Buffer` (reutilizar el del ej. 8) — probar con un productor/consumidor simple.
2. `Task`, `PoisonPill`, `PoisonException`.
3. `StatsWorker` + `ThreadPool` + `WorkerCounter` — probar con tareas dummy (ej. 11).
4. Lógica secuencial de un archivo: leer CSV y calcular `N, Σx, Σx²` — validar con CSVs chicos hechos a mano.
5. Estructura compartida de resultados (monitor) + `StatsTask`.
6. `Main`: args, listado del directorio, partición en lotes, medición de tiempo, escritura de `resultados.csv`.
7. Verificar contra la figura 3 y contra una corrida con 1 thread (los resultados deben ser idénticos para cualquier combinación de threads/lote, salvo redondeo mínimo de `double`).
8. Script de benchmark + gráficos + informe.
