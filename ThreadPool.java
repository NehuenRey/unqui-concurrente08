/**
 * Es el encargado de instanciar e iniciar a los StatsWorker
 */
class ThreadPool {

    private final int cantidadWorkers;
    private final Buffer buffer;
    private final WorkerCounter contador;

    /**
     * Es instanciado con la cantidad de StatsWorker a crear y los datos de requieren ellos
     * @param cantidadWorkers cantidad de StatsWorker a crear
     * @param buffer donde se almacenan las Task a ser consumidas
     * @param contador Monitor que controla la finalizacion de los StastWorker
     */
    public ThreadPool(int cantidadWorkers, Buffer buffer, WorkerCounter contador) {
        this.cantidadWorkers = cantidadWorkers;
        this.buffer = buffer;
        this.contador = contador;
    }

    /** Incia e inicia los StatsWorker*/
    public void iniciar() {
        for (int i = 0; i < cantidadWorkers; i++) {
            new StatsWorker(buffer, contador).start();
        }
    }
}
