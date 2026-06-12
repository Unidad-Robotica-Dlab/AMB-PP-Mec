package com.pph.simramjava;

import java.nio.file.Path;
import java.util.*;

public final class Cli {
    public static void main(String[] args) throws Exception {
        Args a = Args.parse(args);
        if (a.configs.isEmpty()){
            System.err.println("Uso: run.sh --config <path.json> [--config <path2.json>] [--workers N] [--max-runs N] [--save-summary file] [--save-rows file] [--action-telemetry none|global|context|temporal|markov|markov_temporal|dual_markov|continuous]");
            System.exit(2);
        }
        final String variant = "grid"; // único engine en este port
        final List<PlanItem> plan = new ArrayList<>();
        int totalPlanned = 0; // estimado global
        // 1) Pre-scan: cargar configs, filtrar capacidad y generar param sets
        for (Path cfgPath : a.configs){
            Config cfg = Config.load(cfgPath);
            double occ = cfg.params.preyDensity + cfg.params.predDensity;
            String label = (a.sessionPrefix + "_" + stripExt(cfgPath.getFileName().toString()));
            if (occ > 1.0) {
                System.out.printf("[SKIP] %s (%s) por capacidad: prey+pred=%.2f > 1.0\n", label, cfgPath.getFileName(), occ);
                continue;
            }
            List<ParamSet> ps = ParamSetGenerator.generate(cfg, a.maxRuns, a.actionTelemetryMode);
            plan.add(new PlanItem(label, cfgPath, ps));
            totalPlanned += ps.size();
        }
        if (totalPlanned > 0 && a.printProgress) {
            System.out.printf("[GLOBAL] Corridas planificadas (estimado): %d\n", totalPlanned);
        }
        // 2) Ejecutar plan con barra global (ETA total)
        final long globalStart = System.nanoTime();
        final int totalPlannedFinal = totalPlanned; // effectively final
        final int[] runsDone = {0};
        final long[] lastProgressPrintNs = {0L};
        final int[] lastProgressPrintedRuns = {-1};
        for (PlanItem it : plan){
            System.out.printf("\n=== JAVA session: %s (%s) === planned_runs=%d\n", it.label, it.path.getFileName(), it.ps.size());
            SessionRunner.Progress cb = () -> {
                runsDone[0]++;
                if (a.printProgress && totalPlannedFinal > 0){
                    long now = System.nanoTime();
                    boolean shouldPrint = runsDone[0] == totalPlannedFinal
                            || lastProgressPrintedRuns[0] < 0
                            || (now - lastProgressPrintNs[0]) >= 250_000_000L;
                    if (shouldPrint) {
                        lastProgressPrintNs[0] = now;
                        lastProgressPrintedRuns[0] = runsDone[0];
                        double ratio = Math.min(1.0, Math.max(0.0, runsDone[0] / (double) totalPlannedFinal));
                        String bar = progressBar(ratio, 40);
                        double elapsed = (now - globalStart) / 1e9;
                        double rate = runsDone[0] / Math.max(1e-9, elapsed);
                        int remaining = Math.max(0, totalPlannedFinal - runsDone[0]);
                        int eta = (int) Math.round(remaining / Math.max(1e-9, rate));
                        String line = String.format("\r[GLOBAL] %s %d/%d (%.1f%%) | %.2f runs/s | ETA %02d:%02d",
                                bar, runsDone[0], totalPlannedFinal, 100.0 * ratio, rate, eta / 60, eta % 60);
                        System.out.print(line);
                        if (runsDone[0] == totalPlannedFinal) {
                            System.out.print('\n');
                        }
                    }
                }
            };
            boolean timelineEnabled = a.enableTimeline || hasTemporalTelemetry(it.ps);
            SessionRunner.SessionOut sess = SessionRunner.run(a.outDir, it.label, variant, it.ps, a.workers, cb, timelineEnabled, a.enableVideo);
            List<Map<String,Object>> rows = sess.rows();
            if (rows.isEmpty()){
                System.out.println("  -> [WARN] sesión sin corridas");
                continue;
            }
            if (a.saveRows != null) {
                Csv.append(a.saveRows, rows);
            }
            Map<String,Object> sum = Analysis.summarize(rows, it.label, variant);
            if (a.saveSummary != null) Csv.append(a.saveSummary, Collections.singletonList(sum));
            System.out.printf("\n  -> total_runs=%d | p_coexist_end=%.3f | p_viable_hard=%.3f | p_viable_robust=%.3f | p_ext_prey=%.3f\n",
                    ((Number)sum.get("total_runs")).intValue(),
                    ((Number)sum.get("p_coexist_end")).doubleValue(),
                    ((Number)sum.get("p_viable_hard")).doubleValue(),
                    ((Number)sum.get("p_viable_robust")).doubleValue(),
                    ((Number)sum.get("p_ext_prey")).doubleValue());
        }
        if (a.printProgress && totalPlannedFinal > 0) {
            double elapsed = (System.nanoTime()-globalStart)/1e9;
            double rate = runsDone[0] / Math.max(1e-9, elapsed);
            System.out.printf("[GLOBAL] Finalizado: runs=%d/%d | tiempo=%.2fs | tasa=%.2f runs/s\n", runsDone[0], totalPlannedFinal, elapsed, rate);
        }
        System.out.println("Hecho.");
    }

