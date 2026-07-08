package org.openflexo.pamela.editor.model;

/**
 * Marker interface for all elements that belong to a {@link SourceMetaModel}.
 * Every Source* class in the model layer implements this interface.
 */
public interface SourceElement {

    /**
     * The meta-model this element belongs to. Every {@link SourceElement} can resolve
     * back to its owning {@link SourceMetaModel} — used, among other things, to look up
     * the {@link Issue}s raised against this specific element (see
     * {@link SourceMetaModel#hasErrors(SourceElement)} / {@link SourceMetaModel#hasWarnings(SourceElement)}).
     */
    SourceMetaModel getMetaModel();
}
