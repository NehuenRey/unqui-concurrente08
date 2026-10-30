/**
 * Acumula las sumas necesarias para calcular la media y el desvío estándar
 * del IEI de un símbolo, sin tener que guardar cada medición individual.
 *
 * La idea clave es que estas sumas se pueden calcular por partes y después
 * combinar: (sumas del worker A) + (sumas del worker B) = (sumas del total).
 * Eso es lo que permite repartir el trabajo entre threads.
 *
 *   media   = suma / intervalos                                (ecuación 1)
 *   desvío  = sqrt( sumaCuadrados / intervalos - media^2 )     (ecuación 3)
 *
 * NO es un monitor: cada instancia la usa un único thread a la vez
 * (una StatsTask con sus acumuladores locales, o Resultados dentro de
 * sus métodos synchronized).
 */
class Acumulador {

    long archivos = 0;         // cantidad de archivos procesados
    long intervalos = 0;       // cantidad de IEI medidos (N)
    double suma = 0;           // suma de los IEI (Σx)
    double sumaCuadrados = 0;  // suma de los IEI al cuadrado (Σx²)

    /**
     * Suma a este acumulador los valores de otro, campo a campo.
     */
    void sumar(Acumulador otro) {
        archivos += otro.archivos;
        intervalos += otro.intervalos;
        suma += otro.suma;
        sumaCuadrados += otro.sumaCuadrados;
    }

    /**
     * Media poblacional del IEI (ecuación 1). Devuelve 0 si no hay mediciones.
     */
    double media() {
        if (intervalos == 0) return 0;
        return suma / intervalos;
    }

    /**
     * Desvío estándar poblacional del IEI (ecuación 3). Devuelve 0 si no hay mediciones.
     */
    double desvio() {
        if (intervalos == 0) return 0;
        double mu = media();
        double varianza = sumaCuadrados / intervalos - mu * mu;
        // Por redondeo de punto flotante la resta puede dar un negativo
        // minúsculo (ej. -1e-12) cuando todos los IEI son iguales.
        return Math.sqrt(Math.max(0, varianza));
    }

    /**
     * Devuelve una copia independiente de este acumulador.
     */
    Acumulador copia() {
        Acumulador c = new Acumulador();
        c.sumar(this);
        return c;
    }
}
