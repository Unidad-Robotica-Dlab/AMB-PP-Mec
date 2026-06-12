# Capítulo 41: Implementación del Agente BASE en el Motor de Simulación

Este documento desglosa la implementación a nivel de código fuente del Agente BASE dentro del motor `SIM_RAM_CONTEX` (específicamente en `GridEngine.java`), conectando la teoría de su comportamiento estocástico con su ejecución algorítmica.

## 1. Detección del Agente Base (El Switch Arquitectónico)

El motor de simulación opera bajo una arquitectura modular. Para asegurar que el agente evaluado carezca por completo de procesos cognitivos avanzados (comportándose como un "Modelo Nulo"), el ciclo principal de simulación (`run()`) evalúa el estado de las banderas de configuración.

Si las tres capas metacognitivas están desactivadas, el motor bifurca la lógica de decisión hacia la subrutina del Agente Base (`baseOnly = true`):

```java
Action action;
boolean baseOnly = !p.mechSelf && !p.mechSitu && !p.mechReflect;

if (baseOnly) {
    // Agente base: usa pesos fijos configurados (política estacionaria)
    action = sampleAction(
            rng,
            p.baseWeightHunt,
            p.baseWeightMove,
            p.baseWeightStay,
            preyCount > 0,
            emptyCount > 0
    );
}
```

*Análisis:* Esta línea de código es crítica. Demuestra que el agente BASE no evalúa variables de estado interno (hambre, energía) ni variables del parche local (densidad, riesgo), reduciendo su "razonamiento" únicamente a la lectura de sus pesos genéticos y a la posibilidad física de ejecutar la acción.

## 2. El Motor de Decisión Estocástica (`sampleAction`)

La toma de decisiones del Agente BASE se resuelve mediante un muestreo probabilístico por ruleta (*Roulette Wheel Selection*), implementado en la función estática `sampleAction`:

```java
private static Action sampleAction(
        SplittableRandom rng,
        double huntWeight,
        double moveWeight,
        double stayWeight,
        boolean canHunt,
        boolean canMove
) {
    double sh = canHunt ? Math.max(0.0, huntWeight) : 0.0;
    double sm = canMove ? Math.max(0.0, moveWeight) : 0.0;
    double ss = Math.max(0.0, stayWeight);
    
    double sum = sh + sm + ss;
    if (sum <= 0.0) return Action.STAY;
    
    double u = rng.nextDouble() * sum;
    if (u < sh) return Action.HUNT;
    if (u < sh + sm) return Action.MOVE;
    return Action.STAY;
}
```

### 2.1. Condicionamiento Físico del Entorno
Antes de tirar los dados, el agente verifica si la acción es físicamente posible en su celda actual:
*   `canHunt (preyCount > 0)`: Si el radio de percepción del agente está vacío (o si es ciego `Radio=0`), el peso probabilístico de cazar (`sh`) se vuelve `0.0`.
*   `canMove (emptyCount > 0)`: Si el agente está completamente rodeado y no hay celdas vacías, no puede moverse, por lo que `sm` se vuelve `0.0`.
*   *Restricción final:* La acción `STAY` (Esperar/Quedarse) siempre es posible. Si está bloqueado y no puede cazar, el agente se ve forzado a esperar.

### 2.2. La Selección por "Ruleta"
Una vez validados los pesos (ej. $0.5$ caza, $0.25$ moverse, $0.25$ esperar), se suman (`sum`). Luego, el generador pseudoaleatorio de alta eficiencia `SplittableRandom` arroja un número uniforme $u \in [0, sum)$.
El orden de los condicionales (`if u < sh`, etc.) segmenta la línea de probabilidad, garantizando que el agente tome la decisión de forma estrictamente proporcional a los pesos definidos en su "Firma Cognitiva".

## 3. Implicaciones Ecológicas de esta Implementación

La lectura del código fuente revela por qué el modelo BASE colapsa con tanta facilidad:

1.  **Cero Costo Cognitivo:** La función `sampleAction` se ejecuta en tiempo $O(1)$ sin realizar cálculos aritméticos complejos, modelando un organismo perfectamente reactivo impulsado por instinto.
2.  **Ceguera de Supervivencia:** Si un Agente Base de perfil "Depredador" (H50) está rodeado de presas, su probabilidad de cazar sigue siendo del 50%, independientemente de si tiene 1 unidad de energía y está a punto de morir de inanición, o si tiene 50 unidades y debería ahorrar energía.
3.  **El "Suicidio" por Regla:** El código no previene que un agente decida "Moverse" repetidamente hacia celdas vacías agotando su energía metabólica, incluso si hay presas disponibles, porque la ruleta estocástica no tiene "memoria".

Esta implementación directa en Java confirma que el comportamiento del agente BASE es el terreno ideal para probar los módulos avanzados. Al agregar las capas `SELF` o `SITU`, el código ya no pasará por la ruleta ciega, sino que calculará funciones de utilidad que pesarán dinámicamente el riesgo de muerte contra la recompensa de la acción.