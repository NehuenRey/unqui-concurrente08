/**
 *Es un monitor que actua como una cola FIFO acotada que Almacena instancias de Task para ser consumidas
 */
class Buffer {

    private final Task[] data;
    private final int N;
    private int inicio = 0;
    private int fin = 0;

    public Buffer(int capacidad) {
        N = capacidad;
        data = new Task[N + 1];
    }

    /**
     * Deposita una Task en el buffer. En caso de que el buffer este lleno el thread se bloquea hasta que haya espacio disponible
     * @param t La Task a depositar
     * @throws InterruptedException si el thread es interrumpido mientras espera
     */
    public synchronized void producir(Task t) throws InterruptedException {
        while (lleno()) {
            wait();
        }
        data[inicio] = t;
        inicio = next(inicio);
        notifyAll();
    }

    /**
     * Extrae y retorna la siguiente Task del buffer. En caso de que el buffer este vacio el thread se bloquea.
     * @return La Task consumida
     * @throws InterruptedException si el thread es interrumpido mientras espera
     */
    public synchronized Task consumir() throws InterruptedException {
        while (vacio()) {
            wait();
        }
        Task result = data[fin];
        data[fin] = null;
        fin = next(fin);
        notifyAll();
        return result;
    }

    /** Verifica si el buffer no contiene ninguna Task*/
    private boolean vacio() {
        return inicio == fin;
    }

    /** Verifica si el buffer esta en su maxima capacidad */
    private boolean lleno() {
        return next(inicio) == fin;
    }

    /** Calcula la siguiente posicion y la devuelve  */
    private int next(int i) {
        return (i + 1) % (N + 1);
    }
}
