/**
 * Excepcion lanzada al ser ejecutada una PoisonPill
 */
class PoisonException extends RuntimeException {

    public PoisonException() {
        super("PoisonPill: el worker termino su ejecucion");
    }
}
