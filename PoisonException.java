class PoisonException extends RuntimeException {

    public PoisonException() {
        super("PoisonPill: el worker termino su ejecucion");
    }
}
