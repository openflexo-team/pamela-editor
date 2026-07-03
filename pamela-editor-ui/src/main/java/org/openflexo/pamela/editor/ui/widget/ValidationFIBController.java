package org.openflexo.pamela.editor.ui.widget;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.Error;
import org.openflexo.pamela.editor.model.Information;
import org.openflexo.pamela.editor.model.Issue;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.Warning;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;

/**
 * FIB controller for {@link ValidationPanel} (validation-log-panel-design.md).
 *
 * <p>Drives the issues table (icon resolution, click-to-navigate) and the "Revalidate" action
 * (re-runs the metamodel's analysis). Expand/collapse is handled by the enclosing
 * {@code JSplitPane}'s native one-touch-expandable divider, not from here.</p>
 */
public class ValidationFIBController extends PamelaEditorFIBController<SourceMetaModel> {

    private PamelaEditorApplication application;

    public ValidationFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    public void setApplication(PamelaEditorApplication application) {
        this.application = application;
    }

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        if (object instanceof Issue) {
            boolean fixable = !((Issue) object).getFixProposals().isEmpty();
            if (object instanceof Error) {
                return fixable ? PamelaEditorIconLibrary.FIXABLE_ERROR_ICON
                        : PamelaEditorIconLibrary.UNFIXABLE_ERROR_ICON;
            }
            if (object instanceof Warning) {
                return fixable ? PamelaEditorIconLibrary.FIXABLE_WARNING_ICON
                        : PamelaEditorIconLibrary.UNFIXABLE_WARNING_ICON;
            }
            if (object instanceof Information) {
                return PamelaEditorIconLibrary.INFO_ICON;
            }
        }
        return super.retrieveIconForObject(object);
    }

    /**
     * Called when the user clicks a row in the issues table (FIB {@code clickAction}).
     * Navigates the central view to the issue's source element, when known.
     */
    public void selectIssue(Object selected) {
        if (application == null || !(selected instanceof Issue)) {
            return;
        }
        Object source = ((Issue) selected).getSource();
        if (source != null) {
            application.setCurrentSelectedElement(source);
        }
    }

    /** Called from the "Revalidate" link — re-runs the metamodel's Spoon analysis. */
    public void revalidate() {
        if (application == null) {
            return;
        }
        SourceMetaModel model = getDataObject();
        if (model == null) {
            return;
        }
        org.openflexo.pamela.editor.ui.PamelaProject project = application.getProjectForElement(model);
        if (project != null) {
            application.rebuildProject(project);
        }
    }
}
