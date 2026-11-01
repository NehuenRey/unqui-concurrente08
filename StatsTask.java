import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Es una tarea que procesa un lote de archivos.
 */
class StatsTask extends Task {

    private final List<File> lote;
    private final Resultados resultados;

    /**
     * Se intancia para procesar un lote de archivos especifico
     * @param lote lista de archivos a procesar
     * @param resultados Monitor donde se agregan los resultados del procesamiento del lote
     */
    public StatsTask(List<File> lote, Resultados resultados) {
        this.lote = lote;
        this.resultados = resultados;
    }

    @Override
    public void run() {
        // crea un listado local de acumuladores
        Acumulador[] locales = new Acumulador[Resultados.PALOS.length];
        for (int i = 0; i < locales.length; i++) {
            locales[i] = new Acumulador();
        }

        // procesa cada archivo del lote
        for (File archivo : lote) {
            int palo = indiceDelPalo(archivo.getName());
            try {
                locales[palo].sumar(procesarArchivo(archivo));
            } catch (IOException e) {
                throw new UncheckedIOException("Error leyendo " + archivo, e);
            }
        }
        // agrega los resultados del procesamiento del lote
        resultados.agregar(locales);
    }

    /**
     * Lee un archivo linea por linea y calcula las metricas del IEI
     * @param archivo archivo a procesar
     * @return Acumulador con los datos obtenidos del archivo
     * @throws IOException Si ocurre un fallo durante la lectura del disco
     */
    private Acumulador procesarArchivo(File archivo) throws IOException {
        Acumulador acc = new Acumulador();
        acc.archivos = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            reader.readLine();

            long anterior = leerTimestamp(reader.readLine());
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isEmpty()) continue;

                long actual = leerTimestamp(linea);
                long iei = actual - anterior;

                acc.intervalos++;
                acc.suma += iei;
                acc.sumaCuadrados += (double) iei * iei;

                anterior = actual;
            }
        }
        return acc;
    }

    /**
     * Parsea el timestamp
     * @param linea texto contenido de la linea
     * @return el valor del timestamp
     */
    private long leerTimestamp(String linea) {
        return Long.parseLong(linea.substring(0, linea.indexOf(',')));
    }

    /**
     * Determina cual es el palo del archivo y devuelve el indice correspondiente
     * @param nombreArchivo nombre del archivo csv
     * @return el indice numerico del palo
     */
    private int indiceDelPalo(String nombreArchivo) {
        for (int i = 0; i < Resultados.PALOS.length; i++) {
            if (nombreArchivo.startsWith("x" + Resultados.PALOS[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Archivo con palo desconocido: " + nombreArchivo);
    }
}
