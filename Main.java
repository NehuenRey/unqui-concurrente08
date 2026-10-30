import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Punto de entrada del programa. Calcula la media y el desvío estándar del
 * inter-event interval (IEI) por símbolo sobre los archivos .csv de PokerDVS.
 *
 * Uso:
 *   java Main <directorio_entrada> <tamaño_buffer> <cantidad_workers> <tamaño_lote>
 *
 * Ejemplo:
 *   java Main ../poker_csv/poker_csv 16 8 32
 *
 * Flujo:
 *  1. Lee los argumentos y lista los .csv del directorio.
 *  2. Divide los archivos en lotes de tamaño fijo (el último puede ser menor).
 *  3. Inicia los workers (ThreadPool) y encola en el Buffer una StatsTask por
 *     lote, seguida de una PoisonPill por worker.
 *  4. Espera a que terminen todos los workers (WorkerCounter).
 *  5. Calcula los valores finales a partir de los totales de Resultados.
 *  6. Informa el tiempo transcurrido y escribe resultados.csv.
 */
public class Main {

    private static final String ARCHIVO_SALIDA = "resultados.csv";

    /**
     * main declara InterruptedException (de Buffer.producir y
     * WorkerCounter.esperarFin) e IOException (de la escritura del CSV):
     * si ocurren, el programa termina informando el error.
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // ---- 1. Lectura y validación de los parámetros ----
        if (args.length != 4) {
            System.err.println("Uso: java Main <directorio_entrada> <tamaño_buffer> <cantidad_workers> <tamaño_lote>");
            System.exit(1);
        }

        File directorio = new File(args[0]);
        int tamanoBuffer = Integer.parseInt(args[1]);
        int cantidadWorkers = Integer.parseInt(args[2]);
        int tamanoLote = Integer.parseInt(args[3]);

        if (tamanoBuffer < 1 || cantidadWorkers < 1 || tamanoLote < 1) {
            System.err.println("El tamaño del buffer, la cantidad de workers y el tamaño de lote deben ser mayores a 0.");
            System.exit(1);
        }

        // ---- 2. División del trabajo en lotes (antes de medir el tiempo) ----
        List<List<File>> lotes = armarLotes(listarCsv(directorio), tamanoLote);

        // ---- 3. Estructuras compartidas: una sola instancia de cada una ----
        // Buffer: cola de tareas entre Main (productor) y los workers (consumidores)
        // Resultados: donde cada StatsTask publica sus parciales
        // WorkerCounter: arranca en cantidadWorkers, permite a Main esperar el fin
        // ThreadPool: crea e inicia los workers sobre el mismo Buffer
        Buffer buffer = new Buffer(tamanoBuffer);
        Resultados resultados = new Resultados();
        WorkerCounter contador = new WorkerCounter(cantidadWorkers);
        ThreadPool pool = new ThreadPool(cantidadWorkers, buffer, contador);

        // ---- Inicio de la medición: justo antes de iniciar los workers ----
        long inicio = System.nanoTime();

        // start() de cada worker: arrancan y quedan bloqueados en
        // buffer.consumir() hasta que Main encole la primera tarea
        pool.iniciar();

        // ---- 4. Producción de tareas ----
        // Una tarea por lote. Si el buffer se llena, producir() bloquea a Main
        // hasta que algún worker libere lugar.
        for (List<File> lote : lotes) {
            buffer.producir(new StatsTask(lote, resultados));
        }

        // Una PoisonPill por worker, después de todas las tareas: como el
        // buffer es FIFO, ningún worker termina mientras quede trabajo.
        for (int i = 0; i < cantidadWorkers; i++) {
            buffer.producir(new PoisonPill());
        }

        // ---- 5. Espera y cálculo final ----
        // Bloquea hasta que todos los workers hayan terminado. A partir de acá
        // ningún worker escribe más en Resultados: los totales son definitivos.
        contador.esperarFin();

        // Valores finales por símbolo: μ = Σx/N y σ = sqrt(Σx²/N − μ²)
        int cantPalos = Resultados.PALOS.length;
        Acumulador[] totales = new Acumulador[cantPalos];
        double[] medias = new double[cantPalos];
        double[] desvios = new double[cantPalos];
        for (int i = 0; i < cantPalos; i++) {
            totales[i] = resultados.getTotal(i);
            medias[i] = totales[i].media();
            desvios[i] = totales[i].desvio();
        }

        long fin = System.nanoTime();
        // ---- Fin de la medición (no incluye la escritura del archivo) ----

        // ---- 6. Informe del tiempo y escritura de resultados.csv ----
        System.out.printf(Locale.US, "Archivos: %d | Lotes: %d | Workers: %d | Buffer: %d%n",
                contarArchivos(lotes), lotes.size(), cantidadWorkers, tamanoBuffer);
        System.out.printf(Locale.US, "Tiempo de procesamiento: %.3f ms%n", (fin - inicio) / 1_000_000.0);

        escribirResultados(totales, medias, desvios);
        System.out.println("Resultados escritos en " + ARCHIVO_SALIDA);
    }

    /**
     * Devuelve los archivos .csv del directorio, ordenados por nombre.
     * listFiles() no garantiza un orden, y ordenar hace que los lotes sean
     * siempre los mismos entre corridas (mediciones de tiempo comparables).
     */
    private static File[] listarCsv(File directorio) {
        File[] archivos = directorio.listFiles((dir, nombre) -> nombre.toLowerCase().endsWith(".csv"));
        if (archivos == null) {
            throw new IllegalArgumentException("No es un directorio válido: " + directorio);
        }
        Arrays.sort(archivos);
        return archivos;
    }

    /**
     * Divide los archivos en lotes consecutivos de tamaño tamanoLote.
     * Ej: 131 archivos con lotes de 32 → 4 lotes de 32 y 1 lote de 3.
     */
    private static List<List<File>> armarLotes(File[] archivos, int tamanoLote) {
        List<List<File>> lotes = new ArrayList<>();
        for (int desde = 0; desde < archivos.length; desde += tamanoLote) {
            int hasta = Math.min(desde + tamanoLote, archivos.length);
            lotes.add(Arrays.asList(Arrays.copyOfRange(archivos, desde, hasta)));
        }
        return lotes;
    }

    /**
     * Cantidad total de archivos repartidos entre todos los lotes.
     */
    private static int contarArchivos(List<List<File>> lotes) {
        int total = 0;
        for (List<File> lote : lotes) {
            total += lote.size();
        }
        return total;
    }

    /**
     * Escribe resultados.csv con una línea por símbolo, en el orden de
     * Resultados.PALOS. Se usa Locale.US para que el separador decimal sea
     * "." (con el locale de Argentina saldría "," y rompería el CSV).
     */
    private static void escribirResultados(Acumulador[] totales, double[] medias, double[] desvios)
            throws IOException {
        try (PrintWriter out = new PrintWriter(ARCHIVO_SALIDA)) {
            out.println("clase,archivos,intervalos,media_us,std_us");
            for (int i = 0; i < Resultados.PALOS.length; i++) {
                out.printf(Locale.US, "%s,%d,%d,%.6f,%.6f%n",
                        Resultados.PALOS[i], totales[i].archivos, totales[i].intervalos,
                        medias[i], desvios[i]);
            }
        }
    }
}
