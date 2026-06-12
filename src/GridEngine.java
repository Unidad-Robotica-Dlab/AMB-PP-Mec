package com.pph.simramjava;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

final class GridEngine {
    static final class Result {
        final int totalEat, deathStarve, deathConflict, deathEaten, birthPrey, birthPred;
        final int spatialBlockPredMove, spatialBlockPreyMove, spatialBlockPreyBirth;
        final int finalPrey, finalPred;
        final Integer tExtPrey, tExtPred;
        final long actionTotal, actionHunt, actionMove, actionStay, actionHuntCapture;
        final long[] actionContextCounts, actionTransitionCounts, contextTransitionCounts, markovTransitions;
        final long[] finalContextCounts, finalActionCounts;
        final long[] temporalContextActionCounts, temporalMarkovTransitions;
        final int temporalBinCount;
        final double cumulativePredEnergy;
        final long totalPredCount;
        final int tailWindowSteps;
        final double preyTailMeanDensity, predTailMeanDensity;
        final double preyTailCv, predTailCv;
        final double preyTailSlope, predTailSlope;

        Result(int totalEat,int deathStarve,int deathConflict,int deathEaten,int birthPrey,int birthPred,
               int spatialBlockPredMove,int spatialBlockPreyMove,int spatialBlockPreyBirth,
               int finalPrey,int finalPred,Integer tExtPrey,Integer tExtPred,
               long actionTotal,long actionHunt,long actionMove,long actionStay,long actionHuntCapture,
               long[] actionContextCounts,long[] actionTransitionCounts, long[] contextTransitionCounts, long[] markovTransitions,
               long[] finalContextCounts, long[] finalActionCounts,
               long[] temporalContextActionCounts, long[] temporalMarkovTransitions, int temporalBinCount,
               double cumulativePredEnergy, long totalPredCount,
               int tailWindowSteps,
               double preyTailMeanDensity, double predTailMeanDensity,
               double preyTailCv, double predTailCv,
               double preyTailSlope, double predTailSlope) {
            this.totalEat=totalEat; this.deathStarve=deathStarve; this.deathConflict=deathConflict; this.deathEaten=deathEaten; this.birthPrey=birthPrey; this.birthPred=birthPred;
            this.spatialBlockPredMove=spatialBlockPredMove; this.spatialBlockPreyMove=spatialBlockPreyMove; this.spatialBlockPreyBirth=spatialBlockPreyBirth;
            this.finalPrey=finalPrey; this.finalPred=finalPred; this.tExtPrey=tExtPrey; this.tExtPred=tExtPred;
            this.actionTotal=actionTotal; this.actionHunt=actionHunt; this.actionMove=actionMove; this.actionStay=actionStay; this.actionHuntCapture=actionHuntCapture;
            this.actionContextCounts=actionContextCounts; this.actionTransitionCounts=actionTransitionCounts;
            this.contextTransitionCounts = contextTransitionCounts;
            this.markovTransitions = markovTransitions;
            this.finalContextCounts = finalContextCounts;
            this.finalActionCounts = finalActionCounts;
            this.temporalContextActionCounts = temporalContextActionCounts;
            this.temporalMarkovTransitions = temporalMarkovTransitions;
            this.temporalBinCount = temporalBinCount;
            this.cumulativePredEnergy = cumulativePredEnergy;
            this.totalPredCount = totalPredCount;
            this.tailWindowSteps = tailWindowSteps;
            this.preyTailMeanDensity = preyTailMeanDensity;
            this.predTailMeanDensity = predTailMeanDensity;
            this.preyTailCv = preyTailCv;
            this.predTailCv = predTailCv;
            this.preyTailSlope = preyTailSlope;
            this.predTailSlope = predTailSlope;
        }
    }

