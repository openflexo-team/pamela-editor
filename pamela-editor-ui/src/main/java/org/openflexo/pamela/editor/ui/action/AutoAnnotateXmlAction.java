package org.openflexo.pamela.editor.ui.action;

import java.util.function.Supplier;

import javax.swing.ImageIcon;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Metamodel-wide auto-annotation for XML serialization (fill-only-missing —
 * {@code xml-serialization-design.md §2.1 / §5.3}). Applies the standard PAMELA convention
 * across every entity and its declared properties, never touching an element/property that
 * already carries an XML annotation. No dialog (the operation is safe and idempotent);
 * delegates to {@link SourceMetaModel#autoAnnotateXmlSerialization()} and rebuilds.
 */
public class AutoAnnotateXmlAction extends SourceEditingAction {

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.GENERATE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    @Override
    public String getLabel() {
        return loc("auto_annotate_xml_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceMetaModel || target instanceof PamelaProject;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel metaModel = (target instanceof PamelaProject)
                ? ((PamelaProject) target).getMetaModel()
                : (SourceMetaModel) target;
        int added = metaModel.autoAnnotateXmlSerialization();
        // Report the outcome (fill-only-missing may add nothing on a second run).
        ModelEditingSupport.info(app, getLabel(),
                loc("auto_annotate_xml_result") + " " + added);
        final Object anchor = target;
        return () -> anchor;
    }
}
