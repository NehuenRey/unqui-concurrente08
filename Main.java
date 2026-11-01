import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


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

        long fin = System.nanoTime();

        System.out.printf(Locale.US, "Archivos: %d | Lotes: %d | Workers: %d | Buffer: %d%n",
                contarArchivos(lotes), lotes.size(), cantidadWorkers, tamañoBuffer);
        System.out.printf(Locale.US, "Tiempo de procesamiento: %.3f ms%n", (fin - inicio) / 1_000_000.0);

        escribirResultados(totales, medias, desvios);
        System.out.println("Resultados escritos en " + ARCHIVO_SALIDA);
    }

    private static File[] listarCsv(File directorio) {
        File[] archivos = directorio.listFiles((dir, nombre) -> nombre.toLowerCase().endsWith(".csv"));
        if (archivos == null) {
            throw new IllegalArgumentException("No es un directorio válido: " + directorio);
        }
        Arrays.sort(archivos);
        return archivos;
    }

    private static List<List<File>> armarLotes(File[] archivos, int tamañoLote) {
        List<List<File>> lotes = new ArrayList<>();
        for (int desde = 0; desde < archivos.length; desde += tamañoLote) {
            int hasta = Math.min(desde + tamañoLote, archivos.length);
            lotes.add(Arrays.asList(Arrays.copyOfRange(archivos, desde, hasta)));
        }
        return lotes;
    }

    private static int contarArchivos(List<List<File>> lotes) {
        int total = 0;
        for (List<File> lote : lotes) {
            total += lote.size();
        }
        return total;
    }

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
