package org.openflexo.pamela.editor.model;

import java.util.EnumMap;
import java.util.Map;

import spoon.support.compiler.SpoonProgress;

/**
 * Bridges Spoon's {@link SpoonProgress} per-file callbacks to a
 * {@link BuildProgressListener}, computing a single monotonic overall fraction.
 *
 * <p>Spoon runs the parse as a fixed sequence of stages
 * ({@code COMPILE → COMMENT → MODEL → IMPORT → COMMENT_LINKING}), each iterating every
 * input file and emitting {@code step(process, file, i, n)}. Each stage is mapped to a
 * sub-range of {@code [0, 1]} weighted by its observed cost (COMPILE dominates); within a
 * stage the fraction interpolates on {@code i/n}. The reported fraction is clamped to be
 * non-decreasing, so Spoon's trailing no-op stage repetitions never rewind the bar. The
 * top of the range ({@code >= COMPLETE_AT}) is reserved for the three resolution phases,
 * reported by {@link SourceMetaModel} itself.</p>
 */
final class SpoonProgressAdapter implements SpoonProgress {

    /** Spoon stages get [0, COMPLETE_AT]; the resolution phases get [COMPLETE_AT, 1]. */
    static final double COMPLETE_AT = 0.95;

    /** Cumulative [start, end] sub-range of each Spoon stage, weighted by observed cost. */
    private static final Map<Process, double[]> RANGES = new EnumMap<>(Process.class);
    private static final Map<Process, String> LABELS = new EnumMap<>(Process.class);
    static {
        // Spans sum to COMPLETE_AT (0.95). COMPILE is the dominant ~50%.
        RANGES.put(Process.COMPILE,         new double[] {0.00, 0.50});
        RANGES.put(Process.COMMENT,         new double[] {0.50, 0.53});
        RANGES.put(Process.MODEL,           new double[] {0.53, 0.78});
        RANGES.put(Process.IMPORT,          new double[] {0.78, 0.90});
        RANGES.put(Process.COMMENT_LINKING, new double[] {0.90, 0.95});

        // Stable, language-neutral tokens (not display text) — this module has no
        // UI/localization dependency (localization-design.md); the caller
        // (pamela-editor-ui's BuildProgressListener) translates them for display.
        LABELS.put(Process.COMPILE,         "compiling_source_files");
        LABELS.put(Process.COMMENT,         "reading_comments");
        LABELS.put(Process.MODEL,           "building_model");
        LABELS.put(Process.IMPORT,          "resolving_imports");
        LABELS.put(Process.COMMENT_LINKING, "linking_comments");
        LABELS.put(Process.PROCESS,         "processing_files");
        LABELS.put(Process.PRINT,           "printing_files");
    }

    private final BuildProgressListener listener;
    private double lastFraction;

    SpoonProgressAdapter(BuildProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public void start(Process process) {
        double[] range = RANGES.get(process);
        if (range != null) {
            emit(range[0], label(process));
        }
    }

    @Override
    public void step(Process process, String task, int taskId, int nbTasks) {
        double[] range = RANGES.get(process);
        if (range == null) {
            return;
        }
        double within = (nbTasks > 0) ? (double) taskId / nbTasks : 1.0;
        double fraction = range[0] + within * (range[1] - range[0]);
        // Structured, pipe-delimited message ("key|taskId|nbTasks|fileName"): the key is a
        // translatable token, the rest is display detail the UI reassembles verbatim (see
        // BuildProgressListener / PamelaEditorApplication.translateBuildMessage).
        emit(fraction, label(process) + "|" + taskId + "|" + nbTasks + "|" + simpleName(task));
    }

    @Override
    public void step(Process process, String task) {
        // No count available: keep the current fraction, just refresh the message.
        emit(lastFraction, label(process) + "||" + simpleName(task));
    }

    @Override
    public void end(Process process) {
        double[] range = RANGES.get(process);
        if (range != null) {
            emit(range[1], label(process));
        }
    }

    private void emit(double fraction, String message) {
        // Monotonic: never let a trailing no-op stage rewind the bar.
        if (fraction > lastFraction) {
            lastFraction = fraction;
        }
        listener.progress(lastFraction, message);
    }

    private static String label(Process process) {
        String l = LABELS.get(process);
        return l != null ? l : process.name();
    }

    private static String simpleName(String path) {
        if (path == null) {
            return "";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
