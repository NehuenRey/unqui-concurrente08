import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Tarea que calcula las estadísticas parciales del IEI de un lote de archivos.
 *
 * Cada archivo se procesa por separado (los IEI nunca mezclan archivos) y su
 * resultado se suma al acumulador LOCAL del palo correspondiente. Al terminar
 * el lote, la tarea publica sus 4 acumuladores en el monitor Resultados con
 * una única llamada.
 */
class StatsTask extends Task {

    private final List<File> lote;         // archivos que procesa esta tarea
    private final Resultados resultados;   // monitor compartido por todas las tareas

    public StatsTask(List<File> lote, Resultados resultados) {
        this.lote = lote;
        this.resultados = resultados;
    }

    /**
     * Procesa todos los archivos del lote y publica los parciales.
     * Lo ejecuta un StatsWorker al tomar esta tarea del Buffer.
     */
    @Override
    public void run() {
        // Un acumulador local por palo, en el orden de Resultados.PALOS.
        // Son locales a esta tarea: ningún otro thread los ve, así que se
        // pueden modificar sin sincronización.
        Acumulador[] locales = new Acumulador[Resultados.PALOS.length];
        for (int i = 0; i < locales.length; i++) {
            locales[i] = new Acumulador();
        }

        for (File archivo : lote) {
            int palo = indiceDelPalo(archivo.getName());
            try {
                locales[palo].sumar(procesarArchivo(archivo));
            } catch (IOException e) {
                // run() no puede declarar excepciones checked (Runnable no lo
                // permite), así que se envuelve en una unchecked.
                throw new UncheckedIOException("Error leyendo " + archivo, e);
            }
        }

        // Única entrada al monitor por lote.
        resultados.agregar(locales);
    }

    /**
     * Lee un archivo .csv y calcula sus IEI: la diferencia de timestamp entre
     * cada línea y la anterior. Un archivo con n eventos produce n-1 IEI.
     *
     * @return un Acumulador con archivos = 1 y las sumas de ese archivo
     */
    private Acumulador procesarArchivo(File archivo) throws IOException {
        Acumulador acc = new Acumulador();
        acc.archivos = 1;

        // try-with-resources: el archivo se cierra solo al salir del bloque
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            reader.readLine(); // saltea el encabezado "timestamp,x,y,polarity"

            long anterior = leerTimestamp(reader.readLine()); // primer evento
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isEmpty()) continue; // por si hay una línea vacía al final

                long actual = leerTimestamp(linea);
                long iei = actual - anterior; // puede ser 0, y se cuenta igual

                acc.intervalos++;
                acc.suma += iei;
                acc.sumaCuadrados += (double) iei * iei;

                anterior = actual;
            }
        }
        return acc;
    }

    /**
     * Devuelve el timestamp de una línea "timestamp,x,y,polarity",
     * es decir, el número que está antes de la primera coma.
     */
    private long leerTimestamp(String linea) {
        return Long.parseLong(linea.substring(0, linea.indexOf(',')));
    }

    /**
     * Devuelve el índice del palo en Resultados.PALOS a partir del nombre
     * del archivo, que tiene la forma "x<palo><número>[i].csv"
     * (ej. "xclub02i.csv" → club).
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
