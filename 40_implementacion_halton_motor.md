# Capítulo 40: Implementación del Método de Muestreo de Halton en el Motor de Simulación

Este documento detalla la implementación técnica del muestreo cuasi-aleatorio utilizado para explorar el hiperespacio continuo de los parámetros fisiológicos de los agentes en el motor `SIM_RAM_CONTEX`.

## 1. El Problema de la Exploración de Alta Dimensionalidad

El diseño experimental requería someter los escenarios ambientales (generados mediante *Grid Search*) a una amplia gama de variaciones fisiológicas internas de los agentes. En concreto, se debían explorar simultáneamente 5 parámetros continuos o discretos acotados:

1.  Tasa de Reproducción de la Presa (`preyRepro`)
2.  Tasa de Reproducción del Depredador (`predRepro`)
3.  Energía Inicial del Depredador (`predEnergyInit`)
4.  Energía de Caza del Depredador (`predEnergyEat`)
5.  Energía Requerida para Reproducción (`predEnergyRepro`)

El uso de generadores pseudo-aleatorios tradicionales (como Monte Carlo) para poblar este espacio de 5 dimensiones presenta un riesgo conocido como "aglomeración" (clumping): los valores aleatorios tienden a agruparse, dejando "huecos ciegos" masivos en el espacio de parámetros que nunca son evaluados.

## 2. La Solución: Secuencia de Halton (Baja Discrepancia)

Para garantizar una cobertura uniforme y determinista de todo el hiperespacio biológico, el motor implementa un **Muestreo de Baja Discrepancia mediante la Secuencia de Halton**. 

*Nota de nomenclatura en el código:* Aunque en las opciones de interfaz (`menu.sh`) la opción 3 fue referida coloquialmente como `SOBOL` por su naturaleza de baja discrepancia, la implementación algorítmica interna en la clase `ParamSetGenerator.java` corresponde estrictamente a una secuencia de Halton multidimensional basada en la serie de Van der Corput.

### 2.1. El Algoritmo Base: Van der Corput (`vdc`)
El núcleo matemático reside en la función `vdc`, la cual toma el índice iterativo de la configuración ($n$) y lo proyecta a un valor fraccionario ortogonal en el rango $[0, 1)$ utilizando una base prima ($base$):

```java
private static double vdc(int n, int base) {
    double v = 0, f = 1.0 / base;
    while (n > 0) { 
        v += f * (n % base); 
        n /= base; 
        f /= base; 
    }
    return v;
}
```
*Operación:* El algoritmo invierte la representación del número $n$ en el sistema numérico de la $base$ asignada y lo refleja alrededor del punto radical, generando una secuencia que siempre parte el espacio vacío restante en segmentos iguales.

### 2.2. Ortogonalidad Multidimensional (Asignación de Bases Primas)
Para extender el algoritmo a 5 dimensiones y asegurar que ningún parámetro esté matemáticamente correlacionado con otro, el motor asigna a cada parámetro fisiológico una base constituida por los primeros 5 números primos secuenciales:

```java
int[] bases = {2, 3, 5, 7, 11};
```

1.  **Base 2:** Asignada a la Tasa de Reproducción de Presas.
2.  **Base 3:** Asignada a la Tasa de Reproducción de Depredadores.
3.  **Base 5:** Asignada a la Energía Inicial (Convertido a Entero).
4.  **Base 7:** Asignada a la Energía de Caza (Convertido a Entero).
5.  **Base 11:** Asignada a la Energía de Reproducción (Convertido a Entero).

Como los números primos son coprimos entre sí por definición, los ciclos de muestreo nunca se alinean periódicamente en el hipercubo, evitando las fallas de resonancia paramétrica.

### 2.3. Proyección a Rango Biológico (`scale`)
El valor fraccionario estricto entregado por el motor de Halton es proyectado algebraicamente hacia los rangos mínimos y máximos (establecidos empíricamente en la configuración) utilizando interpolación lineal:

```java
private static double scale(double val, double min, double max) { 
    return min + val * (max - min); 
}
```

El ciclo maestro genera iterativamente a los fenotipos mediante la aplicación conjunta del algoritmo `vdc` y `scale`:

```java
for (int i = 0; i < n; i++) {
    matrix[i][0] = scale(vdc(i+1, bases[0]), cfg.params.preyRepro.min, cfg.params.preyRepro.max);
    matrix[i][1] = scale(vdc(i+1, bases[1]), cfg.params.predRepro.min, cfg.params.predRepro.max);
    // Para parámetros de energía, el valor escalado se redondea a entero (Math.round)
    matrix[i][2] = Math.round(scale(vdc(i+1, bases[2]), cfg.params.predEnergyInit.min, cfg.params.predEnergyInit.max));
    ...
}
```

## 3. Justificación y Valor Científico
La implementación de este algoritmo es vital para la solidez metodológica de la investigación. Al generar configuraciones biológicas con baja discrepancia:
1.  Se reduce la varianza de la integración multidimensional más rápido que el muestreo aleatorio puro (tasa de convergencia $O((\ln N)^D / N)$ vs $O(1/\sqrt{N})$ de Monte Carlo).
2.  Se asegura que cualquier falla observada en el modelo no es atribuible a un muestreo pobre de los rangos biológicamente viables, cimentando la robustez de las evaluaciones cognitivas que se efectúen posteriormente.

## 4. Referencias Científicas y Literarias

La decisión arquitectónica de sustituir el muestreo pseudoaleatorio estándar (Monte Carlo) por Secuencias de Halton para la exploración del hiperespacio fisiológico (Quasi-Monte Carlo) está rigurosamente fundamentada en la literatura de la ciencia computacional y la simulación de sistemas complejos:

1.  **Halton, J. H. (1960).** *On the efficiency of certain quasi-random sequences of points in evaluating multi-dimensional integrals*. Numerische Mathematik, 2(1), 84-90.
    *   *Justificación:* El artículo fundacional que demuestra matemáticamente que extender la secuencia 1D de Van der Corput a $N$ dimensiones utilizando bases primas coprimas minimiza la discrepancia espacial, siendo el estándar para poblar hipercubos de parámetros.
2.  **Niederreiter, H. (1992).** *Random Number Generation and Quasi-Monte Carlo Methods*. Society for Industrial and Applied Mathematics (SIAM).
    *   *Justificación:* Texto de referencia obligatoria en simulación estocástica. Niederreiter establece que para dimensiones bajas a moderadas (como los 5 parámetros continuos evaluados en el motor: $D=5$), las secuencias de Halton superan ampliamente la tasa de convergencia del muestreo aleatorio tradicional, evitando las agrupaciones o vacíos ("clumping and gapping").
3.  **Saltelli, A., et al. (2008).** *Global Sensitivity Analysis: The Primer*. John Wiley & Sons.
    *   *Justificación:* En la literatura de Análisis de Sensibilidad y Modelado Basado en Agentes (ABM), Saltelli aboga por el uso de secuencias de baja discrepancia (Sobol, Halton) para el diseño de experimentos computacionales (Design of Experiments - DoE), ya que garantizan que el análisis de la viabilidad no estará sesgado por muestras atípicas ("outliers") derivadas de un mal generador de números aleatorios.