/**
 * Tarea para detener a los StatsWorker
 */
class PoisonPill extends Task {

    @Override
    public void run() {
        throw new PoisonException();
    }
}
