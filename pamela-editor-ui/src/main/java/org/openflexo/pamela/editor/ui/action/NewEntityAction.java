package org.openflexo.pamela.editor.ui.action;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Creates a brand-new PAMELA entity (a new annotated {@code interface}) in a
 * {@link SourcePackage} — the "create" path of {@code model-editing-design.md §1.1}.
 *
 * <p>Flat action: it extends {@link ParameteredAction} directly, owns its
 * own parameter ({@code name}) and references its own form fragment
 * ({@code NewEntityForm.fib}) — its class identity is not tied to its dialog shape.</p>
 */
public class NewEntityAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewEntityForm.fib");

    private String name = "";
    private Set<String> forbidden = Collections.emptySet();

    @Override
    public String getLabel() {
        return "New Entity…";
    }

    @Override
    public boolean isApplicable(Object target) {
        // Lot 1 scope: creation is anchored on a package node (§11 Q6).
        return target instanceof SourcePackage
                && ((SourcePackage) target).getMetaModel() != null;
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return "New Entity";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        SourcePackage pkg = (SourcePackage) target;
        forbidden = new HashSet<>();
        for (SourceModelEntity e : pkg.getEntities()) {
            forbidden.add(e.getSimpleName());
        }
        name = "NewEntity";
        return true;
    }

    @Override
    public boolean isInputValid() {
        return ModelEditingSupport.isAvailableIdentifier(name, forbidden);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourcePackage pkg = (SourcePackage) target;
        SourceMetaModel model = pkg.getMetaModel();
        String simpleName = name.trim();
        String pkgPrefix = pkg.getQualifiedName() == null || pkg.getQualifiedName().isEmpty()
                ? "" : pkg.getQualifiedName() + ".";
        final String qualifiedName = pkgPrefix + simpleName;

        model.createEntity(simpleName, pkg);
        model.addRootTypeName(qualifiedName);
        return () -> model.getEntity(qualifiedName);
    }

    // --- bound by NewEntityForm.fib -----------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        fireInputValidChanged();
    }
}
