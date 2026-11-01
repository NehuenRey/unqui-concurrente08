import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Clase principal.
 *
 * 1) Lee los parametros de entrada (directorio, tamaño de buffer, cantidad de StatsWorker, tamaño de lote)
 * 2) Agrupa los archivos en lotes segun el tamaño de lote pasado por parametro
 * 3) Instancia a el Buffer, Resultados, WorkerCounter y ThreadPool
 * 4) Le da la orden a ThreadPool para que cree los StatsWorker
 * 5) Crea las StatsTaks correspondientes
 * 6) Crea los PoisonPill para cada StatsWorker
 * 7) Espera hasta que todos los StatsWorker finalicen sus tareas
 * 8) Calcula las medias y desvios globales y escribe el reporte
 */
public class Main {

    private static final String ARCHIVO_SALIDA = "resultados.csv";

    public static void main(String[] args) throws InterruptedException, IOException {
        
        File directorio = new File(args[0]);
        int tamañoBuffer = Integer.parseInt(args[1]);
        int cantidadWorkers = Integer.parseInt(args[2]);
        int tamañoLote = Integer.parseInt(args[3]);

        if (tamañoBuffer < 1 || cantidadWorkers < 1 || tamañoLote < 1) {
            System.err.println("El tamaño del buffer, la cantidad de workers y el tamaño de lote deben ser mayores a 0.");
            System.exit(1);
        }

        List<List<File>> lotes = armarLotes(listarCsv(directorio), tamañoLote);
        Buffer buffer = new Buffer(tamañoBuffer);
        Resultados resultados = new Resultados();
        WorkerCounter contador = new WorkerCounter(cantidadWorkers);
        ThreadPool pool = new ThreadPool(cantidadWorkers, buffer, contador);

        //inicia la toma de tiempo del procesamiento
        long inicio = System.nanoTime();

        pool.iniciar();

        for (List<File> lote : lotes) {
            buffer.producir(new StatsTask(lote, resultados));
        }

        for (int i = 0; i < cantidadWorkers; i++) {
            buffer.producir(new PoisonPill());
        }

        contador.esperarFin();

        int cantPalos = Resultados.PALOS.length;
        Acumulador[] totales = new Acumulador[cantPalos];
        double[] medias = new double[cantPalos];
        double[] desvios = new double[cantPalos];
        for (int i = 0; i < cantPalos; i++) {
            totales[i] = resultados.getTotal(i);
            medias[i] = totales[i].media();
            desvios[i] = totales[i].desvio();
        }
        //finaliza la toma de tiempo del procesamiento
        long fin = System.nanoTime();

        System.out.printf(Locale.US, "Archivos: %d | Lotes: %d | Workers: %d | Buffer: %d%n",
                contarArchivos(lotes), lotes.size(), cantidadWorkers, tamañoBuffer);
        System.out.printf(Locale.US, "Tiempo de procesamiento: %.3f ms%n", (fin - inicio) / 1_000_000.0);

        escribirResultados(totales, medias, desvios);
        System.out.println("Resultados escritos en " + ARCHIVO_SALIDA);
    }

    /** Filtra y ordena los achivos del directorio alfabeticamente*/
    private static File[] listarCsv(File directorio) {
        File[] archivos = directorio.listFiles((dir, nombre) -> nombre.toLowerCase().endsWith(".csv"));
        if (archivos == null) {
            throw new IllegalArgumentException("No es un directorio válido: " + directorio);
        }
        Arrays.sort(archivos);
        return archivos;
    }

    /** Divide los archivos en lotes */
    private static List<List<File>> armarLotes(File[] archivos, int tamañoLote) {
        List<List<File>> lotes = new ArrayList<>();
        for (int desde = 0; desde < archivos.length; desde += tamañoLote) {
            int hasta = Math.min(desde + tamañoLote, archivos.length);
            lotes.add(Arrays.asList(Arrays.copyOfRange(archivos, desde, hasta)));
        }
        return lotes;
    }

    /** Cantidad total de archivos*/
    private static int contarArchivos(List<List<File>> lotes) {
        int total = 0;
        for (List<File> lote : lotes) {
            total += lote.size();
        }
        return total;
    }

    /** Escribre el reporte en el archivo resultados.csv, dandele a la media y al devio 6 digitos de decimales*/
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
