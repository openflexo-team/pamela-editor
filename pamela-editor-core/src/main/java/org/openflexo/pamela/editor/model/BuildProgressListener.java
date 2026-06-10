package org.openflexo.pamela.editor.model;

/**
 * Receives progress notifications while a {@link SourceMetaModel} is being built
 * (see {@link SourceMetaModel#setProgressListener}).
 *
 * <p>Implementations are called from the <em>build thread</em> (not the EDT). A Swing
 * client must therefore marshal updates onto the Event Dispatch Thread itself (e.g. via
 * {@code SwingWorker.setProgress}/{@code publish}).</p>
 *
 * <p>The {@code fraction} is an overall, monotonically non-decreasing estimate in
 * {@code [0, 1]}, already computed by the core from Spoon's per-file stage callbacks and
 * the three resolution phases — the UI only has to render it.</p>
 */
public interface BuildProgressListener {

    /**
     * @param fraction overall completion in {@code [0, 1]} (monotonic, non-decreasing)
     * @param message  a short human-readable description of the current step
     */
    void progress(double fraction, String message);
}
