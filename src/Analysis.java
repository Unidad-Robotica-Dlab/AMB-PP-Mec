package com.pph.simramjava;

import java.util.*;

final class Analysis {
    static Map<String,Object> summarize(List<Map<String,Object>> rows, String session, String variant){
        if (rows==null || rows.isEmpty()) throw new IllegalArgumentException("rows vacío");
        Map<String, Object> first = rows.get(0);
        
        double size = getD(first, "param_size", 0.0);
        double preyDensity = getD(first, "param_preyDensity", 0.0);
        double predDensity = getD(first, "param_predDensity", 0.0);
        double perceptionRadius = getD(first, "param_perceptionRadius", 0.0);
        double gridCells = Math.max(1.0, size*size);

        int totalRuns = rows.size();
        int coexistEndRuns=0, hardViableRuns=0, robustViableRuns=0, extPred=0, extPrey=0, extBoth=0;
        List<Double> tExtPredVals = new ArrayList<>();
        List<Double> tExtPreyVals = new ArrayList<>();
        double birthPreySum=0, birthPredSum=0, lossPreySum=0, lossPredSum=0, totalEatSum=0;
        double spatialBlockPredMoveSum = 0, spatialBlockPreyMoveSum = 0, spatialBlockPreyBirthSum = 0;
        List<Double> preyNetVals = new ArrayList<>();
        List<Double> predNetVals = new ArrayList<>();
        List<Double> preyRVals = new ArrayList<>();
        List<Double> predRVals = new ArrayList<>();
        List<Double> preyFinalVals = new ArrayList<>();
        List<Double> predFinalVals = new ArrayList<>();
        List<Double> predEnergyVals = new ArrayList<>();
        List<Double> trophicEfficiencyVals = new ArrayList<>();
        List<Double> actionEntropyVals = new ArrayList<>();
        List<Double> tailWindowVals = new ArrayList<>();
        List<Double> preyTailMeanVals = new ArrayList<>();
        List<Double> predTailMeanVals = new ArrayList<>();
        List<Double> preyTailCvVals = new ArrayList<>();
        List<Double> predTailCvVals = new ArrayList<>();
        List<Double> preyTailSlopeVals = new ArrayList<>();
        List<Double> predTailSlopeVals = new ArrayList<>();
        long actionTotalSum = 0L;
        long actionHuntSum = 0L;
        long actionMoveSum = 0L;
        long actionStaySum = 0L;
        long actionHuntCaptureSum = 0L;
        long[] contextSums = new long[ActionTelemetrySupport.CONTEXT_BIN_COUNT * Action.values().length];
        long[] contextTotalSums = new long[ActionTelemetrySupport.CONTEXT_BIN_COUNT];
        long[] transitionSums = new long[Action.values().length * Action.values().length];
        long[] transitionRowTotals = new long[Action.values().length];
        long[] contextTransitionSums = new long[ActionTelemetrySupport.CONTEXT_BIN_COUNT * ActionTelemetrySupport.CONTEXT_BIN_COUNT];
        long[] contextTransitionRowTotals = new long[ActionTelemetrySupport.CONTEXT_BIN_COUNT];
        long[] finalContextSums = new long[ActionTelemetrySupport.CONTEXT_BIN_COUNT];
        long[] finalActionSums = new long[Action.values().length];
        boolean hasActionTelemetry = false;
        boolean hasContextTelemetry = false;
        boolean hasTransitionTelemetry = false;
        boolean hasContextTransitionTelemetry = false;
        boolean hasFinalStateTelemetry = false;

        for (Map<String,Object> row: rows){
            boolean coexist = row.containsKey("coexist_end")
                    ? isTrue(row.get("coexist_end"))
                    : inferCoexistEnd(row);
            boolean viableHard = row.containsKey("viable_hard")
                    ? isTrue(row.get("viable_hard"))
                    : inferViableHard(row);
            boolean viableRobust = row.containsKey("viable_robust")
                    ? isTrue(row.get("viable_robust"))
                    : inferViableRobust(row);
            Double tPred = getOptD(row.get("t_ext_pred"));
            Double tPrey = getOptD(row.get("t_ext_prey"));
            if (tPred != null) tExtPredVals.add(tPred);
            if (tPrey != null) tExtPreyVals.add(tPrey);
            double birthPrey = getD(row, "birth_prey", 0.0);
            double birthPred = getD(row, "birth_pred", 0.0);
            double lossPrey = getD(row, "death_eaten", 0.0);
            double lossPred = getD(row, "death_starve", 0.0);
            birthPreySum += birthPrey; birthPredSum += birthPred;
            lossPreySum += lossPrey; lossPredSum += lossPred;
            totalEatSum += getD(row, "total_eat", 0.0);
            spatialBlockPredMoveSum += getD(row, "spatial_block_pred_move", 0.0);
            spatialBlockPreyMoveSum += getD(row, "spatial_block_prey_move", 0.0);
            spatialBlockPreyBirthSum += getD(row, "spatial_block_prey_birth", 0.0);
            preyNetVals.add((birthPrey - lossPrey)/gridCells);
            predNetVals.add((birthPred - lossPred)/gridCells);
            double preyR = PopulationMetrics.reproductiveRatio(birthPrey, lossPrey);
            double predR = PopulationMetrics.reproductiveRatio(birthPred, lossPred);
            preyRVals.add(preyR); predRVals.add(predR);
            preyFinalVals.add(getD(row, "prey_final_density", 0.0));
            predFinalVals.add(getD(row, "pred_final_density", 0.0));
            predEnergyVals.add(getD(row, "avg_pred_energy", 0.0));
            trophicEfficiencyVals.add(getD(row, "trophic_efficiency", 0.0));
            tailWindowVals.add(getD(row, "tail_window_steps", 0.0));
            preyTailMeanVals.add(getD(row, "prey_tail_mean_density", 0.0));
            predTailMeanVals.add(getD(row, "pred_tail_mean_density", 0.0));
            preyTailCvVals.add(getD(row, "prey_tail_cv", 0.0));
            predTailCvVals.add(getD(row, "pred_tail_cv", 0.0));
            preyTailSlopeVals.add(getD(row, "prey_tail_slope", 0.0));
            predTailSlopeVals.add(getD(row, "pred_tail_slope", 0.0));
            if (row.containsKey("action_total_n")) {
                hasActionTelemetry = true;
                actionTotalSum += getL(row, "action_total_n", 0L);
                actionHuntSum += getL(row, "action_hunt_n", 0L);
                actionMoveSum += getL(row, "action_move_n", 0L);
                actionStaySum += getL(row, "action_stay_n", 0L);
                actionHuntCaptureSum += getL(row, "action_hunt_capture_n", 0L);
                actionEntropyVals.add(getD(row, "action_entropy", 0.0));
            }
            for (int ctx = 0; ctx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; ctx++) {
                String totalKey = ActionTelemetrySupport.totalKey(ctx);
                if (!row.containsKey(totalKey)) {
                    continue;
                }
                hasContextTelemetry = true;
                long ctxTotal = getL(row, totalKey, 0L);
                contextTotalSums[ctx] += ctxTotal;
                for (Action action : Action.values()) {
                    contextSums[ctx * Action.values().length + action.ordinal()] += getL(row, ActionTelemetrySupport.actionKey(ctx, action), 0L);
                }
            }
            for (Action from : Action.values()) {
                String rowTotalKey = ActionTelemetrySupport.transitionRowTotalKey(from);
                if (!row.containsKey(rowTotalKey)) {
                    continue;
                }
                hasTransitionTelemetry = true;
                long rowTotal = getL(row, rowTotalKey, 0L);
                transitionRowTotals[from.ordinal()] += rowTotal;
                for (Action to : Action.values()) {
                    transitionSums[from.ordinal() * Action.values().length + to.ordinal()] += getL(row, ActionTelemetrySupport.transitionKey(from, to), 0L);
                }
            }
            for (int fromCtx = 0; fromCtx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; fromCtx++) {
                String rowTotalKey = ActionTelemetrySupport.contextTransitionRowTotalKey(fromCtx);
                if (!row.containsKey(rowTotalKey)) {
                    continue;
                }
                hasContextTransitionTelemetry = true;
                long rowTotal = getL(row, rowTotalKey, 0L);
                contextTransitionRowTotals[fromCtx] += rowTotal;
                for (int toCtx = 0; toCtx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; toCtx++) {
                    contextTransitionSums[fromCtx * ActionTelemetrySupport.CONTEXT_BIN_COUNT + toCtx]
                            += getL(row, ActionTelemetrySupport.contextTransitionKey(fromCtx, toCtx), 0L);
                }
            }
            for (int ctx = 0; ctx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; ctx++) {
                String key = ActionTelemetrySupport.finalContextKey(ctx);
                if (row.containsKey(key)) {
                    hasFinalStateTelemetry = true;
                    finalContextSums[ctx] += getL(row, key, 0L);
                }
            }
            for (Action action : Action.values()) {
                String key = ActionTelemetrySupport.finalActionKey(action);
                if (row.containsKey(key)) {
                    hasFinalStateTelemetry = true;
                    finalActionSums[action.ordinal()] += getL(row, key, 0L);
                }
            }
            if (coexist) coexistEndRuns++;
            if (viableHard) hardViableRuns++;
            if (viableRobust) robustViableRuns++;
            if (!coexist) {
                if (tPred!=null && tPrey!=null){
                    if (tPred < tPrey) extPred++; else if (tPrey < tPred) extPrey++; else extBoth++;
                } else if (tPred!=null) extPred++; else if (tPrey!=null) extPrey++; else extBoth++;
            }
        }

        double avgBirthPrey = birthPreySum / totalRuns;
        double avgLossPrey = lossPreySum / totalRuns;
        double avgBirthPred = birthPredSum / totalRuns;
        double avgLossPred = lossPredSum / totalRuns;
        double preyNetPerCell = (avgBirthPrey - avgLossPrey)/gridCells;
        double predNetPerCell = (avgBirthPred - avgLossPred)/gridCells;

        Map<String,Object> summary = new LinkedHashMap<>();
        summary.put("session", session);
        summary.put("variant", variant);
        
        // Copiar todos los parámetros del primer registro (asumiendo que son constantes en el lote)
        for (String key : first.keySet()) {
            if (key.startsWith("param_")) {
                // Si la variable varió en el lote (como param_preyRepro), tal vez no sea ideal
                // poner el valor del primero, pero al menos da una pista.
                // En el caso de UMAP, ayuda tener estas columnas.
                summary.put(key, first.get(key));
            }
        }
        summary.put("base_profile_name", first.getOrDefault("base_profile_name", first.get("param_base_profile_name")));
        summary.put("situ_profile_name", first.getOrDefault("situ_profile_name", first.get("param_situ_profile_name")));
        
        summary.put("grid_size", (int)size);
        summary.put("grid_cells", (int)(size*size));
        summary.put("prey_density", preyDensity);
        summary.put("pred_density", predDensity);
        summary.put("param_perceptionRadius", (int)perceptionRadius);
        summary.put("perception_radius", (int)perceptionRadius);
        summary.put("total_runs", totalRuns);
        summary.put("coexist_end_runs", coexistEndRuns);
        summary.put("viable_runs", hardViableRuns);
        double pCoexistEnd = totalRuns>0 ? (coexistEndRuns/(double)totalRuns) : 0.0;
        double pViableHard = totalRuns>0 ? (hardViableRuns/(double)totalRuns) : 0.0;
        double pViableRobust = totalRuns>0 ? (robustViableRuns/(double)totalRuns) : 0.0;
        summary.put("p_coexist_end", pCoexistEnd);
        summary.put("p_coexist_end_std", binomStd(coexistEndRuns, totalRuns));
        summary.put("hard_viable_runs", hardViableRuns);
        summary.put("p_viable_hard", pViableHard);
        summary.put("p_viable_hard_std", binomStd(hardViableRuns, totalRuns));
        summary.put("robust_viable_runs", robustViableRuns);
        summary.put("p_viable_robust", pViableRobust);
        summary.put("p_viable_robust_std", binomStd(robustViableRuns, totalRuns));
        summary.put("p_viable", pViableHard);
        summary.put("p_viable_std", binomStd(hardViableRuns, totalRuns));
        summary.put("p_ext_pred", totalRuns>0 ? (extPred/(double)totalRuns) : 0.0);
        summary.put("p_ext_pred_std", binomStd(extPred, totalRuns));
        summary.put("p_ext_prey", totalRuns>0 ? (extPrey/(double)totalRuns) : 0.0);
        summary.put("p_ext_prey_std", binomStd(extPrey, totalRuns));
        summary.put("p_ext_both", totalRuns>0 ? (extBoth/(double)totalRuns) : 0.0);
        summary.put("p_ext_both_std", binomStd(extBoth, totalRuns));
        summary.put("avg_t_ext_pred", avg(tExtPredVals));
        summary.put("avg_t_ext_prey", avg(tExtPreyVals));
        summary.put("prey_birth_rate", avgBirthPrey);
        summary.put("prey_loss_rate", avgLossPrey);
        summary.put("pred_birth_rate", avgBirthPred);
        summary.put("pred_loss_rate", avgLossPred);
        summary.put("prey_net_per_cell", preyNetPerCell);
        summary.put("prey_net_per_cell_std", std(preyNetVals));
        summary.put("pred_net_per_cell", predNetPerCell);
        summary.put("pred_net_per_cell_std", std(predNetVals));
        summary.put("avg_total_eat", totalEatSum/totalRuns);
        summary.put("spatial_block_pred_move", spatialBlockPredMoveSum);
        summary.put("spatial_block_prey_move", spatialBlockPreyMoveSum);
        summary.put("spatial_block_prey_birth", spatialBlockPreyBirthSum);
        summary.put("spatial_block_total", spatialBlockPredMoveSum + spatialBlockPreyMoveSum + spatialBlockPreyBirthSum);
        summary.put("prey_final_density", avg(preyFinalVals));
        summary.put("prey_final_density_std", std(preyFinalVals));
        summary.put("pred_final_density", avg(predFinalVals));
        summary.put("pred_final_density_std", std(predFinalVals));
        summary.put("avg_pred_energy", avg(predEnergyVals));
        summary.put("avg_pred_energy_std", std(predEnergyVals));
        summary.put("avg_trophic_efficiency", avg(trophicEfficiencyVals));
        summary.put("avg_trophic_efficiency_std", std(trophicEfficiencyVals));
        summary.put("tail_window_steps", avg(tailWindowVals));
        summary.put("tail_window_steps_std", std(tailWindowVals));
        summary.put("prey_tail_mean_density", avg(preyTailMeanVals));
        summary.put("prey_tail_mean_density_std", std(preyTailMeanVals));
        summary.put("pred_tail_mean_density", avg(predTailMeanVals));
        summary.put("pred_tail_mean_density_std", std(predTailMeanVals));
        summary.put("prey_tail_cv", avg(preyTailCvVals));
        summary.put("prey_tail_cv_std", std(preyTailCvVals));
        summary.put("pred_tail_cv", avg(predTailCvVals));
        summary.put("pred_tail_cv_std", std(predTailCvVals));
        summary.put("prey_tail_slope", avg(preyTailSlopeVals));
        summary.put("prey_tail_slope_std", std(preyTailSlopeVals));
        summary.put("pred_tail_slope", avg(predTailSlopeVals));
        summary.put("pred_tail_slope_std", std(predTailSlopeVals));
        summary.put("prey_R", avg(preyRVals));
        summary.put("prey_R_std", std(preyRVals));
        summary.put("pred_R", avg(predRVals));
        summary.put("pred_R_std", std(predRVals));
        if (hasActionTelemetry) {
            summary.put("action_total_n", actionTotalSum);
            summary.put("action_hunt_n", actionHuntSum);
            summary.put("action_move_n", actionMoveSum);
            summary.put("action_stay_n", actionStaySum);
            summary.put("action_entropy", avg(actionEntropyVals));
            summary.put("action_entropy_std", std(actionEntropyVals));
            summary.put("action_hunt_capture_n", actionHuntCaptureSum);
            summary.put("action_hunt_share", actionTotalSum > 0L ? actionHuntSum / (double) actionTotalSum : 0.0);
            summary.put("action_move_share", actionTotalSum > 0L ? actionMoveSum / (double) actionTotalSum : 0.0);
            summary.put("action_stay_share", actionTotalSum > 0L ? actionStaySum / (double) actionTotalSum : 0.0);
            summary.put("action_hunt_capture_rate", actionHuntSum > 0L ? actionHuntCaptureSum / (double) actionHuntSum : 0.0);
        }
        if (hasContextTelemetry) {
            for (int ctx = 0; ctx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; ctx++) {
                long ctxTotal = contextTotalSums[ctx];
                summary.put(ActionTelemetrySupport.totalKey(ctx), ctxTotal);
                for (Action action : Action.values()) {
                    long count = contextSums[ctx * Action.values().length + action.ordinal()];
                    summary.put(ActionTelemetrySupport.actionKey(ctx, action), count);
                    summary.put(ActionTelemetrySupport.actionShareKey(ctx, action), ctxTotal > 0L ? count / (double) ctxTotal : 0.0);
                }
            }
        }
        if (hasTransitionTelemetry) {
            for (Action from : Action.values()) {
                long rowTotal = transitionRowTotals[from.ordinal()];
                summary.put(ActionTelemetrySupport.transitionRowTotalKey(from), rowTotal);
                for (Action to : Action.values()) {
                    long count = transitionSums[from.ordinal() * Action.values().length + to.ordinal()];
                    summary.put(ActionTelemetrySupport.transitionKey(from, to), count);
                    summary.put(ActionTelemetrySupport.transitionProbKey(from, to), rowTotal > 0L ? count / (double) rowTotal : 0.0);
                }
            }
        }
        if (hasContextTransitionTelemetry) {
            for (int fromCtx = 0; fromCtx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; fromCtx++) {
                long rowTotal = contextTransitionRowTotals[fromCtx];
                summary.put(ActionTelemetrySupport.contextTransitionRowTotalKey(fromCtx), rowTotal);
                for (int toCtx = 0; toCtx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; toCtx++) {
                    long count = contextTransitionSums[fromCtx * ActionTelemetrySupport.CONTEXT_BIN_COUNT + toCtx];
                    summary.put(ActionTelemetrySupport.contextTransitionKey(fromCtx, toCtx), count);
                    summary.put(ActionTelemetrySupport.contextTransitionProbKey(fromCtx, toCtx), rowTotal > 0L ? count / (double) rowTotal : 0.0);
                }
            }
        }
        if (hasFinalStateTelemetry) {
            long finalContextTotal = 0L;
            for (int ctx = 0; ctx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; ctx++) {
                long count = finalContextSums[ctx];
                summary.put(ActionTelemetrySupport.finalContextKey(ctx), count);
                finalContextTotal += count;
            }
            long finalActionTotal = 0L;
            for (Action action : Action.values()) {
                long count = finalActionSums[action.ordinal()];
                summary.put(ActionTelemetrySupport.finalActionKey(action), count);
                finalActionTotal += count;
            }
            summary.put("pred_final_ctx_total_n", finalContextTotal);
            summary.put("pred_final_action_total_n", finalActionTotal);
        }
        return summary;
    }

