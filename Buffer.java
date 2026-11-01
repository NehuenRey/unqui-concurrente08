class Buffer {

    private final Task[] data;
    private final int N;
    private int inicio = 0;
    private int fin = 0;

    public Buffer(int capacidad) {
        N = capacidad;
        data = new Task[N + 1];
    }

    public synchronized void producir(Task t) throws InterruptedException {
        while (lleno()) {
            wait();
        }
        data[inicio] = t;
        inicio = next(inicio);
        notifyAll();
    }

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
