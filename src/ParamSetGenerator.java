package com.pph.simramjava;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ParamSetGenerator {
    private ParamSetGenerator() {}

    public static List<ParamSet> generate(Config cfg, Integer maxRuns) {
        return generate(cfg, maxRuns, null);
    }

    public static List<ParamSet> generate(Config cfg, Integer maxRuns, ActionTelemetryMode overrideTelemetryMode) {
        int sims = Math.max(1, cfg.simsTotal);
        int reps = Math.max(1, cfg.replicas);
        int steps = Math.max(1, cfg.steps);
        List<ParamSet> list = new ArrayList<>();
        ActionTelemetryMode telemetryMode = (overrideTelemetryMode == null ? cfg.actionTelemetryMode : overrideTelemetryMode);
        
        SplittableRandom masterRng = new SplittableRandom(cfg.seedBase ^ 0x9E3779B97F4A7C15L);

        // Generar matriz de parámetros ecológicos según el modo
        double[][] ecoMatrix = generateEcoMatrix(sims, cfg, masterRng);

        outer:
        for (int i = 0; i < sims; i++) {
            double[] ecoParams = ecoMatrix[i];
            
            // Barrido de parámetros de reflexión
            for (double rt = cfg.reflectThresholdRange.min; rt <= cfg.reflectThresholdRange.max + 1e-9; rt += cfg.reflectStep) {
                for (double rc = cfg.reflectCostRange.min; rc <= cfg.reflectCostRange.max + 1e-9; rc += cfg.reflectStep) {
                    for (double rr = cfg.reflectRateRange.min; rr <= cfg.reflectRateRange.max + 1e-9; rr += cfg.reflectStep) {

                        // Barrido de pesos base
                        for (double wh = cfg.baseHuntRange.min; wh <= cfg.baseHuntRange.max + 1e-9; wh += cfg.baseStep) {
                            for (double wm = cfg.baseMoveRange.min; wm <= cfg.baseMoveRange.max + 1e-9; wm += cfg.baseStep) {
                                
                                double ws = 1.0 - (wh + wm);
                                if (ws < -1e-9) continue;
                                ws = Math.max(0.0, ws);

                                for (int r = 0; r < reps; r++) {
                                    long seed = seedFor(cfg.seedBase, i, r);
                                    
                                    ParamSet ps = new ParamSet(
                                            cfg.params.size,
                                            cfg.params.preyDensity,
                                            cfg.params.predDensity,
                                            cfg.params.perceptionRadius,
                                            ecoParams[0], cfg.params.preyRepro.min, cfg.params.preyRepro.max,
                                            ecoParams[1], cfg.params.predRepro.min, cfg.params.predRepro.max,
                                            (int)Math.round(ecoParams[2]), cfg.params.predEnergyInit.min, cfg.params.predEnergyInit.max,
                                            (int)Math.round(ecoParams[3]), cfg.params.predEnergyEat.min, cfg.params.predEnergyEat.max,
                                            (int)Math.round(ecoParams[4]), cfg.params.predEnergyRepro.min, cfg.params.predEnergyRepro.max,
                                            steps, seed, i, r, reps,
                                            cfg.mechSelf, cfg.mechSitu, cfg.mechReflect,
                                            cfg.selfVariant, cfg.selfHungerMode,
                                            cfg.situProfileName,
                                            cfg.baseProfileName,
                                            cfg.situOppWeight, cfg.situRiskWeight, cfg.situDistWeight, cfg.situHungerWeight,
                                            rr, cfg.reflectNoise,
                                            rt, rc,
                                            cfg.gridTopology, cfg.baselineMode, telemetryMode,
                                            wh, cfg.baseHuntRange.min, cfg.baseHuntRange.max,
                                            wm, cfg.baseMoveRange.min, cfg.baseMoveRange.max,
                                            ws, cfg.baseStep
                                    );
                                    list.add(ps);
                                    if (maxRuns != null && maxRuns >= 0 && list.size() >= maxRuns) break outer;
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    private static double[][] generateEcoMatrix(int n, Config cfg, SplittableRandom rng) {
        Config.SamplingMode mode = cfg.samplingMode;

        if (mode == Config.SamplingMode.LHS) {
            double[][] matrix = new double[n][5];
            double[][] columns = new double[5][];
            columns[0] = lhsDimension(n, cfg.params.preyRepro.min, cfg.params.preyRepro.max, rng);
            columns[1] = lhsDimension(n, cfg.params.predRepro.min, cfg.params.predRepro.max, rng);
            columns[2] = lhsToDouble(lhsDimensionInt(n, cfg.params.predEnergyInit.min, cfg.params.predEnergyInit.max, rng));
            columns[3] = lhsToDouble(lhsDimensionInt(n, cfg.params.predEnergyEat.min, cfg.params.predEnergyEat.max, rng));
            columns[4] = lhsToDouble(lhsDimensionInt(n, cfg.params.predEnergyRepro.min, cfg.params.predEnergyRepro.max, rng));
            double[][] finalMatrix = new double[n][5];
            for (int j = 0; j < 5; j++) {
                for (int i = 0; i < n; i++) {
                    finalMatrix[i][j] = columns[j][i];
                }
            }
            return finalMatrix;
        } 
        else if (mode == Config.SamplingMode.SOBOL) {
            double[][] matrix = new double[n][5];
            // Secuencia Cuasi-Aleatoria (Van der Corput en bases primas)
            int[] bases = {2, 3, 5, 7, 11};
            for (int i = 0; i < n; i++) {
                matrix[i][0] = scale(vdc(i+1, bases[0]), cfg.params.preyRepro.min, cfg.params.preyRepro.max);
                matrix[i][1] = scale(vdc(i+1, bases[1]), cfg.params.predRepro.min, cfg.params.predRepro.max);
                matrix[i][2] = Math.round(scale(vdc(i+1, bases[2]), cfg.params.predEnergyInit.min, cfg.params.predEnergyInit.max));
                matrix[i][3] = Math.round(scale(vdc(i+1, bases[3]), cfg.params.predEnergyEat.min, cfg.params.predEnergyEat.max));
                matrix[i][4] = Math.round(scale(vdc(i+1, bases[4]), cfg.params.predEnergyRepro.min, cfg.params.predEnergyRepro.max));
            }
            return matrix;
        } 
        else if (mode == Config.SamplingMode.GRID) {
            return gridMatrix(cfg);
        }
        else { // RANDOM (Original)
            double[][] matrix = new double[n][5];
            for (int i = 0; i < n; i++) {
                matrix[i][0] = uniform(cfg.params.preyRepro.min, cfg.params.preyRepro.max, rng);
                matrix[i][1] = uniform(cfg.params.predRepro.min, cfg.params.predRepro.max, rng);
                matrix[i][2] = uniformInt(cfg.params.predEnergyInit.min, cfg.params.predEnergyInit.max, rng);
                matrix[i][3] = uniformInt(cfg.params.predEnergyEat.min, cfg.params.predEnergyEat.max, rng);
                matrix[i][4] = uniformInt(cfg.params.predEnergyRepro.min, cfg.params.predEnergyRepro.max, rng);
            }
            return matrix;
        }
    }

    private static double[][] gridMatrix(Config cfg) {
        double[] preyRepro = gridLevels(cfg.samplingGrid.preyReproLevels, cfg.params.preyRepro.min, cfg.params.preyRepro.max);
        double[] predRepro = gridLevels(cfg.samplingGrid.predReproLevels, cfg.params.predRepro.min, cfg.params.predRepro.max);
        double[] predEnergyInit = gridLevelsInt(cfg.samplingGrid.predEnergyInitLevels, cfg.params.predEnergyInit.min, cfg.params.predEnergyInit.max);
        double[] predEnergyEat = gridLevelsInt(cfg.samplingGrid.predEnergyEatLevels, cfg.params.predEnergyEat.min, cfg.params.predEnergyEat.max);
        double[] predEnergyRepro = gridLevelsInt(cfg.samplingGrid.predEnergyReproLevels, cfg.params.predEnergyRepro.min, cfg.params.predEnergyRepro.max);
        int total = preyRepro.length * predRepro.length * predEnergyInit.length * predEnergyEat.length * predEnergyRepro.length;
        double[][] matrix = new double[total][5];
        int idx = 0;
        for (double prey : preyRepro) {
            for (double pred : predRepro) {
                for (double eInit : predEnergyInit) {
                    for (double eEat : predEnergyEat) {
                        for (double eRepro : predEnergyRepro) {
                            matrix[idx][0] = prey;
                            matrix[idx][1] = pred;
                            matrix[idx][2] = eInit;
                            matrix[idx][3] = eEat;
                            matrix[idx][4] = eRepro;
                            idx++;
                        }
                    }
                }
            }
        }
        return matrix;
    }

    private static double vdc(int n, int base) {
        double v = 0, f = 1.0 / base;
        while (n > 0) { v += f * (n % base); n /= base; f /= base; }
        return v;
    }
    private static double scale(double val, double min, double max) { return min + val * (max - min); }

    private static double[] gridLevels(int levels, double min, double max) {
        double[] vals = new double[Math.max(1, levels)];
        if (vals.length == 1) {
            vals[0] = (min + max) / 2.0;
            return vals;
        }
        double step = (max - min) / (vals.length - 1);
        for (int i = 0; i < vals.length; i++) {
            vals[i] = min + (i * step);
        }
        return vals;
    }

    private static double[] gridLevelsInt(int levels, int min, int max) {
        double[] vals = new double[Math.max(1, levels)];
        if (vals.length == 1) {
            vals[0] = Math.round((min + max) / 2.0);
            return vals;
        }
        double step = (max - min) / (double) (vals.length - 1);
        for (int i = 0; i < vals.length; i++) {
            vals[i] = Math.round(min + (i * step));
        }
        return vals;
    }

    private static double[] lhsDimension(int n, double min, double max, SplittableRandom rng) {
        double[] vals = new double[n];
        double step = (max - min) / n;
        List<Integer> indices = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(indices, new Random(rng.nextLong()));
        for (int i = 0; i < n; i++) vals[i] = min + (indices.get(i) * step) + (rng.nextDouble() * step);
        return vals;
    }

    private static int[] lhsDimensionInt(int n, int min, int max, SplittableRandom rng) {
        int[] vals = new int[n];
        int rangeSize = (max - min) + 1;
        List<Integer> pool = IntStream.range(0, n).map(i -> min + (i % rangeSize)).boxed().collect(Collectors.toList());
        Collections.shuffle(pool, new Random(rng.nextLong()));
        for (int i = 0; i < n; i++) vals[i] = pool.get(i);
        return vals;
    }

    private static double[] lhsToDouble(int[] src) {
        double[] d = new double[src.length];
        for(int i=0; i<src.length; i++) d[i] = src[i];
        return d;
    }

    private static double uniform(double a, double b, SplittableRandom rng){ return a + (b - a) * rng.nextDouble(); }
    private static int uniformInt(int a, int b, SplittableRandom rng){ return a + rng.nextInt((b - a) + 1); }
    private static long seedFor(int base, int i, int r){ return (((long) base) << 32) ^ (i * 10007L + r * 7919L + 0xD1B54A32D192ED03L); }
}
