class Acumulador {

    long archivos = 0;
    long intervalos = 0;
    double suma = 0;
    double sumaCuadrados = 0;

    void sumar(Acumulador otro) {
        archivos += otro.archivos;
        intervalos += otro.intervalos;
        suma += otro.suma;
        sumaCuadrados += otro.sumaCuadrados;
    }

    double media() {
        if (intervalos == 0) return 0;
        return suma / intervalos;
    }

    double desvio() {
        if (intervalos == 0) return 0;
        double mu = media();
        double varianza = sumaCuadrados / intervalos - mu * mu;
        return Math.sqrt(Math.max(0, varianza));
    }

    Acumulador copia() {
        Acumulador c = new Acumulador();
        c.sumar(this);
        return c;
    }
}
