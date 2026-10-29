/**
 * Excepción que lanza una PoisonPill para indicarle al StatsWorker que la
 * ejecuta que debe terminar.
 *
 * Hereda de RuntimeException (unchecked) porque Runnable.run() no declara
 * excepciones checked.
 */
class PoisonException extends RuntimeException {

    public PoisonException() {
        super("PoisonPill: el worker termino su ejecucion");
    }
}
