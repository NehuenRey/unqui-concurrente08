class WorkerCounter {
    private int workersActivos;

    public WorkerCounter(int cantidadWorkers) {
        workersActivos = cantidadWorkers;
    }

    public synchronized void workerTermino() {
        workersActivos--;
        if (workersActivos == 0) {
            notifyAll();
        }
    }

    public synchronized void esperarFin() throws InterruptedException {
        while (workersActivos > 0) {
            wait();
        }
    }
}
