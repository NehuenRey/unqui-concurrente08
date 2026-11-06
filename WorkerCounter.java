/**
 * Es un monitor que controla la finalizacion de los StatsWorker
 */
class WorkerCounter {
    private int workersActivos;

    /**
     * Inicia el contador con la cantidad de StatsWorker creados
     * @param cantidadWorkers cantidad de StatsWorkers creados
     */
    public WorkerCounter(int cantidadWorkers) {
        workersActivos = cantidadWorkers;
    }

    /**
     * Decrementa el contador de Workers activos. Si llega a cero ejecuta notifyAll para despertar a Main
     */
    public synchronized void workerTermino() {
        workersActivos--;
        if (workersActivos == 0) {
            notifyAll();
        }
    }

    /**
     * Bloquea a Main hasta que todos los StasWorker hayan terminado
     * @throws InterruptedException Si la espera es interrumpida
     */
    public synchronized void esperarFin() throws InterruptedException {
        while (workersActivos > 0) {
            wait();
        }
    }
}
