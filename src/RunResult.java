package com.pph.simramjava;

import java.util.LinkedHashMap;
import java.util.Map;

final class RunResult {
    final String runId;
    final ParamSet p;
    final GridEngine.Result r;
    RunResult(String runId, ParamSet p, GridEngine.Result r){ this.runId=runId; this.p=p; this.r=r; }

    Map<String,Object> toRow(String session, String variant){
        long gridCells = (long) p.size * (long) p.size;
        long preyInitialCount = initialCount(p.preyDensity, gridCells);
        long predInitialCount = initialCount(p.predDensity, gridCells);
        double preyFinalDensity = (p.size>0? ((double)r.finalPrey)/gridCells:0.0);
        double predFinalDensity = (p.size>0? ((double)r.finalPred)/gridCells:0.0);
        long preyMinHardCount = Math.max(10L, Math.round(0.05 * preyInitialCount));
        long predMinHardCount = Math.max(3L, Math.round(0.01 * predInitialCount));
        double preyMinHardDensity = gridCells > 0L ? preyMinHardCount / (double) gridCells : 0.0;
        double predMinHardDensity = gridCells > 0L ? predMinHardCount / (double) gridCells : 0.0;
        boolean coexistEnd = (r.finalPrey > 0 && r.finalPred > 0);
        boolean preyReserveOk = r.finalPrey >= preyMinHardCount;
        boolean predReserveOk = r.finalPred >= predMinHardCount;
        boolean viableHard = coexistEnd && preyReserveOk && predReserveOk;
        boolean robustTailPredOk = r.predTailCv <= 0.25 && r.predTailSlope >= -1e-4;
        boolean viableRobust = viableHard && robustTailPredOk;
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("session", session);
        row.put("variant", variant);
        row.put("run_id", runId);
        row.put("param_size", p.size);
        row.put("param_preyDensity", p.preyDensity);
        row.put("param_predDensity", p.predDensity);
        row.put("param_perceptionRadius", p.perceptionRadius);
        row.put("perception_radius", p.perceptionRadius);
        row.put("param_preyRepro", p.preyRepro);
        row.put("param_preyRepro_min", p.preyReproMin);
        row.put("param_preyRepro_max", p.preyReproMax);
        row.put("param_predRepro", p.predRepro);
        row.put("param_predRepro_min", p.predReproMin);
        row.put("param_predRepro_max", p.predReproMax);
        row.put("param_predEnergyInit", p.predEnergyInit);
        row.put("param_predEnergyInit_min", p.predEnergyInitMin);
        row.put("param_predEnergyInit_max", p.predEnergyInitMax);
        row.put("param_predEnergyEat", p.predEnergyEat);
        row.put("param_predEnergyEat_min", p.predEnergyEatMin);
        row.put("param_predEnergyEat_max", p.predEnergyEatMax);
        row.put("param_predEnergyRepro", p.predEnergyRepro);
        row.put("param_predEnergyRepro_min", p.predEnergyReproMin);
        row.put("param_predEnergyRepro_max", p.predEnergyReproMax);
        row.put("param_steps", p.steps);
        row.put("param_seed", p.seed);
        row.put("param_baselineMode", p.baselineMode);
        row.put("param_mech_self", p.mechSelf ? 1 : 0);
        row.put("param_mech_situ", p.mechSitu ? 1 : 0);
        row.put("param_mech_reflect", p.mechReflect ? 1 : 0);
        row.put("param_self_variant", p.selfVariant);
        row.put("param_self_hunger_mode", p.selfHungerMode);
        row.put("base_profile_name", p.baseProfileName);
        row.put("param_base_profile_name", p.baseProfileName);
        row.put("situ_profile_name", p.situProfileName);
        row.put("param_situ_profile_name", p.situProfileName);
        row.put("param_situ_w_opp", p.situOppWeight);
        row.put("param_situ_w_risk", p.situRiskWeight);
        row.put("param_situ_w_dist", p.situDistWeight);
        row.put("param_situ_w_hunger", p.situHungerWeight);
        row.put("param_reflect_rate", p.reflectRate);
        row.put("param_reflect_noise", p.reflectNoise);
        row.put("param_reflect_threshold", p.reflectThreshold);
        row.put("param_reflect_cost", p.reflectCost);
        row.put("param_grid_topology", p.gridTopology);
        row.put("param_action_telemetry_mode", p.actionTelemetryMode.name().toLowerCase());
        row.put("param_action_ctx_hunger_threshold", ActionTelemetrySupport.HUNGER_HIGH_THRESHOLD);
        row.put("param_action_ctx_risk_threshold", ActionTelemetrySupport.RISK_HIGH_THRESHOLD);
        if (!p.mechSelf && !p.mechSitu && !p.mechReflect) {
            row.put("param_base_weight_hunt", p.baseWeightHunt);
            row.put("param_base_weight_hunt_min", p.baseWeightHuntMin);
            row.put("param_base_weight_hunt_max", p.baseWeightHuntMax);
            row.put("param_base_weight_move", p.baseWeightMove);
            row.put("param_base_weight_move_min", p.baseWeightMoveMin);
            row.put("param_base_weight_move_max", p.baseWeightMoveMax);
            row.put("param_base_weight_stay", p.baseWeightStay);
            row.put("param_base_weight_interval", p.baseWeightInterval);
        }
        row.put("param_base_index", p.baseIndex);
        row.put("param_replica_index", p.replicaIndex);
        row.put("param_replicas_total", p.replicasTotal);
        row.put("param___base_index", p.baseIndex);
        row.put("param___replica_index", p.replicaIndex);
        row.put("total_eat", r.totalEat);
        row.put("death_starve", r.deathStarve);
        row.put("death_conflict", r.deathConflict);
        row.put("death_eaten", r.deathEaten);
        row.put("spatial_block_pred_move", r.spatialBlockPredMove);
        row.put("spatial_block_prey_move", r.spatialBlockPreyMove);
        row.put("spatial_block_prey_birth", r.spatialBlockPreyBirth);
        row.put("spatial_block_total", r.spatialBlockPredMove + r.spatialBlockPreyMove + r.spatialBlockPreyBirth);
        row.put("birth_prey", r.birthPrey);
        row.put("birth_pred", r.birthPred);
        row.put("t_ext_prey", r.tExtPrey);
        row.put("t_ext_pred", r.tExtPred);
        row.put("coexist_end", coexistEnd ? 1 : 0);
        row.put("prey_initial_count", preyInitialCount);
        row.put("pred_initial_count", predInitialCount);
        row.put("prey_final_count", r.finalPrey);
        row.put("pred_final_count", r.finalPred);
        row.put("prey_final_density", preyFinalDensity);
        row.put("pred_final_density", predFinalDensity);
        row.put("hard_prey_min_count", preyMinHardCount);
        row.put("hard_pred_min_count", predMinHardCount);
        row.put("hard_prey_min_density", preyMinHardDensity);
        row.put("hard_pred_min_density", predMinHardDensity);
        row.put("hard_prey_reserve_ok", preyReserveOk ? 1 : 0);
        row.put("hard_pred_reserve_ok", predReserveOk ? 1 : 0);
        row.put("tail_window_steps", r.tailWindowSteps);
        row.put("prey_tail_mean_density", r.preyTailMeanDensity);
        row.put("pred_tail_mean_density", r.predTailMeanDensity);
        row.put("prey_tail_cv", r.preyTailCv);
        row.put("pred_tail_cv", r.predTailCv);
        row.put("prey_tail_slope", r.preyTailSlope);
        row.put("pred_tail_slope", r.predTailSlope);
        row.put("robust_tail_pred_ok", robustTailPredOk ? 1 : 0);
        row.put("viable_hard", viableHard ? 1 : 0);
        row.put("viable_robust", viableRobust ? 1 : 0);
        row.put("avg_pred_energy", r.totalPredCount > 0L ? r.cumulativePredEnergy / (double) r.totalPredCount : 0.0);
        row.put("trophic_efficiency", r.birthPrey > 0 ? r.totalEat / (double) r.birthPrey : 0.0);
        if (p.actionTelemetryMode.includesGlobal()) {
            row.put("action_total_n", r.actionTotal);
            row.put("action_hunt_n", r.actionHunt);
            row.put("action_move_n", r.actionMove);
            row.put("action_stay_n", r.actionStay);
            row.put("action_entropy", PopulationMetrics.shannonEntropy(r.actionHunt, r.actionMove, r.actionStay));
            row.put("action_hunt_capture_n", r.actionHuntCapture);
            row.put("action_hunt_share", r.actionTotal > 0L ? r.actionHunt / (double) r.actionTotal : 0.0);
            row.put("action_move_share", r.actionTotal > 0L ? r.actionMove / (double) r.actionTotal : 0.0);
            row.put("action_stay_share", r.actionTotal > 0L ? r.actionStay / (double) r.actionTotal : 0.0);
            row.put("action_hunt_capture_rate", r.actionHunt > 0L ? r.actionHuntCapture / (double) r.actionHunt : 0.0);
        }
        if (r.actionContextCounts != null) {
            for (int ctx = 0; ctx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; ctx++) {
                long total = 0L;
                for (Action action : Action.values()) {
                    long count = r.actionContextCounts[ctx * Action.values().length + action.ordinal()];
                    row.put(ActionTelemetrySupport.actionKey(ctx, action), count);
                    total += count;
                }
                row.put(ActionTelemetrySupport.totalKey(ctx), total);
            }
        }
        if (r.actionTransitionCounts != null) {
            for (Action from : Action.values()) {
                long rowTotal = 0L;
                for (Action to : Action.values()) {
                    long count = r.actionTransitionCounts[from.ordinal() * Action.values().length + to.ordinal()];
                    row.put(ActionTelemetrySupport.transitionKey(from, to), count);
                    rowTotal += count;
                }
                row.put(ActionTelemetrySupport.transitionRowTotalKey(from), rowTotal);
                for (Action to : Action.values()) {
                    long count = r.actionTransitionCounts[from.ordinal() * Action.values().length + to.ordinal()];
                    row.put(ActionTelemetrySupport.transitionProbKey(from, to), rowTotal > 0L ? count / (double) rowTotal : 0.0);
                }
            }
        }
        if (r.contextTransitionCounts != null) {
            for (int fromCtx = 0; fromCtx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; fromCtx++) {
                long rowTotal = 0L;
                for (int toCtx = 0; toCtx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; toCtx++) {
                    long count = r.contextTransitionCounts[fromCtx * ActionTelemetrySupport.CONTEXT_BIN_COUNT + toCtx];
                    row.put(ActionTelemetrySupport.contextTransitionKey(fromCtx, toCtx), count);
                    rowTotal += count;
                }
                row.put(ActionTelemetrySupport.contextTransitionRowTotalKey(fromCtx), rowTotal);
                for (int toCtx = 0; toCtx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; toCtx++) {
                    long count = r.contextTransitionCounts[fromCtx * ActionTelemetrySupport.CONTEXT_BIN_COUNT + toCtx];
                    row.put(ActionTelemetrySupport.contextTransitionProbKey(fromCtx, toCtx), rowTotal > 0L ? count / (double) rowTotal : 0.0);
                }
            }
        }
        if (r.finalContextCounts != null) {
            long total = 0L;
            for (int ctx = 0; ctx < ActionTelemetrySupport.CONTEXT_BIN_COUNT; ctx++) {
                long count = r.finalContextCounts[ctx];
                row.put(ActionTelemetrySupport.finalContextKey(ctx), count);
                total += count;
            }
            row.put("pred_final_ctx_total_n", total);
        }
        if (r.finalActionCounts != null) {
            long total = 0L;
            for (Action action : Action.values()) {
                long count = r.finalActionCounts[action.ordinal()];
                row.put(ActionTelemetrySupport.finalActionKey(action), count);
                total += count;
            }
            row.put("pred_final_action_total_n", total);
        }
        return row;
    }

    private static long initialCount(double density, long gridCells) {
        return Math.max(0L, Math.round(density * gridCells));
    }
}