    static final class StepTelemetry {
        final int actionTotal, actionHunt, actionMove, actionStay;
        final int preyBirths, preyLosses, predBirths, predLosses;
        final int preyBlocked, predBlocked, preyBirthBlocked;
        final double preyRStep, predRStep, preyRCumulative, predRCumulative;
        StepTelemetry(
                int actionTotal,
                int actionHunt,
                int actionMove,
                int actionStay,
                int preyBirths,
                int preyLosses,
                int predBirths,
                int predLosses,
                int preyBlocked,
                int predBlocked,
                int preyBirthBlocked,
                double preyRStep,
                double predRStep,
                double preyRCumulative,
                double predRCumulative
        ) {
            this.actionTotal = actionTotal;
            this.actionHunt = actionHunt;
            this.actionMove = actionMove;
            this.actionStay = actionStay;
            this.preyBirths = preyBirths;
            this.preyLosses = preyLosses;
            this.predBirths = predBirths;
            this.predLosses = predLosses;
            this.preyBlocked = preyBlocked;
            this.predBlocked = predBlocked;
            this.preyBirthBlocked = preyBirthBlocked;
            this.preyRStep = preyRStep;
            this.predRStep = predRStep;
            this.preyRCumulative = preyRCumulative;
            this.predRCumulative = predRCumulative;
        }
    }

    interface TickListener {
        void onTick(int step, double preyDensity, double predDensity, StepTelemetry telemetry);
    }
    
    interface FrameListener {
        void onFrame(int step, byte[] cells);
    }

    interface ContinuousListener {
        void onAction(int step, int agentIdx, double opp, double risk, double hunger, double d_norm, Action action, double confidence);
    }

    static Result run(ParamSet p) {
        return run(p, null, null, null);
    }

    static Result run(ParamSet p, TickListener listener) {
        return run(p, listener, null, null);
    }

    static Result run(ParamSet p, TickListener listener, FrameListener frameListener) {
        return run(p, listener, frameListener, null);
    }

