package com.pph.simramjava;

final class PopulationMetrics {
    private PopulationMetrics() {}

    static double reproductiveRatio(double births, double losses) {
        return (births + losses) > 0.0 ? (births / Math.max(1e-6, losses)) : 1.0;
    }

    static double shannonEntropy(long nHunt, long nMove, long nStay) {
        long total = nHunt + nMove + nStay;
        if (total <= 0) return 0.0;
        double pHunt = nHunt / (double) total;
        double pMove = nMove / (double) total;
        double pStay = nStay / (double) total;
        double entropy = 0.0;
        if (pHunt > 0) entropy -= pHunt * (Math.log(pHunt) / Math.log(2));
        if (pMove > 0) entropy -= pMove * (Math.log(pMove) / Math.log(2));
        if (pStay > 0) entropy -= pStay * (Math.log(pStay) / Math.log(2));
        return entropy;
    }
}
