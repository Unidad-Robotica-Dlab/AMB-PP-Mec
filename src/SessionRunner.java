package com.pph.simramjava;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;

final class SessionRunner {
    static final class SessionOut {
        final String label, variant;
        final List<RunResult> runs;
        SessionOut(String label, String variant, List<RunResult> runs){ this.label=label; this.variant=variant; this.runs=runs; }
        List<Map<String,Object>> rows(){
            List<Map<String,Object>> out = new ArrayList<>(runs.size());
            for (RunResult r: runs){ out.add(r.toRow(label, variant)); }
            return out;
        }
    }

    static SessionOut run(java.nio.file.Path baseDir, String label, String variant, List<ParamSet> ps, int workers, Progress cb, boolean enableTimeline, boolean enableVideo){
        int n = ps.size();
        List<RunResult> out = new ArrayList<>(n);
        
        final java.nio.file.Path timelineDir = enableTimeline ? baseDir.resolve("serietiempo") : null;
        final java.nio.file.Path videoDir = enableVideo ? baseDir.resolve("videos") : null;
        
        try {
            if (enableTimeline) java.nio.file.Files.createDirectories(timelineDir);
            if (enableVideo) java.nio.file.Files.createDirectories(videoDir);
        } catch (Exception e) { throw new RuntimeException(e); }

        if (workers <= 1){
            for (int i=0;i<n;i++){
                final String runId = String.format("run_%05d", i);
                ParamSet p = ps.get(i);
                GridEngine.Result r = runWithObservers(p, runId, label, timelineDir, videoDir);
                out.add(new RunResult(runId, p, r));
                if (cb != null) cb.step();
            }
        } else {
            ExecutorService ex = Executors.newFixedThreadPool(workers);
            List<Future<RunResult>> futs = new ArrayList<>(n);
            for (int i=0;i<n;i++){
                final int idx=i; final ParamSet p = ps.get(i);
                final String runId = String.format("run_%05d", idx);
                futs.add(ex.submit(() -> new RunResult(runId, p, runWithObservers(p, runId, label, timelineDir, videoDir))));
            }
            for (Future<RunResult> f : futs){ try { out.add(f.get()); if (cb != null) cb.step(); } catch (Exception e){ throw new RuntimeException(e); } }
            ex.shutdown();
        }
        writeTemporalMarkov(baseDir, label, out);
        return new SessionOut(label, variant, out);
    }

    private static void writeTemporalMarkov(java.nio.file.Path baseDir, String label, List<RunResult> runs) {
        int recordCount = 0;
        int firstBinCount = -1;
        for (RunResult rr : runs) {
            if (rr.r.temporalContextActionCounts != null && rr.r.temporalMarkovTransitions != null) {
                recordCount++;
                if (firstBinCount == -1) firstBinCount = rr.r.temporalBinCount;
            }
        }
        if (recordCount == 0) return;

        java.nio.file.Path path = baseDir.resolve("telemetria_markov").resolve(label + ".mtel.gz");
        try {
            java.nio.file.Files.createDirectories(path.getParent());
            try (java.io.DataOutputStream dos = new java.io.DataOutputStream(
                    new java.util.zip.GZIPOutputStream(
                            new java.io.BufferedOutputStream(
                                    new java.io.FileOutputStream(path.toFile()))))) {
                writeUtf8(dos, "SIMRAM_MARKOV_TEMPORAL");
                dos.writeInt(1); // format version
                dos.writeInt(recordCount);
                dos.writeInt(firstBinCount);
                dos.writeInt(ActionTelemetrySupport.CONTEXT_BIN_COUNT);
                dos.writeInt(Action.values().length);
                dos.writeInt(ActionTelemetrySupport.MARKOV_STATE_COUNT);
                for (RunResult rr : runs) {
                    if (rr.r.temporalContextActionCounts == null || rr.r.temporalMarkovTransitions == null) {
                        continue;
                    }
                    if (rr.r.temporalBinCount != firstBinCount) {
                        // Inconsistent bin count in the same session, we skip to avoid breaking the file format
                        continue;
                    }
                    writeUtf8(dos, rr.runId);
                    dos.writeInt(rr.p.size);
                    dos.writeInt(rr.p.steps);
                    dos.writeInt(rr.p.perceptionRadius);
                    dos.writeLong(rr.p.seed);
                    dos.writeInt(rr.p.baseIndex);
                    dos.writeInt(rr.p.replicaIndex);
                    dos.writeInt(rr.p.replicasTotal);
                    dos.writeInt(rr.r.temporalBinCount);
                    dos.writeInt(rr.r.temporalContextActionCounts.length);
                    for (long v : rr.r.temporalContextActionCounts) dos.writeLong(v);
                    dos.writeInt(rr.r.temporalMarkovTransitions.length);
                    for (long v : rr.r.temporalMarkovTransitions) dos.writeLong(v);
                }
            }
            System.out.println("[MTEL] " + path + " (+" + recordCount + " corridas)");
        } catch (Exception e) {
            throw new RuntimeException("No se pudo escribir telemetría Markov en " + path, e);
        }
    }

    private static void writeUtf8(java.io.DataOutputStream dos, String s) throws java.io.IOException {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }

