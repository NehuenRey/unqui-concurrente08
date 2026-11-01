/**
 * Guarda y acumula las estadisticas de los calculos de IEI
 * de un simbolo determinado.
 * datos: cantidad de archivos, cantidad de intervalos, sumas para calcular la media y el devio estandar
 */
class Acumulador {

    long archivos = 0;
    long intervalos = 0;
    double suma = 0;
    double sumaCuadrados = 0;

    /**
     * Suma los acumulados a si mismo
     * @param otro es un Acumulador parcial a ser sumado
     */
    void sumar(Acumulador otro) {
        archivos += otro.archivos;
        intervalos += otro.intervalos;
        suma += otro.suma;
        sumaCuadrados += otro.sumaCuadrados;
    }

    /**
     * Calcula la media poblacional de los intervalos
     * @return el resultado de calcular la media o 0 si no hay intervalos
     */
    double media() {
        if (intervalos == 0) return 0;
        return suma / intervalos;
    }

    /**
     * Calcula el desvio estandar poblacional
     * @return el resultado de calcular el desvio poblacional o 0 si no hay intervalos
     */
    double desvio() {
        if (intervalos == 0) return 0;
        double mu = media();
        double varianza = sumaCuadrados / intervalos - mu * mu;
        return Math.sqrt(Math.max(0, varianza));
    }

    /**
     * Genera una copia independiente de la instancia actual
     * @return un acumulador con los mismo valores de la instania actual
     */
    Acumulador copia() {
        Acumulador c = new Acumulador();
        c.sumar(this);
        return c;
    }
}
