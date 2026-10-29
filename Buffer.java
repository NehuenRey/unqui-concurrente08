/**
 * Monitor que implementa una cola FIFO concurrente de capacidad acotada.
 * Bloquea a quien intenta sacar un elemento cuando está vacía, y a quien
 * intenta agregar un elemento cuando está llena.
 *
 * Se implementa como un buffer circular de N+1 posiciones: una posición
 * queda siempre libre para poder distinguir "vacío" (inicio == fin)
 * de "lleno" (next(inicio) == fin).
 */
class Buffer {

    private final Task[] data;
    private final int N;
    private int inicio = 0; // próxima posición donde se escribe
    private int fin = 0;    // próxima posición de donde se lee

    public Buffer(int capacidad) {
        N = capacidad;
        data = new Task[N + 1];
    }

    /**
     * Agrega una tarea al final de la cola. Si está llena, espera hasta que haya lugar.
     */
    public synchronized void producir(Task t) throws InterruptedException {
        while (lleno()) {
            wait();
        }
        data[inicio] = t;
        inicio = next(inicio);
        notifyAll(); // puede haber consumidores esperando porque estaba vacía
    }

    /**
     * Saca la tarea del principio de la cola. Si está vacía, espera hasta que haya una.
     */
    public synchronized Task consumir() throws InterruptedException {
        while (vacio()) {
            wait();
        }
        Task result = data[fin];
        data[fin] = null; // libera la referencia para el garbage collector
        fin = next(fin);
        notifyAll(); // puede haber productores esperando porque estaba llena
        return result;
    }

    private boolean vacio() {
        return inicio == fin;
    }

    private boolean lleno() {
        return next(inicio) == fin;
    }

    private int next(int i) {
        return (i + 1) % (N + 1);
    }
}
