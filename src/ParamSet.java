package com.pph.simramjava;

public final class ParamSet {
    public final int size;
    public final double preyDensity;
    public final double predDensity;
    public final int perceptionRadius;
    public final double preyRepro, preyReproMin, preyReproMax;
    public final double predRepro, predReproMin, predReproMax;
    public final int predEnergyInit, predEnergyInitMin, predEnergyInitMax;
    public final int predEnergyEat, predEnergyEatMin, predEnergyEatMax;
    public final int predEnergyRepro, predEnergyReproMin, predEnergyReproMax;
    public final int steps;
    public final long seed;
    public final int baseIndex;
    public final int replicaIndex;
    public final int replicasTotal;
    // Mechanisms
    public final boolean mechSelf;
    public final boolean mechSitu;
    public final boolean mechReflect; 
    public final String selfVariant;
    public final String selfHungerMode; // lineal o sigmoide
    public final String situProfileName;
    public final String baseProfileName;
    public final double situOppWeight, situRiskWeight, situDistWeight, situHungerWeight;
    public final double reflectRate; 
    public final double reflectNoise;
    public final double reflectThreshold;
    public final double reflectCost;
    public final String gridTopology;
    public final String baselineMode; // "ecological" o "uniform"
    public final ActionTelemetryMode actionTelemetryMode;

    // Pesos base específicos de esta corrida
    public final double baseWeightHunt, baseWeightHuntMin, baseWeightHuntMax;
    public final double baseWeightMove, baseWeightMoveMin, baseWeightMoveMax;
    public final double baseWeightStay;
    public final double baseWeightInterval;

    public ParamSet(int size, double preyDensity, double predDensity, int perceptionRadius,
                    double preyRepro, double preyReproMin, double preyReproMax,
                    double predRepro, double predReproMin, double predReproMax,
                    int predEnergyInit, int predEnergyInitMin, int predEnergyInitMax,
                    int predEnergyEat, int predEnergyEatMin, int predEnergyEatMax,
                    int predEnergyRepro, int predEnergyReproMin, int predEnergyReproMax,
                    int steps, long seed, int baseIndex, int replicaIndex, int replicasTotal,
                    boolean mechSelf, boolean mechSitu, boolean mechReflect,
                    String selfVariant, String selfHungerMode,
                    String situProfileName,
                    String baseProfileName,
                    double situOppWeight, double situRiskWeight, double situDistWeight, double situHungerWeight,
                    double reflectRate, double reflectNoise,
                    double reflectThreshold, double reflectCost,
                    String gridTopology, String baselineMode, ActionTelemetryMode actionTelemetryMode,
                    double baseWeightHunt, double baseWeightHuntMin, double baseWeightHuntMax,
                    double baseWeightMove, double baseWeightMoveMin, double baseWeightMoveMax,
                    double baseWeightStay, double baseWeightInterval) {
        this.size=size; this.preyDensity=preyDensity; this.predDensity=predDensity; this.perceptionRadius=perceptionRadius;
        this.preyRepro=preyRepro; this.preyReproMin=preyReproMin; this.preyReproMax=preyReproMax;
        this.predRepro=predRepro; this.predReproMin=predReproMin; this.predReproMax=predReproMax;
        this.predEnergyInit=predEnergyInit; this.predEnergyInitMin=predEnergyInitMin; this.predEnergyInitMax=predEnergyInitMax;
        this.predEnergyEat=predEnergyEat; this.predEnergyEatMin=predEnergyEatMin; this.predEnergyEatMax=predEnergyEatMax;
        this.predEnergyRepro=predEnergyRepro; this.predEnergyReproMin=predEnergyReproMin; this.predEnergyReproMax=predEnergyReproMax;
        this.steps=steps; this.seed=seed; this.baseIndex=baseIndex; this.replicaIndex=replicaIndex; this.replicasTotal=replicasTotal;
        this.mechSelf=mechSelf; this.mechSitu=mechSitu;
        this.mechReflect=mechReflect;
        this.selfVariant=selfVariant;
        this.selfHungerMode=selfHungerMode;
        this.situProfileName = (situProfileName==null||situProfileName.isEmpty() ? "unknown" : situProfileName);
        this.baseProfileName = (baseProfileName==null||baseProfileName.isEmpty() ? "neutral" : baseProfileName);
        this.situOppWeight=situOppWeight; this.situRiskWeight=situRiskWeight; this.situDistWeight=situDistWeight; this.situHungerWeight=situHungerWeight;
        this.reflectRate=reflectRate; this.reflectNoise=reflectNoise;
        this.reflectThreshold=reflectThreshold; this.reflectCost=reflectCost;
        this.gridTopology = (gridTopology==null||gridTopology.isEmpty()?"bounded":gridTopology);
        this.baselineMode = (baselineMode==null||baselineMode.isEmpty()?"ecological":baselineMode);
        this.actionTelemetryMode = (actionTelemetryMode == null ? ActionTelemetryMode.NONE : actionTelemetryMode);
        this.baseWeightHunt = baseWeightHunt; this.baseWeightHuntMin = baseWeightHuntMin; this.baseWeightHuntMax = baseWeightHuntMax;
        this.baseWeightMove = baseWeightMove; this.baseWeightMoveMin = baseWeightMoveMin; this.baseWeightMoveMax = baseWeightMoveMax;
        this.baseWeightStay = baseWeightStay;
        this.baseWeightInterval = baseWeightInterval;
    }
}
