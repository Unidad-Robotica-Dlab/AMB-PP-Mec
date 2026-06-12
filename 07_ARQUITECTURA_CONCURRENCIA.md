# Módulo 07: Arquitectura de Concurrencia y Escalabilidad

Para que la "Explosión Combinatoria" de 1.5 millones de simulaciones no tarde meses en calcularse, el motor `SIM_RAM_CONTEX` implementa una arquitectura altamente concurrente, aprovechando el paradigma de paralelismo en memoria compartida (Shared-Memory Parallelism).

## 1. Patrón de Concurrencia (Task Parallelism)

A diferencia de los enfoques donde se paraleliza la actualización interna de una sola grilla (lo cual en autómatas celulares genera problemas de contención y *locks*), este motor paraleliza a nivel de **Escenario**.

*   **Regla de Oro Arquitectónica:** Las simulaciones son procesos "Embarrassingly Parallel" (Vergonzosamente Paralelizables). El estado de la simulación A en la celda 1,1 no depende en absoluto del estado de la simulación B. 
*   Por lo tanto, la estrategia óptima es inyectar un escenario completo en un hilo de procesador diferente y dejar que se ejecute del paso $t=0$ a $t=500$ de forma aislada.

## 2. Implementación en Java (`SessionRunner.java`)

El motor utiliza el marco de trabajo estándar de concurrencia de Java (`ExecutorService`) para gestionar la carga de trabajo sobre los núcleos de CPU físicos disponibles de forma segura.

### 2.1. Gestión del Pool de Hilos (Thread Pool)
```java
// Se detectan o configuran el número de 'workers' (núcleos de CPU)
ExecutorService ex = Executors.newFixedThreadPool(workers);
List<Future<RunResult>> futs = new ArrayList<>(n);

for (int i=0; i<n; i++) {
    final int idx = i; 
    final ParamSet p = ps.get(i);
    final String runId = String.format("run_%05d", idx);
    
    // El Callable encapsula la simulación completa y retorna el RunResult al futuro
    futs.add(ex.submit(() -> new RunResult(runId, p, runWithObservers(p, runId, label, timelineDir, videoDir))));
}
```

### 2.2. Flujo de Ejecución Multihilo
```mermaid
graph TD
    A[Inicio: SessionRunner] --> B[Generar 2,520 x 5 ParamSets en RAM]
    B --> C[Crear FixedThreadPool N Cores]
    C --> D[Submit Callable: Run_00001]
    C --> E[Submit Callable: Run_00002]
    C --> F[Submit Callable: Run_N]
    
    subgraph Hilos del Procesador (Workers)
    D -->|Tick 0-500| G[RunResult 1]
    E -->|Tick 0-500| H[RunResult 2]
    F -->|Tick 0-500| I[RunResult N]
    end
    
    G --> J[Futuros Resueltos 'f.get()']
    H --> J
    I --> J
    
    J --> K[Escribir a memoria / CSV de forma secuencial]
```

## 3. Prevención de Cuellos de Botella (Locks)
Para maximizar el rendimiento (Throughput):
1.  **Instanciación de Objetos:** El generador de números aleatorios (`SplittableRandom rng = new SplittableRandom(seed)`) se aísla por hilo. Si todos los hilos intentaran leer un solo `Math.random()` global, se crearía un bloqueo de concurrencia inmenso.
2.  **Escritura Secuencial Diferida:** Observa el código: `for (Future<RunResult> f : futs){ out.add(f.get()); }`. Ninguna simulación intenta escribir directamente en el archivo `runs.csv` o `summary.csv` mientras corre. Escribir a disco interrumpe el procesador (I/O Block). En lugar de eso, cada hilo guarda su propio objeto `RunResult` en memoria RAM. Cuando todos los hilos han terminado, el hilo principal recolecta los objetos (Future) y hace una sola escritura masiva y bloqueante al disco duro.

## 4. Notas para Migración a Python
Si se migra este motor a Python (ej. librería `Mesa`), usar hilos (Threading) **fracasará catastróficamente** debido al GIL (Global Interpreter Lock), forzando a que los escenarios se ejecuten secuencialmente, a pesar de tener múltiples núcleos.
La migración *debe* utilizar la librería `multiprocessing` (procesos separados con memoria de espacio aislada) y utilizar una cola de recolección segura (`Queue` o `Pool.map`) para devolver el estado telemetrizado al proceso padre.