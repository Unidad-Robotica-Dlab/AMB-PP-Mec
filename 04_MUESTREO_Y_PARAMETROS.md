# Módulo 04: Exploración Paramétrica y Muestreo

Para garantizar validez estadística y evitar sesgos metodológicos ("curse of dimensionality"), el motor no implementa azar puro para la exploración del espacio paramétrico, sino una arquitectura híbrida de diseño de experimentos (DoE).

## 0. Pipeline del Diseño Experimental
```mermaid
graph LR
    A[Grid Search 4D] -->|2,520 Nodos Macro| B[Halton Sequence 5D]
    B -->|120 Fisiologías por Nodo| C[Monte Carlo]
    C -->|5 Réplicas de Semilla| D[1,512,000 Simulaciones]
    
    style A fill:#2ca02c,stroke:#333,stroke-width:2px
    style B fill:#1f77b4,stroke:#333,stroke-width:2px
    style C fill:#9467bd,stroke:#333,stroke-width:2px
    style D fill:#d62728,stroke:#333,stroke-width:2px,color:#fff
```

## 1. Grid Search (El Macro-Entorno Discreto)
Las variables ambientales se determinan usando una matriz ortogonal exhaustiva:
*   **Tamaño (`param_size`):** [8, 16, 32, 64, 128, 256, 512].
*   **Densidades Combinadas (`param_preyDensity`, `param_predDensity`):** Restringidas bajo Exclusión Espacial ($\rho_{prey} + \rho_{pred} \le 1.0$).
*   **Visión (`param_perceptionRadius`):** [0, 3].
*   **Firma BASE:** [H50, M50, S50, Eq].

## 2. Secuencia de Halton (La Fisiología Continua)
Para explorar las mutaciones fisiológicas del agente sin aglomeración estadística (*clumping*), se utiliza una Secuencia Cuasi-Aleatoria (QMC) basada en la serie de Van der Corput en $D=5$ dimensiones.

### 2.1. Dimensiones y Bases Primas Ortogonales
Para garantizar la independencia estadística, a cada parámetro continuo se le asigna un número primo co-primo como base matemática:
*   $P_1 = 2$ $\to$ `param_preyRepro`
*   $P_2 = 3$ $\to$ `param_predRepro`
*   $P_3 = 5$ $\to$ `param_predEnergyInit`
*   $P_4 = 7$ $\to$ `param_predEnergyEat`
*   $P_5 = 11$ $\to$ `param_predEnergyRepro`

### 2.2. Algoritmo de Muestreo (Van der Corput)
El valor bruto de un agente $i$ para la dimensión de base $b$ se calcula invirtiendo la representación de $i$ en el sistema base $b$:
$$v_i = \sum_{j=0}^{k} d_j b^{-(j+1)}$$

Implementación en código (`ParamSetGenerator.java`):
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

Luego, este valor $v_i \in [0, 1)$ se proyecta linealmente a los límites biológicos configurados del ecosistema mediante: $v_{scaled} = \min + v_i \times (\max - \min)$.

```java
private static double scale(double val, double min, double max) { 
    return min + val * (max - min); 
}

// Ejemplo de uso en el generador:
matrix[i][0] = scale(vdc(i+1, bases[0]), cfg.params.preyRepro.min, cfg.params.preyRepro.max);
```

## 3. Justificación Científica
*   **Halton (1960):** Minimización de la discrepancia para integrales de alta dimensión.
*   **Saltelli (2008):** El uso de QMC en Análisis de Sensibilidad Global previene sesgos de agrupamiento que sufren los algoritmos estocásticos clásicos (Monte Carlo puro).