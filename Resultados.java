/**
 * Monitor que almacena los resultados parciales publicados por las StatsTask,
 * acumulados por simbolo.
 * Es utilizado por cada StatsTask cuando termina de procesar un lote para agregar sus resultados parciales
 */
class Resultados {

    static final String[] PALOS = {"club", "diamond", "heart", "spade"};

    // totales[i] contiene las sumas globales del símbolo PALOS[i]
    private final Acumulador[] totales = new Acumulador[PALOS.length];

    public Resultados() {
        for (int i = 0; i < totales.length; i++) {
            totales[i] = new Acumulador();
        }
    }

    /**
     * Suma a los totales los parciales de un lote. Es synchronized para evitar perdidas
     * de datos
     * @param parciales un Acumulador por simbolo, en el orden de PALOS
     */
    public synchronized void agregar(Acumulador[] parciales) {
        for (int i = 0; i < totales.length; i++) {
            totales[i].sumar(parciales[i]);
        }
    }

    /**
     * Devuelve una copia de los totales del simbolo PALOS[indice].
     * Se devuelve una copia para que nadie pueda modificar los datos del monitor
     */
    public synchronized Acumulador getTotal(int indice) {
        return totales[indice].copia();
    }
}