    static Result run(ParamSet p, TickListener listener, FrameListener frameListener, ContinuousListener continuousListener) {
        final int size = p.size;
        final int steps = p.steps;
        final Grid grid = Grid.create(size);
        final SplittableRandom rng = new SplittableRandom(p.seed);
        initGrid(grid, p.preyDensity, p.predDensity, rng, p.predEnergyInit);

        final int pr = Math.max(0, p.perceptionRadius);
        final int n = size * size;
        final int[] coords = new int[n];
        for (int i=0;i<n;i++) coords[i]=i;
        final String topo = (p.gridTopology==null?"bounded":p.gridTopology);
        final int[][] neighCache = (pr<=1) ? null : precomputeNeighbors(size, pr, topo);
        final int Emax = Math.max(8, Math.max(p.predEnergyInit + p.predEnergyEat, p.predEnergyRepro));
        final int tailWindow = Math.max(5, (int)Math.ceil(steps * 0.1));
        final double[] preyTail = new double[tailWindow];
        final double[] predTail = new double[tailWindow];
        int tailCount = 0;

        int total_eat=0, death_starve=0, death_conflict=0, death_eaten=0, birth_prey=0, birth_pred=0;
        int spatial_block_pred_move=0, spatial_block_prey_move=0, spatial_block_prey_birth=0;
        double cumulativePredEnergy = 0.0;
        long totalPredCount = 0L;
        Integer t_ext_prey = null, t_ext_pred = null;
        long action_total = 0L, action_hunt = 0L, action_move = 0L, action_stay = 0L, action_hunt_capture = 0L;
        long[] actionContextCounts = p.actionTelemetryMode.includesContext()
                ? new long[ActionTelemetrySupport.CONTEXT_BIN_COUNT * Action.values().length]
                : null;
        long[] actionTransitionCounts = p.actionTelemetryMode.includesGlobal()
                ? new long[Action.values().length * Action.values().length]
                : null;
        long[] contextTransitionCounts = p.actionTelemetryMode.includesContextMarkov()
                ? new long[ActionTelemetrySupport.CONTEXT_BIN_COUNT * ActionTelemetrySupport.CONTEXT_BIN_COUNT]
                : null;
        long[] markovTransitions = p.actionTelemetryMode.includesMarkov()
                ? new long[ActionTelemetrySupport.MARKOV_STATE_COUNT * ActionTelemetrySupport.MARKOV_STATE_COUNT]
                : null;
        final int markovBinCount = p.actionTelemetryMode.getMarkovBinCount();
        long[] temporalContextActionCounts = (markovBinCount > 0)
                ? new long[markovBinCount * ActionTelemetrySupport.CONTEXT_BIN_COUNT * Action.values().length]
                : null;
        long[] temporalMarkovTransitions = (markovBinCount > 0)
                ? new long[markovBinCount * ActionTelemetrySupport.MARKOV_STATE_COUNT * ActionTelemetrySupport.MARKOV_STATE_COUNT]
                : null;
        long[] finalContextCounts = p.actionTelemetryMode.includesMarkov()
                ? new long[ActionTelemetrySupport.CONTEXT_BIN_COUNT]
                : null;
        long[] finalActionCounts = p.actionTelemetryMode.includesMarkov()
                ? new long[Action.values().length]
                : null;

        final byte[] nextCells = new byte[n];
        final int[] nextEnergy = new int[n];
        final double[] nextConfidence = new double[n];
        final int[] nextAgentState = new int[n];
        final byte[] eaten = new byte[n];

        for (int t=1; t<=steps; t++){
            // conteos globales para heurísticas
            int curPrey=0, curPred=0;
            for (int q=0;q<n;q++){
                byte c = grid.cells[q];
                if (c==Grid.PREY) curPrey++; 
                else if (c==Grid.PRED) {
                    curPred++;
                    cumulativePredEnergy += grid.energy[q];
                }
            }
            totalPredCount += curPred;
            double occGlobal = (curPrey + curPred) / (double)Math.max(1, n);
            int stepActionTotal = 0, stepActionHunt = 0, stepActionMove = 0, stepActionStay = 0;
            int stepBirthPrey = 0, stepBirthPred = 0, stepDeathStarve = 0, stepDeathConflict = 0, stepDeathEaten = 0;
            shuffle(coords, rng);
            // limpiar buffers
            for (int i=0;i<n;i++){ nextCells[i]=0; nextEnergy[i]=0; nextConfidence[i]=1.0; eaten[i]=0; nextAgentState[i]=-1; }
            // Depredadores
            for (int k=0;k<n;k++){
                int idx = coords[k];
                if (grid.cells[idx] != Grid.PREY && grid.cells[idx] != Grid.PRED) continue;
                if (grid.cells[idx] == Grid.PREY) continue; // Presas se saltan aquí

                int e0 = grid.energy[idx];
                int e = e0 - 1; 
                if (e <= 0) { death_starve++; stepDeathStarve++; continue; }
                
                int y = idx / size, x = idx % size;
                int[] nb = neighbors(size, pr, y, x, neighCache, topo);
                
                int preyCount=0, emptyCount=0, predCount=0, neighborCount=0;
                int selPreyIdx=-1, selEmptyIdx=-1;
                for (int j=0;j<nb.length;j++){
                    int q = nb[j]; if (q<0) break;
                    neighborCount++;
                    byte c = grid.cells[q];
                    if (c == Grid.PREY){ if (rng.nextInt(++preyCount)==0) selPreyIdx = q; }
                    else if (c == Grid.EMPTY && nextCells[q]==Grid.EMPTY){ if (rng.nextInt(++emptyCount)==0) selEmptyIdx = q; }
                    else if (c == Grid.PRED){ predCount++; }
                }

                int distPrey = (pr > 0 ? pr : 1);
                if (preyCount > 0) {
                    for (int j=0;j<nb.length;j++){
                        int q = nb[j]; if (q<0) break;
                        if (grid.cells[q] == Grid.PREY){
                            int qy = q / size, qx = q % size;
                            int dy = Math.abs(qy - y); int dx = Math.abs(qx - x);
                            if ("toroidal".equalsIgnoreCase(topo)){
                                dy = Math.min(dy, size - dy); dx = Math.min(dx, size - dx);
                            }
                            int d = Math.max(dy, dx);
                            if (d < distPrey) distPrey = d;
                        }
                    }
                    if (distPrey <= 0) distPrey = 1;
                }
                double d_norm = (double) distPrey / (double)Math.max(1, pr);

                double hunger = 1.0 - Math.min(1.0, e0 / (double)Math.max(1, Emax));
                if ("sigmoide".equalsIgnoreCase(p.selfHungerMode)) {
                    hunger = sigmoidHunger(hunger);
                }
                double opp = Math.min(1.0, preyCount / (double)Math.max(1, neighborCount));
                double risk = Math.min(1.0, predCount / (double)Math.max(1, neighborCount));
                
                // Metacognition: Load confidence
                double curConf = grid.confidence[idx];

                Action action;
                boolean baseOnly = !p.mechSelf && !p.mechSitu && !p.mechReflect;
                if (baseOnly) {
                    // Agente base: usa pesos fijos configurados,
                    // o uniforme 1/3 si baseline_mode=uniform.
                    if ("uniform".equalsIgnoreCase(p.baselineMode)) {
                        action = sampleAction(rng, 1.0, 1.0, 1.0, preyCount > 0, emptyCount > 0);
                    } else {
                        action = sampleAction(
                                rng,
                                p.baseWeightHunt,
                                p.baseWeightMove,
                                p.baseWeightStay,
                                preyCount > 0,
                                emptyCount > 0
                        );
                    }
                } else {
                    double sc_hunt = (preyCount>0 ? 1.0 : 0.0);
                    double sc_move = (emptyCount>0 ? 1.0 - occGlobal : 0.0);
                    double sc_stay = 1.0;

                    if (p.mechSelf){
                        String sv = (p.selfVariant == null ? "h_move" : p.selfVariant);
                        if ("h_move".equalsIgnoreCase(sv) || "h_all".equalsIgnoreCase(sv)){ sc_move *= (0.5 + 0.5 * hunger); }
                        if (("h_eat".equalsIgnoreCase(sv) || "h_all".equalsIgnoreCase(sv)) && preyCount > 0){ sc_hunt += 0.5 * hunger; }
                    }
                    if (p.mechSitu){
                        sc_hunt += (p.situOppWeight + p.situHungerWeight * hunger) * opp - p.situRiskWeight * risk - p.situDistWeight * d_norm;
                        sc_move += (p.situOppWeight * 0.5) * opp - (p.situRiskWeight * 0.5) * risk - (p.situDistWeight * 0.4) * d_norm;
                    }

                    double sh = Math.max(0.0, sc_hunt), sm = Math.max(0.0, sc_move), ss = Math.max(0.0, sc_stay);
                    
                    // REFLECT: Resource-Rational Metacognition (Griffiths & Lieder, 2020)
                    if (p.mechReflect) {
                        // System 1: Impulsive action proposal (weights sh, sm, ss)
                        double sum = sh + sm + ss; if (sum <= 0.0){ sh=1.0; sum=1.0; }
                        double u = rng.nextDouble() * sum;
                        Action proposedAction;
                        if (u < sh) proposedAction = Action.HUNT;
                        else if (u < sh + sm) proposedAction = Action.MOVE;
                        else proposedAction = Action.STAY;

                        // System 2: Metacognitive Gating
                        // Value of Information (VOI) is proportional to Uncertainty and Risk
                        double voi = (1.0 - curConf) * Math.max(risk, 0.1); 
                        
                        if (voi > p.reflectThreshold) {
                            // "Stop and Think": Deliberate and override impulse
                            e -= (int)Math.round(p.reflectCost); // Penalty for the luxury of thinking
                            
                            // Deliberative logic: Choose the safest/most convenient path
                            if (risk > 0.4) {
                                action = Action.STAY; // Avoid danger if uncertain
                            } else if (hunger < 0.3 && opp < 0.2) {
                                action = Action.MOVE; // Look for better grounds if not desperate
                            } else {
                                action = proposedAction; // Trust impulse if no clear danger/reason
                            }
                            // Thinking increases internal model consistency (confidence gain)
                            curConf = Math.min(1.0, curConf + p.reflectRate * 0.5);
                        } else {
                            // Act impulsively
                            action = proposedAction;
                        }
                    } else {
                        // Standard Decision
                        double sum = sh + sm + ss; if (sum <= 0.0){ sh=1.0; sum=1.0; }
                        double u = rng.nextDouble() * sum;
                        if (u < sh) action = Action.HUNT;
                        else if (u < sh + sm) action = Action.MOVE;
                        else action = Action.STAY;
                    }
                }

                if (p.actionTelemetryMode.includesGlobal()) {
                    action_total++;
                    stepActionTotal++;
                    switch (action) {
                        case HUNT:
                            action_hunt++;
                            stepActionHunt++;
                            break;
                        case MOVE:
                            action_move++;
                            stepActionMove++;
                            break;
                        case STAY:
                            action_stay++;
                            stepActionStay++;
                            break;
                    }
                }

                if (continuousListener != null) {
                    // Export confidence too for continuous telemetry
                    continuousListener.onAction(t, idx, opp, risk, hunger, d_norm, action, curConf);
                }

                int ctxIdx = ActionTelemetrySupport.contextIndex(preyCount, emptyCount, hunger, risk);
                if (actionContextCounts != null) {
                    actionContextCounts[ctxIdx * Action.values().length + action.ordinal()]++;
                }
                int temporalBin = -1;
                if (temporalContextActionCounts != null) {
                    temporalBin = ActionTelemetrySupport.temporalBin(t, steps, markovBinCount);
                    int temporalContextOffset = temporalBin * ActionTelemetrySupport.CONTEXT_BIN_COUNT * Action.values().length;
                    temporalContextActionCounts[temporalContextOffset + ctxIdx * Action.values().length + action.ordinal()]++;
                }
                
                int currFullState = ActionTelemetrySupport.markovState(ctxIdx, action);
                int prevFullState = grid.agentState[idx];
                if (actionTransitionCounts != null && prevFullState != -1) {
                    int prevActionOrdinal = prevFullState % Action.values().length;
                    actionTransitionCounts[prevActionOrdinal * Action.values().length + action.ordinal()]++;
                }
                if (contextTransitionCounts != null && prevFullState != -1) {
                    int prevCtxIdx = prevFullState / Action.values().length;
                    contextTransitionCounts[prevCtxIdx * ActionTelemetrySupport.CONTEXT_BIN_COUNT + ctxIdx]++;
                }
                if (temporalMarkovTransitions != null) {
                    if (prevFullState != -1) {
                        if (temporalBin < 0) temporalBin = ActionTelemetrySupport.temporalBin(t, steps, markovBinCount);
                        int temporalMarkovOffset = temporalBin
                                * ActionTelemetrySupport.MARKOV_STATE_COUNT
                                * ActionTelemetrySupport.MARKOV_STATE_COUNT;
                        temporalMarkovTransitions[
                                temporalMarkovOffset
                                        + prevFullState * ActionTelemetrySupport.MARKOV_STATE_COUNT
                                        + currFullState
                        ]++;
                    }
                }
                if (markovTransitions != null && prevFullState != -1) {
                    markovTransitions[
                            prevFullState * ActionTelemetrySupport.MARKOV_STATE_COUNT
                                    + currFullState
                    ]++;
                }

                int dest = idx;
                boolean huntSuccess = false;
                if (action == Action.HUNT && preyCount > 0){
                    dest = selPreyIdx; e += p.predEnergyEat;
                    if (eaten[dest]==0){ 
                        eaten[dest]=1; total_eat++; action_hunt_capture++; 
                        huntSuccess = true;
                    }
                } else if (action == Action.MOVE && emptyCount > 0){
                    dest = selEmptyIdx;
                }

                // REFLECT: Update confidence based on Prediction Error
                if (p.mechReflect && action == Action.HUNT) {
                    double expectation = opp; // Simple expectation: opportunity level
                    double result = huntSuccess ? 1.0 : 0.0;
                    double error = Math.abs(result - expectation);
                    // Confidence decreases with error, increases with consistency
                    curConf = curConf * (1.0 - p.reflectRate) + (1.0 - error) * p.reflectRate;
                    curConf = Math.max(0.0, Math.min(1.0, curConf));
                }

                if (nextCells[dest]==Grid.EMPTY){
                    nextCells[dest]=Grid.PRED; nextEnergy[dest]=Math.max(1, e);
                    nextConfidence[dest] = curConf;
                    nextAgentState[dest] = currFullState;
                } else {
                    spatial_block_pred_move++;
                }

                // REPRODUCCIÓN Y HERENCIA
                if (nextCells[dest]==Grid.PRED && nextEnergy[dest] >= p.predEnergyRepro && rng.nextDouble() < p.predRepro){
                    int childSpot=-1, ec=0;
                    for (int j=0;j<nb.length;j++){
                        int q=nb[j]; if (q<0) break;
                        if (nextCells[q]==Grid.EMPTY && grid.cells[q]==Grid.EMPTY){ if (rng.nextInt(++ec)==0) childSpot=q; }
                    }
                    if (childSpot>=0){
                        nextCells[childSpot]=Grid.PRED;
                        int childE = Math.max(1, nextEnergy[dest]/2);
                        nextEnergy[childSpot]=childE;
                        nextEnergy[dest]=Math.max(1, nextEnergy[dest]-childE);
                        nextConfidence[childSpot] = curConf;
                        birth_pred++;
                        stepBirthPred++;
                    }
                }
            }
            // Presas
            for (int k=0;k<n;k++){
                int idx = coords[k];
                if (grid.cells[idx] != Grid.PREY) continue;
                if (eaten[idx]==1){ death_eaten++; stepDeathEaten++; continue; }
                int y = idx / size, x = idx % size;
                int[] nb = neighbors(size, pr, y, x, neighCache, topo);
                int emptyCount=0, selEmpty=-1;
                for (int j=0;j<nb.length;j++){
                    int q=nb[j]; if (q<0) break;
                    if (grid.cells[q]==Grid.EMPTY && nextCells[q]==Grid.EMPTY){ if (rng.nextInt(++emptyCount)==0) selEmpty=q; }
                }
                int dest = (selEmpty>=0 ? selEmpty : idx);
                if (nextCells[dest] != Grid.EMPTY) {
                    // Si el movimiento queda bloqueado, la presa intenta permanecer en su celda original.
                    // Si tampoco puede, la colisión queda contada explícitamente.
                    dest = idx;
                }
                if (nextCells[dest]==Grid.EMPTY){
                    nextCells[dest]=Grid.PREY;
                } else {
                    spatial_block_prey_move++;
                    continue;
                }
                // reproducción
                int empty2Count=0, selEmpty2=-1;
                int dy = dest / size, dx = dest % size;
                int[] nb2 = neighbors(size, pr, dy, dx, neighCache, topo);
                for (int j=0;j<nb2.length;j++){
                    int q=nb2[j]; if (q<0) break;
                    if (nextCells[q]==Grid.EMPTY){ if (rng.nextInt(++empty2Count)==0) selEmpty2=q; }
                }
                if (selEmpty2>=0 && rng.nextDouble() < p.preyRepro){
                    if (nextCells[selEmpty2] == Grid.EMPTY) {
                        nextCells[selEmpty2]=Grid.PREY;
                        birth_prey++;
                        stepBirthPrey++;
                    } else {
                        spatial_block_prey_birth++;
                    }
                }
            }
            // Commit
            int preyCnt=0, predCnt=0;
            for (int q=0;q<n;q++){
                grid.cells[q]=nextCells[q]; grid.energy[q]=nextEnergy[q];
                grid.confidence[q]=nextConfidence[q];
                grid.agentState[q]=nextAgentState[q];
                
                if (grid.cells[q]==Grid.PREY) preyCnt++;
                else if (grid.cells[q]==Grid.PRED) predCnt++;
            }
            if (preyCnt==0 && t_ext_prey==null) t_ext_prey = t;
            if (predCnt==0 && t_ext_pred==null) t_ext_pred = t;
            if (listener != null) {
                int stepPreyLosses = stepDeathEaten;
                int stepPredLosses = stepDeathStarve;
                StepTelemetry stepTelemetry = new StepTelemetry(
                        stepActionTotal,
                        stepActionHunt,
                        stepActionMove,
                        stepActionStay,
                        stepBirthPrey,
                        stepPreyLosses,
                        stepBirthPred,
                        stepPredLosses,
                        spatial_block_prey_move,
                        spatial_block_pred_move,
                        spatial_block_prey_birth,
                        PopulationMetrics.reproductiveRatio(stepBirthPrey, stepPreyLosses),
                        PopulationMetrics.reproductiveRatio(stepBirthPred, stepPredLosses),
                        PopulationMetrics.reproductiveRatio(birth_prey, death_eaten),
                        PopulationMetrics.reproductiveRatio(birth_pred, death_starve)
                );
                listener.onTick(t, preyCnt / (double)n, predCnt / (double)n, stepTelemetry);
            }
            int tailIdx = tailCount % tailWindow;
            preyTail[tailIdx] = preyCnt / (double)n;
            predTail[tailIdx] = predCnt / (double)n;
            tailCount++;
            if (frameListener != null) frameListener.onFrame(t, grid.cells);
        }
        // Final
        int fprey=0, fpred=0;
        if (finalContextCounts != null || finalActionCounts != null) {
            for (int q=0; q<n; q++) {
                if (grid.cells[q] != Grid.PRED) continue;
                int state = grid.agentState[q];
                if (state < 0) continue;
                int ctxIdx = state / Action.values().length;
                int actionIdx = state % Action.values().length;
                if (finalContextCounts != null) finalContextCounts[ctxIdx]++;
                if (finalActionCounts != null) finalActionCounts[actionIdx]++;
            }
        }
        for (byte c: grid.cells){ if (c==Grid.PREY) fprey++; else if (c==Grid.PRED) fpred++; }
        int effectiveTailCount = Math.min(tailCount, tailWindow);
        double[] preyTailSeries = tailSeries(preyTail, effectiveTailCount, tailCount);
        double[] predTailSeries = tailSeries(predTail, effectiveTailCount, tailCount);
        return new Result(
                total_eat, death_starve, death_conflict, death_eaten, birth_prey, birth_pred,
                spatial_block_pred_move, spatial_block_prey_move, spatial_block_prey_birth,
                fprey, fpred, t_ext_prey, t_ext_pred,
                action_total, action_hunt, action_move, action_stay, action_hunt_capture,
                actionContextCounts, actionTransitionCounts, contextTransitionCounts, markovTransitions,
                finalContextCounts, finalActionCounts,
                temporalContextActionCounts, temporalMarkovTransitions, markovBinCount,
                cumulativePredEnergy, totalPredCount,
                effectiveTailCount,
                mean(preyTailSeries), mean(predTailSeries),
                coefficientOfVariation(preyTailSeries), coefficientOfVariation(predTailSeries),
                slope(preyTailSeries), slope(predTailSeries)
        );
    }

