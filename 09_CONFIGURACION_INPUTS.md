# Módulo 09: Configuración de Inputs (JSON)

Para asegurar la reproducibilidad estricta de las sesiones computacionales (como la sesión `NEW_CONTEX_BASE_20260520_032409`), el motor ha desacoplado completamente los "números mágicos" hardcodeados del código fuente de Java (clase `Config.java`).

Toda simulación obedece a un contrato (Schema) definido en un archivo JSON externo, permitiendo instanciar lotes experimentales automatizados sin necesidad de re-compilar el simulador.

## 1. Ingesta de Datos (`Json.java`)
La clase de parseo traduce un archivo `config.json` genérico en el objeto strongly-typed `Config`, controlando el arranque de `ParamSetGenerator`.

## 2. Esquema Estándar de Configuración (Schema)
El motor asume la siguiente taxonomía de configuración para instanciar el hipercubo:

```json
{
  "simulation": {
    "seed_base": -3335637410064962301,
    "steps": 500,
    "sampling_mode": "SOBOL",
    "sampling_grid": {
      "preyReproLevels": 3,
      "predReproLevels": 3,
      "predEnergyInitLevels": 3,
      "predEnergyEatLevels": 3,
      "predEnergyReproLevels": 3
    },
    "params": {
      "size": 8,
      "preyDensity": 0.1,
      "predDensity": 0.1,
      "perceptionRadius": 3,
      "preyRepro": {"min": 0.1, "max": 0.55, "type": "double"},
      "predRepro": {"min": 0.02, "max": 0.18, "type": "double"},
      "predEnergyInit": {"min": 8, "max": 16, "type": "int"},
      "predEnergyEat": {"min": 4, "max": 12, "type": "int"},
      "predEnergyRepro": {"min": 18, "max": 30, "type": "int"}
    },
    "scenarios": [
      {
        "name": "base",
        "mech_self": false,
        "mech_situ": false,
        "mech_reflect": false,
        "baseWeightHunt": 0.5,
        "baseWeightMove": 0.25,
        "baseWeightStay": 0.25
      }
    ]
  }
}
```

## 3. Dinámicas de Interpretación de Parámetros
### 3.1. Campos Discretos vs. Rangos (Ranges)
*   **Campos Fijos:** Variables como `size: 8` obligan al motor a ejecutar ese escenario con la grilla forzada. Para iterar 7 tamaños de grilla en Bash, el entorno ejecuta el CLI del motor 7 veces inyectando un archivo JSON distinto (o pisando los parámetros mediante *Environment Variables* sobreescribiendo el config).
*   **Campos de Rango (`min`, `max`):** Aquellas claves que declaran un diccionario con mínimos y máximos (ej. `predEnergyInit`) son detectadas por la clase `Config.java` como variables fisiológicas a resolver mediante el método de Muestreo.

### 3.2. El Factor de Niveles (`Levels`)
Bajo la clave `sampling_grid`, las variables `Levels` controlan la subdivisión continua del parámetro. Aunque en el experimento se utiliza una secuencia de Halton (`sampling_mode: SOBOL`), la multiplicación matemática de estas subdivisiones determina la cantidad de agentes o configuraciones únicas a generar (el volumen del hipercubo interno). En este modelo base $3 \times 3 \times 3 \times 3 \times 3$ no se aplica porque Halton no subdivide uniformemente como un Grid Search multidimensional, pero si la métrica cambiara a `GRID`, esto dictaría un crecimiento de resoluciones de $3^5$.

## 4. El Arreglo Escenarios (`scenarios`)
El arreglo permite agrupar múltiples perfiles en una sola corrida de motor. 
Por ejemplo, si un archivo JSON tiene bajo "scenarios" 4 objetos JSON (uno con el peso de cazar a 50%, otro a 33%, etc.), el motor creará una "cola" (Queue) de simulación, ejecutando la matriz de parámetros iterativos repetidas veces de manera serial, anexándolos ordenadamente al archivo global de salida. Esto disminuye la sobrecarga de Entrada/Salida (I/O Overhead).