    private static GridEngine.Result runWithObservers(ParamSet p, String runId, String label, java.nio.file.Path tlDir, java.nio.file.Path vidDir) {
        GridEngine.TickListener tl = null;
        GridEngine.FrameListener fl = null;
        GridEngine.ContinuousListener cl = null;
        
        // Carpeta especifica para esta configuracion de la exploracion
        final java.nio.file.Path configVidDir = vidDir != null ? vidDir.resolve(label) : null;
        final java.nio.file.Path configTimelineDir = tlDir != null ? tlDir.resolve(label).resolve("timelines") : null;
        final java.nio.file.Path configContexDir = tlDir != null ? tlDir.resolve(label).resolve("contex") : null;
        if (configVidDir != null) {
            try { java.nio.file.Files.createDirectories(configVidDir); } catch (Exception e) {}
        }
        if (configTimelineDir != null) {
            try { java.nio.file.Files.createDirectories(configTimelineDir); } catch (Exception e) {}
        }
        if (configContexDir != null) {
            try { java.nio.file.Files.createDirectories(configContexDir); } catch (Exception e) {}
        }

        // Observador de Cronologia (CSV)
        final boolean temporalActions = p.actionTelemetryMode.includesTemporal();
        final StringBuilder sb = (tlDir != null)
                ? new StringBuilder(temporalActions
                ? "step,preyDensity,predDensity,preyBirths,preyLosses,predBirths,predLosses,preyRStep,predRStep,preyRCumulative,predRCumulative,actionTotal,actionHunt,actionMove,actionStay,actionHuntShare,actionMoveShare,actionStayShare\n"
                : "step,preyDensity,predDensity,preyBirths,preyLosses,predBirths,predLosses,preyRStep,predRStep,preyRCumulative,predRCumulative\n")
                : null;
        if (tlDir != null) {
            tl = (step, prey, pred, telemetry) -> {
                sb.append(step)
                        .append(",").append(String.format(Locale.ROOT, "%.6f", prey))
                        .append(",").append(String.format(Locale.ROOT, "%.6f", pred))
                        .append(",").append(telemetry == null ? 0 : telemetry.preyBirths)
                        .append(",").append(telemetry == null ? 0 : telemetry.preyLosses)
                        .append(",").append(telemetry == null ? 0 : telemetry.predBirths)
                        .append(",").append(telemetry == null ? 0 : telemetry.predLosses)
                        .append(",").append(String.format(Locale.ROOT, "%.6f", telemetry == null ? 1.0 : telemetry.preyRStep))
                        .append(",").append(String.format(Locale.ROOT, "%.6f", telemetry == null ? 1.0 : telemetry.predRStep))
                        .append(",").append(String.format(Locale.ROOT, "%.6f", telemetry == null ? 1.0 : telemetry.preyRCumulative))
                        .append(",").append(String.format(Locale.ROOT, "%.6f", telemetry == null ? 1.0 : telemetry.predRCumulative));
                if (temporalActions) {
                    int total = telemetry == null ? 0 : telemetry.actionTotal;
                    int hunt = telemetry == null ? 0 : telemetry.actionHunt;
                    int move = telemetry == null ? 0 : telemetry.actionMove;
                    int stay = telemetry == null ? 0 : telemetry.actionStay;
                    double denom = Math.max(1, total);
                    sb.append(",").append(total)
                            .append(",").append(hunt)
                            .append(",").append(move)
                            .append(",").append(stay)
                            .append(",").append(String.format(Locale.ROOT, "%.6f", hunt / denom))
                            .append(",").append(String.format(Locale.ROOT, "%.6f", move / denom))
                            .append(",").append(String.format(Locale.ROOT, "%.6f", stay / denom));
                }
                sb.append("\n");
            };
        }
        
        // Observador de Video (Binary GZIP) - Replica 0
        final java.util.zip.GZIPOutputStream gzos;
        if (configVidDir != null && p.replicaIndex == 0) {
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(configVidDir.resolve(runId + ".bin.gz").toFile());
                gzos = new java.util.zip.GZIPOutputStream(new java.io.BufferedOutputStream(fos));
                fl = (step, cells) -> {
                    try { gzos.write(cells); } catch (Exception e) {}
                };
            } catch (Exception e) { throw new RuntimeException(e); }
        } else {
            gzos = null;
        }

        // Observador de Contexto Continuo (Binary GZIP)
        final java.io.DataOutputStream dos;
        if (configContexDir != null && p.actionTelemetryMode.includesContinuous()) {
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(configContexDir.resolve(runId + ".contex.gz").toFile());
                java.util.zip.GZIPOutputStream gzosC = new java.util.zip.GZIPOutputStream(new java.io.BufferedOutputStream(fos));
                dos = new java.io.DataOutputStream(gzosC);
                cl = (step, agentIdx, opp, risk, hunger, d_norm, action, confidence) -> {
                    try {
                        dos.writeInt(step);
                        dos.writeInt(agentIdx);
                        dos.writeFloat((float)opp);
                        dos.writeFloat((float)risk);
                        dos.writeFloat((float)hunger);
                        dos.writeFloat((float)d_norm);
                        dos.writeFloat((float)confidence);
                        dos.writeByte((byte)action.ordinal());
                    } catch (Exception e) {}
                };
            } catch (Exception e) { throw new RuntimeException(e); }
        } else {
            dos = null;
        }
        
        GridEngine.Result res = GridEngine.run(p, tl, fl, cl);
        
        // Guardar CSV si aplica
        if (sb != null && configTimelineDir != null) {
            try { java.nio.file.Files.writeString(configTimelineDir.resolve(runId + ".csv"), sb.toString()); } catch (Exception e) {}
        }
        
        // Cerrar stream de video si aplica
        if (gzos != null) {
            try { gzos.finish(); gzos.close(); } catch (Exception e) {}
        }

        // Cerrar stream de contexto si aplica
        if (dos != null) {
            try { dos.close(); } catch (Exception e) {}
        }
        
        return res;
    }

    interface Progress { void step(); }
}