    private static double[] tailSeries(double[] ring, int count, int totalSeen) {
        if (count <= 0) return new double[0];
        double[] out = new double[count];
        int start = (totalSeen - count) % ring.length;
        if (start < 0) start += ring.length;
        for (int i = 0; i < count; i++) out[i] = ring[(start + i) % ring.length];
        return out;
    }

    private static double mean(double[] xs) {
        if (xs == null || xs.length == 0) return 0.0;
        double sum = 0.0;
        for (double x : xs) sum += x;
        return sum / xs.length;
    }

    private static double coefficientOfVariation(double[] xs) {
        if (xs == null || xs.length == 0) return 0.0;
        double avg = mean(xs);
        if (avg <= 1e-12) {
            for (double x : xs) {
                if (Math.abs(x) > 1e-12) return Double.POSITIVE_INFINITY;
            }
            return 0.0;
        }
        double var = 0.0;
        for (double x : xs) {
            double d = x - avg;
            var += d * d;
        }
        var /= xs.length;
        return Math.sqrt(var) / avg;
    }

    private static double slope(double[] ys) {
        if (ys == null || ys.length <= 1) return 0.0;
        double mx = (ys.length - 1) / 2.0;
        double my = mean(ys);
        double num = 0.0;
        double den = 0.0;
        for (int i = 0; i < ys.length; i++) {
            double dx = i - mx;
            num += dx * (ys[i] - my);
            den += dx * dx;
        }
        return den > 0.0 ? num / den : 0.0;
    }

