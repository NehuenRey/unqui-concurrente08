/**
 * Monitor que cuenta los StatsWorker que siguen activos.
 * Permite que Main se bloquee hasta que todos los workers hayan terminado.
 */
class WorkerCounter {
    private int workersActivos;

    /**
     * El contador arranca en la cantidad total de workers, antes de iniciarlos,
     * para que esperarFin() no pueda devolver antes de que arranquen.
     */
    public WorkerCounter(int cantidadWorkers) {
        workersActivos = cantidadWorkers;
    }

    /**
     * Lo invoca cada StatsWorker al terminar su ejecución.
     */
    public synchronized void workerTermino() {
        workersActivos--;
        if (workersActivos == 0) {
            notifyAll(); // despierta a Main
        }
    }

    /**
     * Bloquea al thread que la invoca (Main) hasta que no queden workers activos.
     */
    public synchronized void esperarFin() throws InterruptedException {
        while (workersActivos > 0) {
            wait();
        }
    }
}
