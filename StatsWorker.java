/**
 * Thread que, mientras esté activo, toma tareas del Buffer y las ejecuta.
 * Termina cuando toma una PoisonPill, y al terminar se lo informa al WorkerCounter.
 */
class StatsWorker extends Thread {

    private final Buffer buffer;
    private final WorkerCounter contador;

    public StatsWorker(Buffer buffer, WorkerCounter contador) {
        this.buffer = buffer;
        this.contador = contador;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Task tarea = buffer.consumir();
                tarea.run();
            }
        } catch (PoisonException e) {
            // Tomó una PoisonPill: fin normal del worker.
        } catch (InterruptedException e) {
            // Interrumpido mientras esperaba en el buffer: termina también.
            Thread.currentThread().interrupt();
        } finally {
            // Se ejecuta siempre (incluso ante un error inesperado en una tarea),
            // así Main nunca queda esperando a un worker que ya no existe.
            contador.workerTermino();
        }
    }
}