    private static boolean inferCoexistEnd(Map<String,Object> row) {
        return getD(row, "prey_final_density", 0.0) > 0.0 && getD(row, "pred_final_density", 0.0) > 0.0;
    }

    private static boolean inferViableHard(Map<String,Object> row) {
        if (!inferCoexistEnd(row)) return false;
        double size = getD(row, "param_size", 0.0);
        double gridCells = Math.max(1.0, size * size);
        long preyInitialCount = initialCount(getD(row, "param_preyDensity", 0.0), gridCells);
        long predInitialCount = initialCount(getD(row, "param_predDensity", 0.0), gridCells);
        long preyFinalCount = row.containsKey("prey_final_count")
                ? Math.round(getD(row, "prey_final_count", 0.0))
                : Math.round(getD(row, "prey_final_density", 0.0) * gridCells);
        long predFinalCount = row.containsKey("pred_final_count")
                ? Math.round(getD(row, "pred_final_count", 0.0))
                : Math.round(getD(row, "pred_final_density", 0.0) * gridCells);
        long preyMinHardCount = Math.max(10L, Math.round(0.05 * preyInitialCount));
        long predMinHardCount = Math.max(3L, Math.round(0.01 * predInitialCount));
        return preyFinalCount >= preyMinHardCount && predFinalCount >= predMinHardCount;
    }