    private static double sigmoidHunger(double h) {
        // k=10, x0=0.5
        double s = 1.0 / (1.0 + Math.exp(-10.0 * (h - 0.5)));
        // Normalización para f(0)=0 y f(1)=1
        double s0 = 1.0 / (1.0 + Math.exp(5.0));
        double s1 = 1.0 / (1.0 + Math.exp(-5.0));
        return (s - s0) / (s1 - s0);
    }

    private static void initGrid(Grid grid, double preyDens, double predDens, SplittableRandom rng, int eInit) {

        int n = grid.w * grid.h;
        int nPrey = (int) Math.round(preyDens * n);
        int nPred = (int) Math.round(predDens * n);
        int[] coords = new int[n]; for (int i=0;i<n;i++) coords[i]=i;
        shuffle(coords, rng);

        // Presas primero
        int k=0; int limit=Math.min(nPrey, n);
        for (; k<limit; k++){ grid.cells[coords[k]] = Grid.PREY; }
        // Depredadores en vacíos
        int placed=0; int i=k;
        while (placed < nPred && i < n){ 
            int q=coords[i++]; 
            if (grid.cells[q]==Grid.EMPTY){
                grid.cells[q]=Grid.PRED; 
                grid.energy[q]=eInit; 
                placed++; 
            }
        }
    }


