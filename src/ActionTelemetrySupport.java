package com.pph.simramjava;

final class ActionTelemetrySupport {
    static final int CONTEXT_BIN_COUNT = 16;
    static final int TEMPORAL_BIN_COUNT = 10;
    static final double HUNGER_HIGH_THRESHOLD = 0.5;
    static final double RISK_HIGH_THRESHOLD = 0.5;
    static final int MARKOV_STATE_COUNT = CONTEXT_BIN_COUNT * Action.values().length;
    private static final String[] CONTEXT_LABELS = new String[CONTEXT_BIN_COUNT];
    private static final String[] CONTEXT_TOTAL_KEYS = new String[CONTEXT_BIN_COUNT];
    private static final String[][] CONTEXT_ACTION_KEYS = new String[CONTEXT_BIN_COUNT][Action.values().length];
    private static final String[][] CONTEXT_ACTION_SHARE_KEYS = new String[CONTEXT_BIN_COUNT][Action.values().length];
    private static final String[][] CONTEXT_TRANSITION_KEYS = new String[CONTEXT_BIN_COUNT][CONTEXT_BIN_COUNT];
    private static final String[][] CONTEXT_TRANSITION_PROB_KEYS = new String[CONTEXT_BIN_COUNT][CONTEXT_BIN_COUNT];
    private static final String[] CONTEXT_TRANSITION_ROW_TOTAL_KEYS = new String[CONTEXT_BIN_COUNT];
    private static final String[][] TRANSITION_KEYS = new String[Action.values().length][Action.values().length];
    private static final String[][] TRANSITION_PROB_KEYS = new String[Action.values().length][Action.values().length];
    private static final String[] TRANSITION_ROW_TOTAL_KEYS = new String[Action.values().length];
    private static final String[] FINAL_CONTEXT_KEYS = new String[CONTEXT_BIN_COUNT];
    private static final String[] FINAL_ACTION_KEYS = new String[Action.values().length];
    
    private static final String[] MARKOV_STATE_LABELS = new String[MARKOV_STATE_COUNT];
    private static final String[][] MARKOV_TRANSITION_KEYS = new String[MARKOV_STATE_COUNT][MARKOV_STATE_COUNT];

    static {
        for (int idx = 0; idx < CONTEXT_BIN_COUNT; idx++) {
            int prey = ((idx & 1) != 0) ? 1 : 0;
            int empty = ((idx & 2) != 0) ? 1 : 0;
            int hunger = ((idx & 4) != 0) ? 1 : 0;
            int risk = ((idx & 8) != 0) ? 1 : 0;
            String label = contextLabel(prey, empty, hunger, risk);
            CONTEXT_LABELS[idx] = label;
            CONTEXT_TOTAL_KEYS[idx] = label + "_total_n";
            CONTEXT_TRANSITION_ROW_TOTAL_KEYS[idx] = "context_markov_" + label + "_row_total_n";
            FINAL_CONTEXT_KEYS[idx] = "pred_final_ctx_" + label + "_n";
            for (Action action : Action.values()) {
                String actionName = action.name().toLowerCase();
                CONTEXT_ACTION_KEYS[idx][action.ordinal()] = label + "_" + actionName + "_n";
                CONTEXT_ACTION_SHARE_KEYS[idx][action.ordinal()] = label + "_" + actionName + "_share";
                
                int stateIdx = idx * Action.values().length + action.ordinal();
                MARKOV_STATE_LABELS[stateIdx] = "c" + idx + "_" + actionName;
            }
            for (int j = 0; j < CONTEXT_BIN_COUNT; j++) {
                String toLabel = contextLabel(
                        ((j & 1) != 0) ? 1 : 0,
                        ((j & 2) != 0) ? 1 : 0,
                        ((j & 4) != 0) ? 1 : 0,
                        ((j & 8) != 0) ? 1 : 0
                );
                CONTEXT_TRANSITION_KEYS[idx][j] = "context_markov_" + label + "_to_" + toLabel + "_n";
                CONTEXT_TRANSITION_PROB_KEYS[idx][j] = "context_markov_" + label + "_to_" + toLabel + "_p";
            }
        }
        for (Action from : Action.values()) {
            String fromName = from.name().toLowerCase();
            TRANSITION_ROW_TOTAL_KEYS[from.ordinal()] = "markov_" + fromName + "_row_total_n";
            FINAL_ACTION_KEYS[from.ordinal()] = "pred_final_action_" + fromName + "_n";
            for (Action to : Action.values()) {
                String toName = to.name().toLowerCase();
                TRANSITION_KEYS[from.ordinal()][to.ordinal()] = "markov_" + fromName + "_to_" + toName + "_n";
                TRANSITION_PROB_KEYS[from.ordinal()][to.ordinal()] = "markov_" + fromName + "_to_" + toName + "_p";
            }
        }
        for (int i = 0; i < MARKOV_STATE_COUNT; i++) {
            for (int j = 0; j < MARKOV_STATE_COUNT; j++) {
                MARKOV_TRANSITION_KEYS[i][j] = "markov_v2_" + MARKOV_STATE_LABELS[i] + "_to_" + MARKOV_STATE_LABELS[j] + "_n";
            }
        }
    }