    private static String stripExt(String s){ int i=s.lastIndexOf('.'); return (i>=0? s.substring(0,i):s); }
    private static String progressBar(double r, int w){ int filled=(int)Math.floor(w*r); StringBuilder sb=new StringBuilder("["); for(int i=0;i<w;i++) sb.append(i<filled?'#':'-'); sb.append("]"); return sb.toString(); }

    private static boolean hasTemporalTelemetry(List<ParamSet> ps){
        for (ParamSet p : ps) {
            if (p.actionTelemetryMode.includesTemporal()) return true;
        }
        return false;
    }

    static final class Args {
        final List<Path> configs = new ArrayList<>();
        String sessionPrefix = "JRAM";
        Integer maxRuns = null;
        int workers = Math.max(1, Runtime.getRuntime().availableProcessors());
        Path saveSummary = null;
        Path saveRows = null;
        Path outDir = Path.of("out");
        boolean printProgress = true;
        boolean enableTimeline = false;
        boolean enableVideo = false;
        ActionTelemetryMode actionTelemetryMode = null;

        static Args parse(String[] argv){
            Args a = new Args();
            for (int i=0;i<argv.length;i++){
                String tok = argv[i];
                switch (tok){
                    case "--config": a.configs.add(Path.of(argv[++i])); break;
                    case "--out-dir": a.outDir = Path.of(argv[++i]); break;
                    case "--workers": a.workers = Integer.parseInt(argv[++i]); if (a.workers<=0) a.workers = Math.max(1, Runtime.getRuntime().availableProcessors()); break;
                    case "--max-runs": a.maxRuns = Integer.parseInt(argv[++i]); break;
                    case "--save-summary": a.saveSummary = Path.of(argv[++i]); break;
                    case "--save-rows": a.saveRows = Path.of(argv[++i]); break;
                    case "--session-prefix": a.sessionPrefix = argv[++i]; break;
                    case "--no-progress": a.printProgress = false; break;
                    case "--timeline": a.enableTimeline = true; break;
                    case "--video": a.enableVideo = true; break;
                    case "--action-telemetry": a.actionTelemetryMode = ActionTelemetryMode.parse(argv[++i]); break;
                    default: throw new IllegalArgumentException("Flag desconocido: "+tok);
                }
            }
            return a;
        }
    }

    private static final class PlanItem {
        final String label; final Path path; final List<ParamSet> ps;
        PlanItem(String label, Path path, List<ParamSet> ps){ this.label=label; this.path=path; this.ps=ps; }
    }
}
