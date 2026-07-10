package org.openflexo.pamela.editor.ui.action;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import javax.swing.ImageIcon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication.NewEntityDisplay;
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

    /**
     * How the new entity should be presented, captured from the active central view
     * <em>before</em> the mutation (see {@code model-editing-design.md}, NewEntityAction).
     */
    private NewEntityDisplay display = NewEntityDisplay.CODE;

    @Override
    public String getLabel() {
        return "New Entity…";
    }

    @Override
    public boolean isApplicable(Object target) {
        // A class diagram (right-click on the diagram background) is also a valid context:
        // the entity is created and added to that diagram (its metamodel is resolved via the
        // application in prepareDialog).
        return resolveMetaModel(target) != null || target instanceof PamelaClassDiagram;
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
        // Capture how to present the result from the currently active central view, before
        // the mutation + rebuild recreate the displayed element.
        display = (app != null) ? app.currentViewPresentation() : NewEntityDisplay.CODE;

        SourceMetaModel model = resolveMetaModel(target);
        if (model == null && target instanceof PamelaClassDiagram && app != null) {
            // Right-click on a diagram: resolve the metamodel through the owning project.
            PamelaProject project = app.getProjectForElement(target);
            if (project != null) {
                model = project.getMetaModel();
            }
        }
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
        } else if (target instanceof PamelaClassDiagram) {
            prefillFromDiagram(model, (PamelaClassDiagram) target, app);
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

    @Override
    protected void presentResult(PamelaEditorApplication app, Object result) {
        // Present the new entity according to the view that was active when creation was
        // requested (tabular stays / diagram box / new code view) — model-editing-design.md.
        if (result instanceof SourceModelEntity) {
            app.presentNewEntity((SourceModelEntity) result, display);
        } else {
            super.presentResult(app, result);
        }
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

    /**
     * Pre-fills the source folder / package for a "New Entity" invoked from a diagram background
     * (right-click on the canvas), inferring the most likely destination — a cascade from the most
     * direct intent signal to the most general fallback:
     * <ol>
     *   <li><b>Selected entity</b>: if an entity is currently selected <em>on this diagram</em>
     *       (e.g. the box just clicked or created) → pre-select its package (and its folder);</li>
     *   <li><b>Majority package</b>: else the package shared by the most resolved entities on the
     *       diagram → pre-select it (and its folder); a strict tie for the top is skipped as
     *       ambiguous;</li>
     *   <li><b>Majority source folder</b>: else the folder shared by the most on-diagram entities
     *       → pre-select it (the package is left to the user); a strict tie is skipped;</li>
     *   <li><b>Single source folder</b>: else, if the metamodel has exactly one source folder →
     *       pre-select it;</li>
     *   <li>otherwise pre-fill nothing (the user picks both).</li>
     * </ol>
     * Unresolved entity views (dangling placeholders) and package-less entities are ignored.
     */
    private void prefillFromDiagram(SourceMetaModel model, PamelaClassDiagram diagram,
            PamelaEditorApplication app) {
        // 1. Strongest signal: the entity currently selected on this diagram → its package.
        Object selected = (app != null) ? app.getCurrentSelectedElement() : null;
        if (selected instanceof SourceModelEntity && isOnDiagram((SourceModelEntity) selected, diagram)) {
            SourcePackage pkg = ((SourceModelEntity) selected).getSourcePackage();
            if (pkg != null) {
                sourcePackage = pkg;
                sourceFolder = folderContaining(model, pkg);
                return;
            }
        }
        // Tally the packages / folders of the resolved entities on the diagram.
        java.util.Map<SourcePackage, Integer> packageCounts = new java.util.LinkedHashMap<>();
        java.util.Map<SourceFolder, Integer> folderCounts = new java.util.LinkedHashMap<>();
        for (EntityView ev : diagram.getEntityViews()) {
            SourceModelEntity entity = ev.getEntity();
            if (entity == null) {
                continue;
            }
            SourcePackage pkg = entity.getSourcePackage();
            if (pkg == null) {
                continue;
            }
            packageCounts.merge(pkg, 1, Integer::sum);
            SourceFolder folder = folderContaining(model, pkg);
            if (folder != null) {
                folderCounts.merge(folder, 1, Integer::sum);
            }
        }
        // 2. Majority package (unique winner).
        SourcePackage majorityPackage = strictMostFrequent(packageCounts);
        if (majorityPackage != null) {
            sourcePackage = majorityPackage;
            sourceFolder = folderContaining(model, majorityPackage);
            return;
        }
        // 3. Majority source folder (disambiguates a package tie when they share a folder).
        SourceFolder majorityFolder = strictMostFrequent(folderCounts);
        if (majorityFolder != null) {
            sourceFolder = majorityFolder;
            return;
        }
        // 4. Single source folder in the metamodel.
        if (model.getSourceFolders().size() == 1) {
            sourceFolder = model.getSourceFolders().get(0);
        }
    }

    /** Whether {@code entity} is represented by an {@link EntityView} on {@code diagram}. */
    private static boolean isOnDiagram(SourceModelEntity entity, PamelaClassDiagram diagram) {
        String qualifiedName = entity.getQualifiedName();
        for (EntityView ev : diagram.getEntityViews()) {
            if (qualifiedName.equals(ev.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The key with the strictly highest count, or {@code null} when the map is empty or several
     * keys tie for the top count (ambiguous → let the caller fall through to a coarser rule).
     */
    private static <T> T strictMostFrequent(java.util.Map<T, Integer> counts) {
        int max = 0;
        for (int c : counts.values()) {
            max = Math.max(max, c);
        }
        if (max == 0) {
            return null;
        }
        T winner = null;
        int winners = 0;
        for (java.util.Map.Entry<T, Integer> e : counts.entrySet()) {
            if (e.getValue() == max) {
                winner = e.getKey();
                winners++;
            }
        }
        return winners == 1 ? winner : null;
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
