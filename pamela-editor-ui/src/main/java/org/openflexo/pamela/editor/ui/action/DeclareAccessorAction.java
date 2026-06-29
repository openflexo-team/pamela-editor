package org.openflexo.pamela.editor.ui.action;

import javax.swing.ImageIcon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Base for the method-level "complete a concept" promote actions: attach an existing plain method
 * to an existing PAMELA property as one of its accessors (setter / updater / adder / remover /
 * reindexer) — model-editing-design.md §3.3. Keyed on a {@link PromotableMethod} facet; the set of
 * candidate target properties is computed from the method's <strong>signature</strong> (parameter
 * types) against the property types, never from the method name. The user picks the target property
 * in a shared dropdown form ({@code AttachAccessorForm.fib}); it is pre-selected to the first match.
 *
 * <p>Subclasses define the role: which properties are candidates ({@link #candidateProperties})
 * and how to attach the method ({@link #attach}).</p>
 */
public abstract class DeclareAccessorAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/AttachAccessorForm.fib");

    private List<SourceModelProperty> candidates = Collections.emptyList();
    private SourceModelProperty targetProperty;
    private SourceModelEntity entity;
    private String methodName;
    private int parameterCount;
    private String entityQN;

    // --- role hooks (subclasses) --------------------------------------------

    /** Properties this method may be attached to, given its signature. */
    protected abstract List<SourceModelProperty> candidateProperties(PromotableMethod pm);

    /** Attaches the method (by name) to the chosen property in the chosen role. */
    protected abstract void attach(SourceModelProperty target, String methodName) throws IOException;

    /** Short role noun for the property identifier of the target ({@code setter}, {@code adder}…). */
    protected abstract String roleNoun();

    // --- ContextualAction / ParameteredAction -------------------------------

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.PROMOTE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.PROPERTY_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.REINJECT };
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof PromotableMethod
                && !candidateProperties((PromotableMethod) target).isEmpty();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        PromotableMethod pm = (PromotableMethod) target;
        candidates = candidateProperties(pm);
        if (candidates.isEmpty()) {
            return false;
        }
        targetProperty = candidates.get(0);
        entity = pm.getEntity();
        methodName = pm.getMethodName();
        parameterCount = pm.getParameterCount();
        entityQN = pm.getEntity().getQualifiedName();
        return true;
    }

    @Override
    public boolean isInputValid() {
        return targetProperty != null;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        final String propertyId = targetProperty.getPropertyIdentifier();
        final SourceMetaModel model = targetProperty.getModelEntity().getMetaModel();
        attach(targetProperty, methodName);
        // Reselect the now-completed property after the rebuild.
        return () -> {
            SourceModelEntity e = model.getEntity(entityQN);
            return e != null ? e.getDeclaredProperties().get(propertyId) : null;
        };
    }

    // --- bound by AttachAccessorForm.fib ------------------------------------

    public List<SourceModelProperty> getCandidates() {
        return candidates;
    }

    /** Owning entity — the {@link org.openflexo.pamela.editor.ui.widget.ModelPropertySelector} context. */
    public SourceModelEntity getEntity() {
        return entity;
    }

    public SourceModelProperty getTargetProperty() {
        return targetProperty;
    }

    public void setTargetProperty(SourceModelProperty targetProperty) {
        this.targetProperty = targetProperty;
        fireInputValidChanged();
    }

    /** Unused-but-handy for the form prompt. */
    public int getParameterCount() {
        return parameterCount;
    }

    // --- shared signature helpers for subclasses ----------------------------

    /** SINGLE properties whose type equals the single parameter type and that lack the given accessor. */
    protected static List<SourceModelProperty> singlePropertiesOfParamType(PromotableMethod pm,
            java.util.function.Predicate<SourceModelProperty> lacksAccessor) {
        if (pm.getParameterCount() != 1) {
            return Collections.emptyList();
        }
        return matching(pm.getEntity(), pm.getParameterTypeQualifiedName(0), false, lacksAccessor);
    }

    /** LIST properties whose element type equals the single parameter type and that lack the accessor. */
    protected static List<SourceModelProperty> listPropertiesOfElementType(PromotableMethod pm,
            java.util.function.Predicate<SourceModelProperty> lacksAccessor) {
        if (pm.getParameterCount() != 1) {
            return Collections.emptyList();
        }
        return matching(pm.getEntity(), pm.getParameterTypeQualifiedName(0), true, lacksAccessor);
    }

    private static List<SourceModelProperty> matching(SourceModelEntity entity, String paramTypeQN,
            boolean wantList, java.util.function.Predicate<SourceModelProperty> lacksAccessor) {
        List<SourceModelProperty> result = new ArrayList<>();
        if (entity == null || paramTypeQN == null) {
            return result;
        }
        for (SourceModelProperty p : entity.getDeclaredProperties().values()) {
            boolean isList = p.getCardinality() == org.openflexo.pamela.annotations.Getter.Cardinality.LIST;
            if (isList != wantList) {
                continue;
            }
            if (p.getType() == null || !paramTypeQN.equals(p.getType().getQualifiedName())) {
                continue;
            }
            if (lacksAccessor.test(p)) {
                result.add(p);
            }
        }
        return result;
    }
}
