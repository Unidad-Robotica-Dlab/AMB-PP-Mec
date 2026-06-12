package com.pph.simramjava;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class Config {
    public final String sessionName;
    public final int simsTotal;
    public final int replicas;
    public final int steps;
    public final int seedBase;
    public final Params params;
    
    public enum SamplingMode { RANDOM, LHS, SOBOL, GRID }
    public final SamplingMode samplingMode;
    public final SamplingGrid samplingGrid;

    // Mechanisms / Scenario
    public final boolean mechSelf;
    public final boolean mechSitu;
    public final boolean mechReflect;
    public final String selfVariant;
    public final String selfHungerMode; // lineal o sigmoide
    public final String situProfileName;
    public final String baseProfileName;
    public final double situOppWeight, situRiskWeight, situDistWeight, situHungerWeight;
    public final double reflectRate, reflectNoise, reflectThreshold, reflectCost;
    public final String gridTopology; 
    public final String baselineMode; // "ecological" o "uniform"
    public final ActionTelemetryMode actionTelemetryMode;

    // Rangos de exploración para pesos base
    public final RangeF baseHuntRange;
    public final RangeF baseMoveRange;
    public final double baseStep;

    // Rangos de exploración para reflexión
    public final RangeF reflectThresholdRange;
    public final RangeF reflectCostRange;
    public final RangeF reflectRateRange;
    public final double reflectStep;

    public static final class RangeF { public final double min, max; RangeF(double a,double b){ this.min=a; this.max=b; } }
    public static final class RangeI { public final int min, max; RangeI(int a,int b){ this.min=a; this.max=b; } }

    public static final class SamplingGrid {
        public final int preyReproLevels;
        public final int predReproLevels;
        public final int predEnergyInitLevels;
        public final int predEnergyEatLevels;
        public final int predEnergyReproLevels;

        SamplingGrid(int preyReproLevels, int predReproLevels, int predEnergyInitLevels, int predEnergyEatLevels, int predEnergyReproLevels) {
            this.preyReproLevels = Math.max(1, preyReproLevels);
            this.predReproLevels = Math.max(1, predReproLevels);
            this.predEnergyInitLevels = Math.max(1, predEnergyInitLevels);
            this.predEnergyEatLevels = Math.max(1, predEnergyEatLevels);
            this.predEnergyReproLevels = Math.max(1, predEnergyReproLevels);
        }

        long ecologicalCombinationCount() {
            return (long) preyReproLevels
                    * (long) predReproLevels
                    * (long) predEnergyInitLevels
                    * (long) predEnergyEatLevels
                    * (long) predEnergyReproLevels;
        }
    }

    public static final class Params {
        public final int size;
        public final double preyDensity;
        public final double predDensity;
        public final int perceptionRadius;
        public final RangeF preyRepro;
        public final RangeF predRepro;
        public final RangeI predEnergyInit;
        public final RangeI predEnergyEat;
        public final RangeI predEnergyRepro;
        public Params(int size, double preyDensity, double predDensity, int perceptionRadius,
                      RangeF preyRepro, RangeF predRepro,
                      RangeI predEnergyInit, RangeI predEnergyEat, RangeI predEnergyRepro) {
            this.size=size; this.preyDensity=preyDensity; this.predDensity=predDensity; this.perceptionRadius=perceptionRadius;
            this.preyRepro=preyRepro; this.predRepro=predRepro; this.predEnergyInit=predEnergyInit; this.predEnergyEat=predEnergyEat; this.predEnergyRepro=predEnergyRepro;
        }
    }

    private Config(String sessionName, int simsTotal, int replicas, int steps, int seedBase, Params params,
                   SamplingMode samplingMode, SamplingGrid samplingGrid,
                   boolean mechSelf, boolean mechSitu, boolean mechReflect,
                   String selfVariant, String selfHungerMode,
                   String situProfileName,
                   String baseProfileName,
                   double situOppWeight, double situRiskWeight, double situDistWeight, double situHungerWeight,
                   double reflectRate, double reflectNoise, double reflectThreshold, double reflectCost,
                   RangeF reflectThresholdRange, RangeF reflectCostRange, RangeF reflectRateRange, double reflectStep,
                   String gridTopology, String baselineMode, ActionTelemetryMode actionTelemetryMode,
                   RangeF baseHuntRange, RangeF baseMoveRange, double baseStep) {
        this.sessionName=sessionName; this.simsTotal=simsTotal; this.replicas=replicas; this.steps=steps; this.seedBase=seedBase; this.params=params;
        this.samplingMode = samplingMode;
        this.samplingGrid = samplingGrid;
        this.mechSelf=mechSelf; this.mechSitu=mechSitu;
        this.mechReflect=mechReflect;
        this.selfVariant=selfVariant;
        this.selfHungerMode=selfHungerMode;
        this.situProfileName = (situProfileName==null||situProfileName.isEmpty() ? inferSituProfileName(situOppWeight, situRiskWeight, situDistWeight, situHungerWeight) : situProfileName);
        this.baseProfileName = (baseProfileName==null||baseProfileName.isEmpty() ? "neutral" : baseProfileName);
        this.situOppWeight=situOppWeight; this.situRiskWeight=situRiskWeight; this.situDistWeight=situDistWeight; this.situHungerWeight=situHungerWeight;
        this.reflectRate=reflectRate; this.reflectNoise=reflectNoise;
        this.reflectThreshold=reflectThreshold; this.reflectCost=reflectCost;
        this.reflectThresholdRange = reflectThresholdRange;
        this.reflectCostRange = reflectCostRange;
        this.reflectRateRange = reflectRateRange;
        this.reflectStep = reflectStep;
        this.gridTopology = (gridTopology==null||gridTopology.isEmpty()?"bounded":gridTopology);
        this.baselineMode = (baselineMode==null||baselineMode.isEmpty()?"ecological":baselineMode);
        this.actionTelemetryMode = (actionTelemetryMode == null ? ActionTelemetryMode.NONE : actionTelemetryMode);
        this.baseHuntRange = baseHuntRange;
        this.baseMoveRange = baseMoveRange;
        this.baseStep = baseStep;
    }

    @SuppressWarnings("unchecked")
    public static Config load(Path path) throws IOException {
        Object root = Json.parse(path);
        if (!(root instanceof Map)) throw new IllegalArgumentException("Config JSON debe ser objeto");
        Map<String,Object> m = (Map<String,Object>) root;
        String sessionName = optString(m.get("session_name"), "");
        int simsTotal = toInt(m.get("sims_total"), 1000);
        int replicas = toInt(m.get("replicas"), 5);
        int steps = toInt(m.get("steps"), 300);
        int seedBase = toInt(m.get("seed_base"), 12345);
        String baselineMode = optString(m.get("baseline_mode"), "ecological");
        Map<String,Object> telemetry = safeMap(m.get("telemetry"));
        ActionTelemetryMode actionTelemetryMode = ActionTelemetryMode.parse(telemetry == null ? null : telemetry.get("action_mode"));
        
        // Sampling Mode
        String sModeStr = optString(m.get("sampling_mode"), "RANDOM").toUpperCase();
        SamplingMode sMode = SamplingMode.RANDOM;
        try { sMode = SamplingMode.valueOf(sModeStr); } catch(Exception e) { sMode = SamplingMode.RANDOM; }

        Map<String,Object> pm = safeMap(m.get("params"));
        if (pm == null) {
            throw new IllegalArgumentException("Config invalida: falta objeto 'params' en " + path);
        }
        int size = toInt(pm.get("size"), 32);
        double preyDensity = toDouble(pm.get("preyDensity"), 0.1);
        double predDensity = toDouble(pm.get("predDensity"), 0.1);
        int pr = toInt(pm.get("perceptionRadius"), 1);
        RangeF preyRepro = toRangeF((Map<String,Object>) pm.get("preyRepro"), 0.1, 0.55);
        RangeF predRepro = toRangeF((Map<String,Object>) pm.get("predRepro"), 0.02, 0.18);
        RangeI predEnergyInit = toRangeI((Map<String,Object>) pm.get("predEnergyInit"), 8, 16);
        RangeI predEnergyEat = toRangeI((Map<String,Object>) pm.get("predEnergyEat"), 4, 12);
        RangeI predEnergyRepro = toRangeI((Map<String,Object>) pm.get("predEnergyRepro"), 18, 30);
        Params params = new Params(size, preyDensity, predDensity, pr, preyRepro, predRepro, predEnergyInit, predEnergyEat, predEnergyRepro);
        SamplingGrid samplingGrid = parseSamplingGrid(m.get("sampling_grid"));
        if (sMode == SamplingMode.GRID) {
            validateSamplingGrid(samplingGrid, params);
            long combos = samplingGrid.ecologicalCombinationCount();
            if (combos > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("sampling_grid genera demasiadas combinaciones: " + combos);
            }
            simsTotal = (int) combos;
        }
        
        // Mechanisms
        Map<String,Object> mech = safeMap(m.get("mechanisms_on"));
        if (mech == null) mech = Collections.emptyMap();
        boolean mechSelf = toBool(mech.get("self"), false);
        boolean mechSitu = toBool(mech.get("situ"), false);
        boolean mechReflect = toBool(mech.get("reflect"), toBool(mech.get("reflex"), false));
        String selfVar = optString(mech.get("self_variant"), "h_move");
        String selfHMode = optString(mech.get("self_hunger_mode"), "lineal");
        
        Map<String,Object> reflex = safeMap(m.get("reflex"));
        if (reflex == null) reflex = Collections.emptyMap();
        Map<String,Object> legacyReasoning = safeMap(m.get("reasoning"));
        if (legacyReasoning == null) legacyReasoning = Collections.emptyMap();
        double rRate = toDouble(firstNonNull(reflex.get("reflect_rate"), reflex.get("rate"), legacyReasoning.get("reflect_rate")), 0.1);
        double rNoise = toDouble(firstNonNull(reflex.get("reflect_noise"), reflex.get("noise"), legacyReasoning.get("reflect_noise")), 1.0);
        double rThreshold = toDouble(firstNonNull(reflex.get("reflect_threshold"), reflex.get("threshold"), legacyReasoning.get("reflect_threshold")), 0.5);
        double rCost = toDouble(firstNonNull(reflex.get("reflect_cost"), reflex.get("cost"), legacyReasoning.get("reflect_cost")), 0.0);
        
        // Reflex Sweep
        Map<String,Object> rExp = safeMap(m.get("reflex_exploration"));
        if (rExp == null) rExp = Collections.emptyMap();
        RangeF rThresholdRange = toRangeF((Map<String,Object>) rExp.get("threshold"), rThreshold, rThreshold);
        RangeF rCostRange = toRangeF((Map<String,Object>) rExp.get("cost"), rCost, rCost);
        RangeF rRateRange = toRangeF((Map<String,Object>) rExp.get("rate"), rRate, rRate);
        double rStep = toDouble(rExp.get("interval"), 0.1);

        // Scenario
        Map<String,Object> scn = safeMap(m.get("scenario"));
        if (scn == null) scn = Collections.emptyMap();
        String gridTopology = optString(scn.getOrDefault("grid_topology", ""), "bounded");
        Map<String,Object> situProfile = safeMap(m.get("situ_profile"));
        if (situProfile == null) situProfile = Collections.emptyMap();
        String situProfileName = optString(situProfile.get("name"), "");
        Map<String,Object> baseProfile = safeMap(m.get("base_profile"));
        if (baseProfile == null) baseProfile = Collections.emptyMap();
        String baseProfileName = optString(baseProfile.get("name"), "");
        double situOppWeight = toDouble(scn.get("situ_w_opp"), 1.0);
        double situRiskWeight = toDouble(scn.get("situ_w_risk"), 1.0);
        double situDistWeight = toDouble(scn.get("situ_w_dist"), 0.5);
        double situHungerWeight = toDouble(scn.get("situ_w_hunger"), 0.5);

        // Base action exploration
        Map<String,Object> bExp = safeMap(m.get("base_exploration"));
        if (bExp == null) bExp = Collections.emptyMap();
        RangeF bHunt = toRangeF((Map<String,Object>) bExp.get("hunt"), 0.33, 0.33);
        RangeF bMove = toRangeF((Map<String,Object>) bExp.get("move"), 0.33, 0.33);
        double bStep = toDouble(bExp.get("interval"), 0.1);
        
        return new Config(sessionName, simsTotal, replicas, steps, seedBase, params,
                sMode, samplingGrid,
                mechSelf, mechSitu, mechReflect, selfVar, selfHMode,
                situProfileName,
                baseProfileName,
                situOppWeight, situRiskWeight, situDistWeight, situHungerWeight,
                rRate, rNoise, rThreshold, rCost,
                rThresholdRange, rCostRange, rRateRange, rStep,
                gridTopology, baselineMode, actionTelemetryMode,
                bHunt, bMove, bStep);
    }

    private static String inferSituProfileName(double opp, double risk, double dist, double hunger) {
        if (close(opp, 1.0) && close(risk, 1.0) && close(dist, 0.5) && close(hunger, 0.5)) return "original";
        if (close(opp, 1.5) && close(risk, 0.5) && close(dist, 0.2) && close(hunger, 1.0)) return "agresivo";
        if (close(opp, 0.8) && close(risk, 1.5) && close(dist, 1.0) && close(hunger, 0.2)) return "cauto";
        if (close(opp, 2.0) && close(risk, 0.1) && close(dist, 0.2) && close(hunger, 1.5)) return "gloton";
        return "custom";
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) <= 1e-9;
    }

    private static String optString(Object o, String d){ return o instanceof String ? (String)o : d; }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> safeMap(Object o) {
        return o instanceof Map ? (Map<String,Object>) o : null;
    }
    private static int toInt(Object o, int d){ 
        if (o instanceof Number) return ((Number) o).intValue(); 
        if (o instanceof String) { try { return (int)Double.parseDouble((String)o); } catch(Exception e){} }
        return d; 
    }
    private static double toDouble(Object o, double d){ 
        if (o instanceof Number) return ((Number)o).doubleValue(); 
        if (o instanceof String) { try { return Double.parseDouble((String)o); } catch(Exception e){} }
        return d; 
    }
    private static boolean toBool(Object o, boolean d){ if (o instanceof Boolean) return (Boolean)o; if (o instanceof Number) return ((Number)o).intValue()!=0; if (o instanceof String){ String s=((String)o).trim().toLowerCase(); if (s.equals("true")||s.equals("1")) return true; if (s.equals("false")||s.equals("0")) return false; } return d; }
    private static Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }
    private static RangeF toRangeF(Map<String,Object> m, double dmin,double dmax){ if (m==null) return new RangeF(dmin,dmax); return new RangeF(toDouble(m.get("min"), dmin), toDouble(m.get("max"), dmax)); }
    private static RangeI toRangeI(Map<String,Object> m, int dmin,int dmax){ if (m==null) return new RangeI(dmin,dmax); return new RangeI(toInt(m.get("min"), dmin), toInt(m.get("max"), dmax)); }
    private static SamplingGrid parseSamplingGrid(Object o) {
        Map<String,Object> m = safeMap(o);
        if (m == null) return new SamplingGrid(3, 3, 3, 3, 3);
        return new SamplingGrid(
                toInt(m.get("preyReproLevels"), 3),
                toInt(m.get("predReproLevels"), 3),
                toInt(m.get("predEnergyInitLevels"), 3),
                toInt(m.get("predEnergyEatLevels"), 3),
                toInt(m.get("predEnergyReproLevels"), 3)
        );
    }
    private static void validateSamplingGrid(SamplingGrid grid, Params params) {
        validateIntLevels("predEnergyInitLevels", grid.predEnergyInitLevels, params.predEnergyInit.min, params.predEnergyInit.max);
        validateIntLevels("predEnergyEatLevels", grid.predEnergyEatLevels, params.predEnergyEat.min, params.predEnergyEat.max);
        validateIntLevels("predEnergyReproLevels", grid.predEnergyReproLevels, params.predEnergyRepro.min, params.predEnergyRepro.max);
    }
    private static void validateIntLevels(String label, int levels, int min, int max) {
        int uniqueValues = Math.max(1, (max - min) + 1);
        if (levels > uniqueValues) {
            throw new IllegalArgumentException(label + "=" + levels + " excede valores enteros unicos disponibles (" + uniqueValues + ")");
        }
    }
}