    private static int[][] precomputeNeighbors(int size, int r, String topo){
        int n = size*size; int[][] out = new int[n][];
        for (int y=0;y<size;y++){
            for (int x=0;x<size;x++){
                List<Integer> nb = new ArrayList<>();
                if (r<=0){ // 4 vecinos
                    addNeighbor(size, y, x-1, topo, nb);
                    addNeighbor(size, y, x+1, topo, nb);
                    addNeighbor(size, y-1, x, topo, nb);
                    addNeighbor(size, y+1, x, topo, nb);
                } else {
                    for (int dy=-r; dy<=r; dy++){
                        for (int dx=-r; dx<=r; dx++){
                            if (dx==0 && dy==0) continue;
                            if (Math.max(Math.abs(dx),Math.abs(dy))>r) continue;
                            addNeighbor(size, y+dy, x+dx, topo, nb);
                        }
                    }
                }
                // build array with sentinel -1
                int[] arr = new int[nb.size()+1];
                for (int i=0;i<nb.size();i++) arr[i]=nb.get(i);
                arr[nb.size()] = -1; // sentinel
                out[Grid.idx(size,y,x)] = arr;
            }
        }
        return out;
    }

    private static void addNeighbor(int size, int ny, int nx, String topo, List<Integer> acc){
        // Map coordinates according to topology; for bounded, drop ooB; for toroidal, wrap; for reflect, mirror
        int my = mapCoord(size, ny, topo);
        int mx = mapCoord(size, nx, topo);
        if (my < 0 || mx < 0) return; // bounded drop
        int idx = Grid.idx(size, my, mx);
        // prevent duplicates
        if (!acc.contains(idx)) acc.add(idx);
    }

