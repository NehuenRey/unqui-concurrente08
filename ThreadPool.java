class ThreadPool {

    private final int cantidadWorkers;
    private final Buffer buffer;
    private final WorkerCounter contador;

    public ThreadPool(int cantidadWorkers, Buffer buffer, WorkerCounter contador) {
        this.cantidadWorkers = cantidadWorkers;
        this.buffer = buffer;
        this.contador = contador;
    }

    public void iniciar() {
        for (int i = 0; i < cantidadWorkers; i++) {
            new StatsWorker(buffer, contador).start();
        }
    }
}
