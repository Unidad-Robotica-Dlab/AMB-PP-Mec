package com.pph.simramjava;

enum ActionTelemetryMode {
    NONE,
    GLOBAL,
    CONTEXT,
    TEMPORAL,
    MARKOV,
    MARKOV_TEMPORAL,
    DUAL_MARKOV,
    CONTINUOUS;

    static ActionTelemetryMode parse(Object raw) {
        if (raw == null) {
            return NONE;
        }
        String s = String.valueOf(raw).trim().toUpperCase();
        if (s.isEmpty()) {
            return NONE;
        }
        if ("0".equals(s) || "OFF".equals(s) || "DISABLED".equals(s)) {
            return NONE;
        }
        if ("1".equals(s) || "BASIC".equals(s)) {
            return GLOBAL;
        }
        if ("2".equals(s) || "CONTEXTUAL".equals(s)) {
            return CONTEXT;
        }
        if ("3".equals(s) || "FULL".equals(s)) {
            return TEMPORAL;
        }
        if ("4".equals(s) || "MARKOV".equals(s)) {
            return MARKOV;
        }
        if ("5".equals(s) || "MARKOV_TEMPORAL".equals(s) || "TEMPORAL_MARKOV".equals(s)
                || "MARKOV10".equals(s) || "MARKOV_TEMPORAL10".equals(s)) {
            return MARKOV_TEMPORAL;
        }
        if ("6".equals(s) || "CONTINUOUS".equals(s)) {
            return CONTINUOUS;
        }
        if ("7".equals(s) || "DUAL_MARKOV".equals(s) || "DUALMARKOV".equals(s)) {
            return DUAL_MARKOV;
        }
        try {
            return ActionTelemetryMode.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }

    boolean includesGlobal() {
        return this != NONE;
    }

    boolean includesContext() {
        return this == CONTEXT || this == TEMPORAL || this == MARKOV || this == MARKOV_TEMPORAL || this == DUAL_MARKOV || this == CONTINUOUS;
    }

    boolean includesTemporal() {
        return this == TEMPORAL;
    }

    boolean includesMarkov() {
        return this == MARKOV || this == MARKOV_TEMPORAL || this == DUAL_MARKOV;
    }

    boolean includesContextMarkov() {
        return this == MARKOV || this == MARKOV_TEMPORAL || this == DUAL_MARKOV;
    }

    boolean isMarkovTemporal() {
        return this == MARKOV_TEMPORAL;
    }

    int getMarkovBinCount() {
        if (this == MARKOV) return 1;
        if (this == MARKOV_TEMPORAL) return ActionTelemetrySupport.TEMPORAL_BIN_COUNT;
        if (this == DUAL_MARKOV) return 1;
        return 0;
    }

    boolean includesContinuous() {
        return this == CONTINUOUS;
    }
}
