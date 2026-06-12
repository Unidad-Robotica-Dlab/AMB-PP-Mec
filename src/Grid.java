package com.pph.simramjava;

final class Grid {
    static final byte EMPTY = 0;
    static final byte PREY = 1;
    static final byte PRED = 2;

    final int w, h;
    final byte[] cells;   // 1D row-major: y*w + x
    final int[] energy;   // same layout
    final double[] confidence; // Metacognitive state (0.0 to 1.0)
    final int[] agentState; // Stores ContextIndex * 3 + ActionOrdinal for Markov

    Grid(int n){ 
        this.w=n; 
        this.h=n; 
        this.cells=new byte[n*n]; 
        this.energy=new int[n*n]; 
        this.confidence = new double[n*n];
        this.agentState = new int[n*n];
        for(int i=0; i<n*n; i++) {
            this.confidence[i] = 1.0; // Start with full confidence
            this.agentState[i] = -1;
        }
    }

    static Grid create(int size){ return new Grid(size); }

    static int idx(int w, int y, int x){ return y*w + x; }
}
