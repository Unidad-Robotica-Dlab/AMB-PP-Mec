# Módulo 05: Telemetría, Métricas y Viabilidad Robusta

El motor instrumenta cada entidad y el entorno holístico para exportar métricas rigurosas de la Teoría de Sistemas Complejos y de la Información.

## 1. Definición de Atractores y Viabilidad
Medir si un ecosistema ABM sobrevive basándose en la "presencia de agentes en el último tick" (`Coexistencia Simple`) acarrea enormes falsos positivos. Para validar el éxito algorítmico, se implementa la métrica condicional de **Viabilidad Robusta**.

### 1.1. Las 3 Condiciones de Viabilidad Robusta
Sea $P(t)$ la densidad poblacional en el tiempo $t$, evaluado en la cola temporal $t \in [t_{max} - W, t_{max}]$ (donde $W$ es la ventana de análisis, e.g., últimos 100 ticks).

1.  **Persistencia de Reserva:** La densidad final no debe perforar la cota mínima de riesgo estocástico ($P(t_{max}) \ge P_{min}$). Garantiza ausencia de Allee effect destructivo.
2.  **Constancia Estacionaria (Coeficiente de Variación):** La fluctuación de la cola temporal debe cumplir $CV \le 0.25$. Donde $CV = \frac{\sigma}{\mu}$. Demuestra que la oscilación de Lotka-Volterra ha sido amortiguada o ha encontrado un atractor estable.
3.  **Límite de Derivada (Slope):** La pendiente de la regresión lineal en la ventana $W$ debe ser $Slope \ge -10^{-4}$. Un slope negativo significativo indica que el sistema se considera en "Agonía Lenta", no en equilibrio estacionario.

Implementación en código (`Analysis.java` / `RunResult.java`):
```java
// Viabilidad Fuerte (Persistencia)
boolean coexistEnd = (finalPrey > 0 && finalPred > 0);
boolean preyReserveOk = finalPrey >= preyMinHardCount;
boolean predReserveOk = finalPred >= predMinHardCount;
boolean viableHard = coexistEnd && preyReserveOk && predReserveOk;

// Viabilidad Robusta (Persistencia + Constancia Estacionaria)
boolean robustTailPredOk = predTailCv <= 0.25 && predTailSlope >= -1e-4;
boolean viableRobust = viableHard && robustTailPredOk;
```

*Referencia Teórica:* Grimm & Wissel (1997) "Babel, or the ecological stability discussions"; Turchin (2003) "Complex Population Dynamics".

## 2. Entropía de Acciones y Cadenas de Markov
Para auditar la "conciencia" del agente (especialmente en mecanismos REFLEX), el motor mide cuán predecibles o dinámicas son sus políticas.

### 2.1. Entropía de Shannon del Comportamiento
$$H(A) = - \sum_{i \in \{Hunt, Move, Stay\}} p(a_i) \log_2 p(a_i)$$
Donde $p(a_i)$ es la proporción empírica de dicha acción sobre el total de acciones en el ecosistema.
*   **Significado:** Si $H(A)$ es máxima, el agente es caótico. Si cae a 0, el agente está bloqueado en un modo autómata (Ej. solo quedarse quieto para ahorrar energía al infinito).

### 2.2. Telemetría Markoviana de Contexto
El motor intercepta el "Micro-Estado" ($S_i$) de cada agente en cada tick antes de tomar la decisión.
*   **Tupla de Estado:** Hambre (Alta/Baja) $\times$ Riesgo (Alto/Bajo) $\times$ Acción Anterior $\times$ Acción Tomada.
*   Al emitir estos logs continuos en formato `.mtel`, herramientas post-procesamiento en Python mapean Grafos Dirigidos de Cadenas de Markov empíricas para demostrar que las firmas cognitivas difieren estructuralmente en su *manifold* conductual, no solo en su éxito final de supervivencia.