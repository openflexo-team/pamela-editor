package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceImplementationClass;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Generates a brand-new abstract implementation class for an entity and attaches it — the
 * "create" path of {@code implementation-class-support-design.md §3.2/§4} (the "attach existing
 * class" path is {@link AttachImplementationClassAction}).
 *
 * <p>Flat action with its own {@code name} parameter and form fragment
 * ({@code CreateImplementationClassForm.fib}), pre-filled with the conventional
 * {@code <EntitySimpleName>Impl} name. Delegates to
 * {@link SourceMetaModel#createImplementationClass}. Always placed right next to the entity's
 * own file — no folder/package picker.</p>
 */
public class CreateImplementationClassAction extends ParameteredAction {

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.REFACTOR;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/CreateImplementationClassForm.fib");

    private String name = "";
    private Set<String> forbidden = Collections.emptySet();

    @Override
    public String getLabel() {
        return loc("create_implementation_class_action");
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof SourceModelEntity;
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("create_implementation_class_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourceModelEntity entity = (SourceModelEntity) target;
        if (entity.getCompilationUnit() == null || entity.getCompilationUnit().getFile() == null) {
            return false;
        }
        forbidden = existingSimpleNamesInSameDirectory(entity);
        name = entity.getSimpleName() + "Impl";
        return true;
    }

    @Override
    public boolean isInputValid() {
        return ModelEditingSupport.isAvailableIdentifier(name, forbidden);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceModelEntity entity = (SourceModelEntity) target;
        SourceMetaModel model = entity.getMetaModel();
        SourceImplementationClass impl = model.createImplementationClass(entity, name.trim());
        if (impl == null) {
            return null;
        }
        final String entityQN = entity.getQualifiedName();
        return () -> model.getEntity(entityQN);
    }

    // --- bound by CreateImplementationClassForm.fib -------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        fireInputValidChanged();
    }

    // -------------------------------------------------------------------------

    /**
     * Simple names of every {@code .java} file already present in the entity's own directory
     * (the target directory for the new impl class file) — used to reject a name collision.
     */
    private static Set<String> existingSimpleNamesInSameDirectory(SourceModelEntity entity) {
        Set<String> names = new HashSet<>();
        File dir = entity.getCompilationUnit().getFile().getParentFile();
        File[] files = dir != null ? dir.listFiles((d, n) -> n.endsWith(".java")) : null;
        if (files != null) {
            for (File f : files) {
                String n = f.getName();
                names.add(n.substring(0, n.length() - ".java".length()));
            }
        }
        return names;
    }
}