    private static boolean inferViableRobust(Map<String,Object> row) {
        if (!inferViableHard(row)) return false;
        return getD(row, "pred_tail_cv", 0.0) <= 0.25 && getD(row, "pred_tail_slope", 0.0) >= -1e-4;
    }

    private static long initialCount(double density, double gridCells) {
        return Math.max(0L, Math.round(density * gridCells));
    }

    private static boolean isTrue(Object v){
        if (v == null) return false; if (v instanceof Boolean) return (Boolean)v; if (v instanceof Number) return ((Number)v).intValue()!=0;
        String s = String.valueOf(v).trim();
        return s.equals("1") || s.equalsIgnoreCase("true");
    }

    private static double getD(Map<String,Object> m, String k, double d){ Object v=m.get(k); if (v instanceof Number) return ((Number)v).doubleValue(); return d; }
    private static long getL(Map<String,Object> m, String k, long d){ Object v=m.get(k); if (v instanceof Number) return ((Number)v).longValue(); return d; }
    private static Double getOptD(Object v){ if (v==null) return null; if (v instanceof Number) return ((Number)v).doubleValue(); try { return Double.parseDouble(String.valueOf(v)); } catch(Exception ignore){ return null; } }
    private static double avg(List<Double> xs){ if (xs==null||xs.isEmpty()) return 0.0; double s=0; for(double x:xs)s+=x; return s/xs.size(); }
    private static double std(List<Double> xs){ if (xs==null||xs.size()<=1) return 0.0; double m=avg(xs); double v=0; for(double x:xs){ double d=x-m; v+=d*d; } v/= (xs.size()-1); return Math.sqrt(v); }
    private static double binomStd(int success, int n){ if (n<=0) return 0.0; double p = success/(double)n; return Math.sqrt((p*(1-p))/n); }
}
