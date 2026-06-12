# Módulo 08: Esquemas de Datos y Telemetría de Cadenas de Markov

La validez del motor no recae únicamente en sus reglas de ecología, sino en su capacidad para externalizar los datos microscópicos de forma cuantificable. Este módulo define el contrato de datos (API) entre el motor de simulación y los analistas estadísticos (Python).

## 1. Contrato de Datos Estructurales (CSV)
El motor exporta dos entidades fundamentales post-simulación. 

*   **`runs.csv`**: Tabla transaccional masiva. Cada fila es una corrida de simulación independiente (1.5 Millones de filas en el experimento BASE). Contiene el identificador `run_id`, el ID de la semilla, el tiempo en que se extinguió, etc.
*   **`summary.csv`**: Agregación analítica por "Firma Paramétrica". Agrupa las $N$ réplicas estocásticas y promedia la columna binaria `viable_robust` para emitir la tasa estadística `p_viable_robust`. 

## 2. Niveles de Telemetría Cognitiva (`ActionTelemetryMode`)
Para permitir el estudio de la "mente" de los agentes, el motor captura cada evento neuronal antes de la ejecución física de la acción a través de su clase de serialización `ActionTelemetrySupport.java`. Existen niveles ascendentes de inspección:

1.  **`GLOBAL`**: Resume al final del tick: % de cazas, % de movimientos, y la "Entropía de Shannon" global del enjambre.
2.  **`CONTEXT`**: Agrupa acciones por variables simples (ej. ¿Cazó estando hambriento o lleno?).
3.  **`DUAL_MARKOV` (El Estándar Científico Principal)**: Registra la secuencia de estados completa en el tiempo.

## 3. Arquitectura del Espacio de Estados Markoviano

Para generar los mapas de Grafos y el Meta-Modelo causal, el sistema de telemetría define un **Micro-Estado Discreto** $S_i$ por cada agente, compuesto por $16$ bins contextuales.

### 3.1. Operadores de Bitwise para el Contexto
El código original de Java define la matriz de estado cognitivo utilizando máscaras binarias para codificar el entorno percibido y el estado interno del agente en un único número entero (`idx` de 0 a 15):

```java
for (int idx = 0; idx < CONTEXT_BIN_COUNT; idx++) {
    int prey   = ((idx & 1) != 0) ? 1 : 0; // ¿Percibe presa?
    int empty  = ((idx & 2) != 0) ? 1 : 0; // ¿Tiene espacio para moverse?
    int hunger = ((idx & 4) != 0) ? 1 : 0; // ¿Hambre por encima de HUNGER_HIGH_THRESHOLD (0.5)?
    int risk   = ((idx & 8) != 0) ? 1 : 0; // ¿Riesgo competitivo alto?
}
```

### 3.2. La Cadena de Transición Empírica (El Grafo Dirigido)
El motor no solo evalúa el contexto en $t$, sino la **Acción Tomada** $a_t \in \{HUNT, MOVE, STAY\}$. 
Por lo tanto, la telemetría registra las probabilidades de transición:
$$P(S_{i_{t+1}} \mid S_{i_t})$$

Donde la "firma conductual" real del agente no es simplemente qué acción toma, sino cómo su acción altera su contexto futuro.
*   *Ejemplo de transición capturada:* Un agente en estado [Presa Sí, Hambriento] ejecuta `HUNT` $\rightarrow$ y transiciona al estado [Presa No, Lleno].

### 3.3. Compresión Binaria Discreta (`.mtel.gz`)
Dado el volumen extremo de estos registros (miles de agentes decidiendo miles de acciones por cada uno de los 500 pasos a través de 1.5 millones de corridas), la escritura en CSV colapsaría la memoria de almacenamiento. 
El modo `MARKOV_TEMPORAL` y `DUAL_MARKOV` vectorizan las frecuencias absolutas en arreglos serializados que se comprimen con GZip (`.mtel.gz`). Los scripts externos de Python descomprimen estos archivos on-the-fly para reconstruir los Grafos Markovianos.