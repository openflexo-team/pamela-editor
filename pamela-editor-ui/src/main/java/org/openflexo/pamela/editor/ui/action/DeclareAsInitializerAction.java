package org.openflexo.pamela.editor.ui.action;

import javax.swing.ImageIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
 * Declares an existing method as an {@code @Initializer}, mapping each parameter to a property via
 * {@code @Parameter("propId")} (model-editing-design.md §3.3a). Applicable (by signature) to a
 * {@link PromotableMethod} that returns {@code void} or the entity's own type and whose every
 * parameter type matches at least one property (so the mapping is feasible).
 *
 * <p>The dialog shows one row per parameter ({@code name : Type} + a property dropdown pre-selected
 * by type, then by name); up to {@value #MAX_PARAMS} parameters are supported.</p>
 */
public class DeclareAsInitializerAction extends ParameteredAction {

    /** Max parameters handled by the fixed-slot form. */
    public static final int MAX_PARAMS = 6;

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/DeclareInitializerForm.fib");

    private SourceModelEntity entity;
    private String methodName;
    private int parameterCount;
    private final ParameterMapping[] params = new ParameterMapping[MAX_PARAMS];

    public DeclareAsInitializerAction() {
        for (int i = 0; i < MAX_PARAMS; i++) {
            params[i] = new ParameterMapping();
        }
    }

    @Override
    public String getLabel() {
        return loc("declare_as_initializer_action");
    }

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.PROMOTE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.INITIALIZER_ICON;
    }

    @Override
    protected IconMarker[] getMarkers() {
        return new IconMarker[] { PamelaEditorIconLibrary.REINJECT };
    }

    @Override
    public boolean isApplicable(Object target) {
        if (!(target instanceof PromotableMethod)) {
            return false;
        }
        PromotableMethod pm = (PromotableMethod) target;
        if (pm.getParameterCount() > MAX_PARAMS || !isInitializerShaped(pm)) {
            return false;
        }
        // Every parameter must map to at least one property of matching type.
        for (int i = 0; i < pm.getParameterCount(); i++) {
            if (propertiesOfType(pm.getEntity(), pm.getParameterTypeQualifiedName(i)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** An initializer returns {@code void} or the entity's own type (a factory). */
    private static boolean isInitializerShaped(PromotableMethod pm) {
        String ret = pm.getReturnTypeQualifiedName();
        return "void".equals(ret) || pm.getEntity().getQualifiedName().equals(ret);
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("declare_method_as_initializer_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        PromotableMethod pm = (PromotableMethod) target;
        entity = pm.getEntity();
        methodName = pm.getMethodName();
        parameterCount = pm.getParameterCount();
        for (int i = 0; i < MAX_PARAMS; i++) {
            if (i < parameterCount) {
                List<SourceModelProperty> candidates =
                        propertiesOfType(entity, pm.getParameterTypeQualifiedName(i));
                String paramName = pm.getParameterName(i);
                SourceModelProperty def = firstNamedOrFirst(candidates, paramName);
                params[i].configure(true,
                        paramName + " : " + pm.getParameterTypeSimpleName(i), candidates, def);
            } else {
                params[i].configure(false, "", Collections.emptyList(), null);
            }
        }
        return true;
    }

    @Override
    public boolean isInputValid() {
        for (int i = 0; i < parameterCount; i++) {
            if (params[i].getSelected() == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < parameterCount; i++) {
            ids.add(params[i].getSelected().getPropertyIdentifier());
        }
        entity.declareInitializer(methodName, ids);
        return () -> model.getEntity(entityQN);
    }

    /** Properties (declared + inherited) whose type matches {@code typeQN}. */
    private static List<SourceModelProperty> propertiesOfType(SourceModelEntity entity, String typeQN) {
        List<SourceModelProperty> result = new ArrayList<>();
        if (typeQN == null) {
            return result;
        }
        for (Map.Entry<String, SourceModelProperty> e : entity.getAllProperties().entrySet()) {
            SourceModelProperty p = e.getValue();
            if (p.getType() != null && typeQN.equals(p.getType().getQualifiedName())) {
                result.add(p);
            }
        }
        return result;
    }

    private static SourceModelProperty firstNamedOrFirst(List<SourceModelProperty> props, String id) {
        for (SourceModelProperty p : props) {
            if (p.getPropertyIdentifier().equals(id)) {
                return p;
            }
        }
        return props.isEmpty() ? null : props.get(0);
    }

    // --- bound by DeclareInitializerForm.fib --------------------------------

    /** Owning entity — the per-parameter {@code ModelPropertySelector} context. */
    public SourceModelEntity getEntity() { return entity; }

    public ParameterMapping getParam0() { return params[0]; }
    public ParameterMapping getParam1() { return params[1]; }
    public ParameterMapping getParam2() { return params[2]; }
    public ParameterMapping getParam3() { return params[3]; }
    public ParameterMapping getParam4() { return params[4]; }
    public ParameterMapping getParam5() { return params[5]; }

    // =========================================================================

    /** One parameter row: a {@code name : Type} label + a property dropdown. */
    public final class ParameterMapping {

        private boolean show;
        private String label = "";
        private List<SourceModelProperty> candidates = Collections.emptyList();
        private SourceModelProperty selected;

        void configure(boolean show, String label, List<SourceModelProperty> candidates,
                SourceModelProperty selected) {
            this.show = show;
            this.label = label;
            this.candidates = candidates;
            this.selected = selected;
        }

        public boolean isShow()                          { return show; }
        public String getLabel()                         { return label; }
        public List<SourceModelProperty> getCandidates() { return candidates; }

        public SourceModelProperty getSelected()              { return selected; }
        public void setSelected(SourceModelProperty s)        { this.selected = s; fireInputValidChanged(); }
    }
}
