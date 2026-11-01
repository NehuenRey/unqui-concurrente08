import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
 
class StatsTask extends Task {

    private final List<File> lote;
    private final Resultados resultados;

    public StatsTask(List<File> lote, Resultados resultados) {
        this.lote = lote;
        this.resultados = resultados;
    }

    @Override
    public void run() {
        Acumulador[] locales = new Acumulador[Resultados.PALOS.length];
        for (int i = 0; i < locales.length; i++) {
            locales[i] = new Acumulador();
        }

        for (File archivo : lote) {
            int palo = indiceDelPalo(archivo.getName());
            try {
                locales[palo].sumar(procesarArchivo(archivo));
            } catch (IOException e) {
                throw new UncheckedIOException("Error leyendo " + archivo, e);
            }
        }

        resultados.agregar(locales);
    }

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

    
    private long leerTimestamp(String linea) {
        return Long.parseLong(linea.substring(0, linea.indexOf(',')));
    }

    private int indiceDelPalo(String nombreArchivo) {
        for (int i = 0; i < Resultados.PALOS.length; i++) {
            if (nombreArchivo.startsWith("x" + Resultados.PALOS[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Archivo con palo desconocido: " + nombreArchivo);
    }
}
