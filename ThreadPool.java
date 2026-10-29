/**
 * Se encarga de instanciar e iniciar los StatsWorker, todos compartiendo el
 * mismo Buffer de tareas. Las tareas y las PoisonPills las encola Main
 * directamente en el Buffer.
 */
class ThreadPool {

    private final int cantidadWorkers;
    private final Buffer buffer;
    private final WorkerCounter contador;

    public ThreadPool(int cantidadWorkers, Buffer buffer, WorkerCounter contador) {
        this.cantidadWorkers = cantidadWorkers;
        this.buffer = buffer;
        this.contador = contador;
    }

    /**
     * Crea e inicia los workers. El WorkerCounter ya debe estar inicializado
     * con la misma cantidad de workers.
     */
    public void iniciar() {
        for (int i = 0; i < cantidadWorkers; i++) {
            new StatsWorker(buffer, contador).start();
        }
    }
}