    private ActionTelemetrySupport() {}

    static int contextIndex(int preyCount, int emptyCount, double hunger, double risk) {
        int idx = 0;
        if (preyCount > 0) idx |= 1;
        if (emptyCount > 0) idx |= 2;
        if (hunger >= HUNGER_HIGH_THRESHOLD) idx |= 4;
        if (risk >= RISK_HIGH_THRESHOLD) idx |= 8;
        return idx;
    }

    static int markovState(int ctxIdx, Action action) {
        return ctxIdx * Action.values().length + action.ordinal();
    }

    static int temporalBin(int step, int totalSteps, int totalBins) {
        if (totalBins <= 1) return 0;
        int safeSteps = Math.max(1, totalSteps);
        int bin = (int)(((long)Math.max(1, step) - 1L) * totalBins / safeSteps);
        if (bin < 0) return 0;
        if (bin >= totalBins) return totalBins - 1;
        return bin;
    }

    static String markovKey(int fromState, int toState) {
        return MARKOV_TRANSITION_KEYS[fromState][toState];
    }

    static String contextLabel(int idx) {
        return CONTEXT_LABELS[idx];
    }

    private static String contextLabel(int prey, int empty, int hunger, int risk) {
        return String.format("act_ctx_p%d_e%d_h%d_r%d", prey, empty, hunger, risk);
    }

    static String totalKey(int idx) {
        return CONTEXT_TOTAL_KEYS[idx];
    }

    static String actionKey(int idx, Action action) {
        return CONTEXT_ACTION_KEYS[idx][action.ordinal()];
    }

    static String actionShareKey(int idx, Action action) {
        return CONTEXT_ACTION_SHARE_KEYS[idx][action.ordinal()];
    }

    static String contextTransitionKey(int fromCtx, int toCtx) {
        return CONTEXT_TRANSITION_KEYS[fromCtx][toCtx];
    }

    static String contextTransitionProbKey(int fromCtx, int toCtx) {
        return CONTEXT_TRANSITION_PROB_KEYS[fromCtx][toCtx];
    }

    static String contextTransitionRowTotalKey(int fromCtx) {
        return CONTEXT_TRANSITION_ROW_TOTAL_KEYS[fromCtx];
    }

    static String transitionKey(Action from, Action to) {
        return TRANSITION_KEYS[from.ordinal()][to.ordinal()];
    }

    static String transitionProbKey(Action from, Action to) {
        return TRANSITION_PROB_KEYS[from.ordinal()][to.ordinal()];
    }

    static String transitionRowTotalKey(Action from) {
        return TRANSITION_ROW_TOTAL_KEYS[from.ordinal()];
    }

    static String finalContextKey(int idx) {
        return FINAL_CONTEXT_KEYS[idx];
    }

    static String finalActionKey(Action action) {
        return FINAL_ACTION_KEYS[action.ordinal()];
    }

    static String markovStateLabel(int stateIdx) {
        return MARKOV_STATE_LABELS[stateIdx];
    }
}
