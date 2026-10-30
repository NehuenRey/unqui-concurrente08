/**
 * Monitor que almacena los resultados parciales publicados por las StatsTask,
 * acumulados por símbolo.
 *
 * Funcionamiento:
 *  - Cada StatsTask procesa su lote de archivos acumulando en variables
 *    LOCALES (fuera del monitor, en paralelo con las demás tareas).
 *  - Al terminar el lote, entra UNA sola vez al monitor con agregar(...)
 *    para sumar sus parciales a los totales.
 *  - Main lee los totales con getTotal(...) una vez que terminaron todos
 *    los workers.
 *
 * Entrar una sola vez por lote (y no por cada IEI) minimiza el tiempo que
 * los workers pasan esperando el lock: lo que se ejecuta dentro del monitor
 * no es concurrente.
 */
class Resultados {

    /**
     * Orden fijo de los símbolos. El índice de cada uno en este array es el
     * que se usa en todos los arrays de Acumulador (aquí y en StatsTask), y
     * también el orden de las filas en resultados.csv.
     */
    static final String[] PALOS = {"club", "diamond", "heart", "spade"};

    // totales[i] contiene las sumas globales del símbolo PALOS[i]
    private final Acumulador[] totales = new Acumulador[PALOS.length];

    public Resultados() {
        for (int i = 0; i < totales.length; i++) {
            totales[i] = new Acumulador();
        }
    }

    /**
     * Suma a los totales los parciales de un lote. Lo invoca cada StatsTask
     * al terminar. Es synchronized porque varios workers pueden terminar su
     * lote al mismo tiempo, y "+=" no es atómico (leer, sumar, escribir):
     * sin exclusión mutua dos parciales podrían pisarse y perderse uno.
     *
     * @param parciales un Acumulador por símbolo, en el orden de PALOS
     */
    public synchronized void agregar(Acumulador[] parciales) {
        for (int i = 0; i < totales.length; i++) {
            totales[i].sumar(parciales[i]);
        }
    }

    /**
     * Devuelve una copia de los totales del símbolo PALOS[indice].
     * Se devuelve una copia para que nadie pueda modificar el estado interno
     * del monitor por fuera de sus métodos synchronized.
     */
    public synchronized Acumulador getTotal(int indice) {
        return totales[indice].copia();
    }
}