    private static int mapCoord(int size, int v, String topo){
        if ("toroidal".equalsIgnoreCase(topo)){
            int m = v % size; if (m < 0) m += size; return m;
        } else if ("reflect".equalsIgnoreCase(topo) || "reflective".equalsIgnoreCase(topo)){
            if (size <= 1) return 0;
            int period = 2*size - 2;
            int t = v % period; if (t < 0) t += period;
            return (t < size) ? t : (period - t);
        } else { // bounded
            return (0 <= v && v < size) ? v : -1;
        }
    }

    private static int[] neighbors(int size, int r, int y, int x, int[][] cache, String topo){
        if (cache != null) return cache[Grid.idx(size,y,x)];
        if (r <= 0){
            int[] tmp = new int[5]; int n=0; // sentinel -1
            // 4-neigh
            int my = mapCoord(size, y, topo);
            int mxL = mapCoord(size, x-1, topo); if (mxL>=0 && my>=0) tmp[n++] = Grid.idx(size,my,mxL);
            int mxR = mapCoord(size, x+1, topo); if (mxR>=0 && my>=0) tmp[n++] = Grid.idx(size,my,mxR);
            int myU = mapCoord(size, y-1, topo); int mx = mapCoord(size, x, topo); if (mx>=0 && myU>=0) tmp[n++] = Grid.idx(size,myU,mx);
            int myD = mapCoord(size, y+1, topo); if (mx>=0 && myD>=0) tmp[n++] = Grid.idx(size,myD,mx);
            tmp[n] = -1; return tmp;
        } else {
            List<Integer> nb = new ArrayList<>();
            for (int dy=-r; dy<=r; dy++){
                for (int dx=-r; dx<=r; dx++){
                    if (dx==0 && dy==0) continue;
                    if (Math.max(Math.abs(dx),Math.abs(dy))>r) continue;
                    addNeighbor(size, y+dy, x+dx, topo, nb);
                }
            }
            int[] arr = new int[nb.size()+1];
            for (int i=0;i<nb.size();i++) arr[i]=nb.get(i);
            arr[nb.size()] = -1; return arr;
        }
    }

    private static void shuffle(int[] a, SplittableRandom rng){
        for (int i=a.length-1;i>0;i--){ int j = rng.nextInt(i+1); int tmp=a[i]; a[i]=a[j]; a[j]=tmp; }
    }

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
}
