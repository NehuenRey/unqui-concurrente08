/**
 * Es thread que consume tareas del buffer
 */
class StatsWorker extends Thread {

    private final Buffer buffer;
    private final WorkerCounter contador;

    public StatsWorker(Buffer buffer, WorkerCounter contador) {
        this.buffer = buffer;
        this.contador = contador;
    }

    /**
     * Bucle que consume tareas del buffer hasta que consuma un PoisonPill
     */
    @Override
    public void run() {
        try {
            while (true) {
                Task tarea = buffer.consumir();
                tarea.run();
            }
        } catch (PoisonException e) {
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            //Notifica al WorkerCounter que finalizo
            contador.workerTermino();
        }
    }
}
