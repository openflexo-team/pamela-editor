package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Creates a brand-new PAMELA entity (a new annotated {@code interface}) — the
 * "create" path of {@code model-editing-design.md §1.1}.
 *
 * <p>Collects source folder, package, name, abstractness and an optional parent
 * entity. Invocable from several contexts (a {@link SourcePackage}, a
 * {@link SourceFolder}, an existing {@link SourceModelEntity}, or the project
 * root — {@link SourceMetaModel} / {@link PamelaProject}); {@link #prepareDialog}
 * pre-fills whatever the context already pins down, per
 * {@code model-editing-design.md} (NewEntityAction section).</p>
 *
 * <p>Flat action: it extends {@link ParameteredAction} directly and references
 * its own form fragment ({@code NewEntityForm.fib}) — its class identity is not
 * tied to its dialog shape.</p>
 */
public class NewEntityAction extends ParameteredAction {


    @Override
    public ActionGroup getGroup() {
        return ActionGroup.NEW;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.ENTITY_ICON;
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/NewEntityForm.fib");

    private SourceMetaModel metaModel;
    private SourceFolder sourceFolder;
    private SourcePackage sourcePackage;
    private String name = "";
    private boolean abstractEntity = false;
    private SourceModelEntity superEntity;
    private Set<String> forbidden = Collections.emptySet();

    @Override
    public String getLabel() {
        return "New Entity…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return resolveMetaModel(target) != null;
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
        SourceMetaModel model = resolveMetaModel(target);
        if (model == null) {
            return false;
        }
        metaModel = model;
        name = "NewEntity";
        abstractEntity = false;
        superEntity = null;
        sourceFolder = null;
        sourcePackage = null;

        if (target instanceof SourcePackage) {
            sourcePackage = (SourcePackage) target;
            sourceFolder = folderContaining(model, sourcePackage);
        } else if (target instanceof SourceFolder) {
            sourceFolder = (SourceFolder) target;
        } else if (target instanceof SourceModelEntity) {
            SourceModelEntity entity = (SourceModelEntity) target;
            sourcePackage = entity.getSourcePackage();
            sourceFolder = folderContaining(model, sourcePackage);
            superEntity = entity;
        }
        // SourceMetaModel / PamelaProject: nothing pre-filled beyond the metamodel scope.

        recomputeForbidden();
        return true;
    }

    @Override
    public boolean isInputValid() {
        return sourceFolder != null
                && sourcePackage != null
                && ModelEditingSupport.isAvailableIdentifier(name, forbidden);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel model = project.getMetaModel();
        String simpleName = name.trim();

        SourceModelEntity entity = model.createEntity(simpleName, sourcePackage, sourceFolder, abstractEntity);
        model.addRootTypeName(entity.getQualifiedName());
        if (superEntity != null) {
            entity.addSuperEntity(superEntity);
        }
        final String qualifiedName = entity.getQualifiedName();
        return () -> model.getEntity(qualifiedName);
    }

    // --- bound by NewEntityForm.fib -----------------------------------------

    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    public SourceFolder getSourceFolder() {
        return sourceFolder;
    }

    public void setSourceFolder(SourceFolder sourceFolder) {
        SourceFolder old = this.sourceFolder;
        this.sourceFolder = sourceFolder;
        getPropertyChangeSupport().firePropertyChange("sourceFolder", old, sourceFolder);
        if (sourcePackage != null
                && (sourceFolder == null || !sourceFolder.getPackages().contains(sourcePackage))) {
            setSourcePackage(null);
        }
        fireInputValidChanged();
    }

    public SourcePackage getSourcePackage() {
        return sourcePackage;
    }

    public void setSourcePackage(SourcePackage sourcePackage) {
        SourcePackage old = this.sourcePackage;
        this.sourcePackage = sourcePackage;
        getPropertyChangeSupport().firePropertyChange("sourcePackage", old, sourcePackage);
        recomputeForbidden();
        fireInputValidChanged();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        fireInputValidChanged();
    }

    public boolean isAbstractEntity() {
        return abstractEntity;
    }

    public void setAbstractEntity(boolean abstractEntity) {
        boolean old = this.abstractEntity;
        this.abstractEntity = abstractEntity;
        getPropertyChangeSupport().firePropertyChange("abstractEntity", old, abstractEntity);
    }

    /** Optional parent entity — a super-interface added right after creation. */
    public SourceModelEntity getSuperEntity() {
        return superEntity;
    }

    public void setSuperEntity(SourceModelEntity superEntity) {
        SourceModelEntity old = this.superEntity;
        this.superEntity = superEntity;
        getPropertyChangeSupport().firePropertyChange("superEntity", old, superEntity);
    }

    // -------------------------------------------------------------------------

    private void recomputeForbidden() {
        Set<String> names = new HashSet<>();
        if (sourcePackage != null) {
            for (SourceModelEntity e : sourcePackage.getEntities()) {
                names.add(e.getSimpleName());
            }
        }
        forbidden = names;
    }

    private static SourceMetaModel resolveMetaModel(Object target) {
        if (target instanceof SourcePackage) {
            return ((SourcePackage) target).getMetaModel();
        }
        if (target instanceof SourceFolder) {
            return ((SourceFolder) target).getMetaModel();
        }
        if (target instanceof SourceModelEntity) {
            return ((SourceModelEntity) target).getMetaModel();
        }
        if (target instanceof SourceMetaModel) {
            return (SourceMetaModel) target;
        }
        if (target instanceof PamelaProject) {
            return ((PamelaProject) target).getMetaModel();
        }
        return null;
    }

    private static SourceFolder folderContaining(SourceMetaModel model, SourcePackage pkg) {
        if (model == null || pkg == null) {
            return null;
        }
        for (SourceFolder folder : model.getSourceFolders()) {
            if (folder.getPackages().contains(pkg)) {
                return folder;
            }
        }
        return null;
    }
}
