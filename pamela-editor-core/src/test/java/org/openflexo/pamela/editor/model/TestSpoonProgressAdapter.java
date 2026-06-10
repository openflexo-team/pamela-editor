package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import spoon.support.compiler.SpoonProgress;
import spoon.support.compiler.SpoonProgress.Process;

/**
 * Unit tests for {@link SpoonProgressAdapter}'s fraction mapping: the reported fraction
 * must stay within {@code [0, 1]} and be monotonically non-decreasing, even when Spoon
 * replays trailing no-op stages (as it does after the main parse).
 */
public class TestSpoonProgressAdapter {

    private static final Process[] STAGES = {
            Process.COMPILE, Process.COMMENT, Process.MODEL,
            Process.IMPORT, Process.COMMENT_LINKING
    };

    /** Drives the adapter with a realistic Spoon callback sequence and records fractions. */
    private List<Double> run(int nbFiles) {
        List<Double> fractions = new ArrayList<>();
        SpoonProgress adapter = new SpoonProgressAdapter(
                (fraction, message) -> fractions.add(fraction));

        for (Process p : STAGES) {
            adapter.start(p);
            for (int i = 1; i <= nbFiles; i++) {
                adapter.step(p, "/src/File" + i + ".java", i, nbFiles);
            }
            adapter.end(p);
        }
        // Spoon replays some stages as no-ops (start/end, no steps) after the main parse.
        adapter.start(Process.MODEL);
        adapter.end(Process.MODEL);
        adapter.start(Process.IMPORT);
        adapter.end(Process.IMPORT);
        adapter.start(Process.COMMENT_LINKING);
        adapter.end(Process.COMMENT_LINKING);
        return fractions;
    }

    @Test
    public void testMonotonicAndBounded() {
        List<Double> f = run(403);
        double prev = -1;
        for (double x : f) {
            assertTrue("fraction must be >= 0: " + x, x >= 0.0);
            assertTrue("fraction must be <= 1: " + x, x <= 1.0);
            assertTrue("fraction must not decrease: " + prev + " -> " + x, x >= prev - 1e-9);
            prev = x;
        }
    }

    @Test
    public void testReachesParseCeilingButNotComplete() {
        List<Double> f = run(403);
        double max = 0;
        for (double x : f) {
            max = Math.max(max, x);
        }
        // The Spoon stages fill up to COMPLETE_AT; the final [COMPLETE_AT, 1] band is
        // reserved for the resolution phases reported by SourceMetaModel, not the adapter.
        assertTrue("should reach the parse ceiling", max >= SpoonProgressAdapter.COMPLETE_AT - 1e-9);
        assertTrue("adapter alone must not reach 100%", max <= SpoonProgressAdapter.COMPLETE_AT + 1e-9);
    }

    @Test
    public void testTrailingNoOpStagesDoNotRewind() {
        // The last reported fraction (after the no-op replays) must equal the ceiling,
        // i.e. the no-op MODEL/IMPORT replays did not pull the bar back down.
        List<Double> f = run(50);
        double last = f.get(f.size() - 1);
        assertTrue("trailing no-op stages must not rewind the bar: " + last,
                last >= SpoonProgressAdapter.COMPLETE_AT - 1e-9);
    }
}
