/**
 * Tarea especial que, al ejecutarse, hace terminar al StatsWorker que la tomó.
 * Main encola una por cada worker, después de todas las StatsTask.
 */
class PoisonPill extends Task {

    @Override
    public void run() {
        throw new PoisonException();
    }
}
