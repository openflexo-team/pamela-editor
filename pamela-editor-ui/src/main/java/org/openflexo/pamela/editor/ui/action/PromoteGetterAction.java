package org.openflexo.pamela.editor.ui.action;

import javax.swing.ImageIcon;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Reindexer;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.Updater;
import org.openflexo.pamela.editor.model.MethodSignature;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Method-level "promote" action: turns a plain no-arg getter into a <em>new</em> PAMELA property
 * (model-editing-design.md §3.3a). Keyed on a {@link PromotableMethod} facet and applicable on
 * <strong>signature</strong> alone (no parameter, non-{@code void} return): a {@code List<T>} return
 * yields a LIST property, anything else a SINGLE property.
 *
 * <p>The dialog also offers to <strong>pull in the property's other accessors</strong> found in the
 * same interface: for a SINGLE getter a setter / updater; for a LIST getter an adder / remover /
 * reindexer. Candidates are gathered by <strong>signature</strong> (parameter types vs the property
 * type / element type) and, when several match, the default is picked by <strong>name</strong>
 * (e.g. {@code getX} → {@code setX}). Each accessor is a "also promote …" checkbox + a dropdown of
 * the matching methods. All annotations are written in a single text edit by
 * {@link SourceModelEntity#promoteGetterToProperty(String, String, boolean, Map)}.</p>
 */
public class PromoteGetterAction extends ParameteredAction {

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
        return new IconMarker[] { PamelaEditorIconLibrary.PLUS };
    }

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/PromoteGetterForm.fib");

    private SourceModelEntity entity;
    private String methodName;
    private boolean list;
    private String propertyTypeQN;          // property type (SINGLE) / element type (LIST)
    private String propertyIdentifier = "";
    private Set<String> existingIdentifiers = new HashSet<>();

    // One option per candidate accessor role (shown only when relevant + candidates exist).
    private final AccessorOption setterOption    = new AccessorOption("Setter", Setter.class);
    private final AccessorOption updaterOption    = new AccessorOption("Updater", Updater.class);
    private final AccessorOption adderOption       = new AccessorOption("Adder", Adder.class);
    private final AccessorOption removerOption    = new AccessorOption("Remover", Remover.class);
    private final AccessorOption reindexerOption = new AccessorOption("Reindexer", Reindexer.class);

    @Override
    public String getLabel() {
        return "Promote as New Property…";
    }

    @Override
    public boolean isApplicable(Object target) {
        return target instanceof PromotableMethod
                && ((PromotableMethod) target).isValueGetterShape();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return "Promote Method to New Property";
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        PromotableMethod pm = (PromotableMethod) target;
        entity = pm.getEntity();
        methodName = pm.getMethodName();
        list = pm.returnsList();
        propertyTypeQN = list ? pm.getListElementTypeQualifiedName() : pm.getReturnTypeQualifiedName();
        existingIdentifiers = new HashSet<>(entity.getDeclaredProperties().keySet());
        propertyIdentifier = derivePropertyName(methodName);

        List<MethodSignature> candidates = entity.getCandidateAccessorMethods();
        String base = capitalisedBase(methodName);
        if (list) {
            // LIST property: adder / remover (1 param = element type), reindexer (element, int).
            adderOption.configure(oneParamCandidates(candidates, propertyTypeQN),
                    new String[] { "addTo" + base, "add" + base });
            removerOption.configure(oneParamCandidates(candidates, propertyTypeQN),
                    new String[] { "removeFrom" + base, "remove" + base });
            reindexerOption.configure(reindexerCandidates(candidates, propertyTypeQN),
                    new String[] { "reindex" + base, "setIndexFor" + base });
        } else {
            // SINGLE property: setter / updater (1 param = property type).
            setterOption.configure(oneParamCandidates(candidates, propertyTypeQN),
                    new String[] { "set" + base });
            updaterOption.configure(oneParamCandidates(candidates, propertyTypeQN),
                    new String[] { "update" + base });
        }
        return true;
    }

    @Override
    public boolean isInputValid() {
        return ModelEditingSupport.isAvailableIdentifier(propertyIdentifier, existingIdentifiers);
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();

        Map<Class<? extends Annotation>, String> extras = new LinkedHashMap<>();
        for (AccessorOption opt : new AccessorOption[] {
                setterOption, updaterOption, adderOption, removerOption, reindexerOption }) {
            if (opt.isShow() && opt.getInclude() && opt.getMethod() != null) {
                extras.put(opt.annotationType, opt.getMethod());
            }
        }
        entity.promoteGetterToProperty(methodName, propertyIdentifier.trim(), list, extras);
        return () -> model.getEntity(entityQN);
    }

    // --- candidate computation (by signature) -------------------------------

    /** Methods with exactly one parameter whose type matches {@code paramTypeQN}. */
    private static List<String> oneParamCandidates(List<MethodSignature> all, String paramTypeQN) {
        List<String> result = new ArrayList<>();
        if (paramTypeQN == null) {
            return result;
        }
        for (MethodSignature s : all) {
            if (s.getParameterCount() == 1 && paramTypeQN.equals(s.getParameterTypeQualifiedName(0))) {
                result.add(s.getMethodName());
            }
        }
        return result;
    }

    /** Methods {@code (T, int)} where {@code T} matches {@code elementTypeQN}. */
    private static List<String> reindexerCandidates(List<MethodSignature> all, String elementTypeQN) {
        List<String> result = new ArrayList<>();
        if (elementTypeQN == null) {
            return result;
        }
        for (MethodSignature s : all) {
            if (s.getParameterCount() == 2
                    && elementTypeQN.equals(s.getParameterTypeQualifiedName(0))
                    && isInt(s.getParameterTypeQualifiedName(1))) {
                result.add(s.getMethodName());
            }
        }
        return result;
    }

    private static boolean isInt(String qn) {
        return "int".equals(qn) || "java.lang.Integer".equals(qn);
    }

    // --- bound by PromoteGetterForm.fib -------------------------------------

    public String getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public void setPropertyIdentifier(String propertyIdentifier) {
        this.propertyIdentifier = propertyIdentifier;
        fireInputValidChanged();
    }

    /** Read-only label shown in the form (SINGLE / LIST), inferred from the return type. */
    public String getCardinalityLabel() {
        return list ? "LIST (collection)" : "SINGLE";
    }

    public AccessorOption getSetterOption()    { return setterOption; }
    public AccessorOption getUpdaterOption()   { return updaterOption; }
    public AccessorOption getAdderOption()     { return adderOption; }
    public AccessorOption getRemoverOption()   { return removerOption; }
    public AccessorOption getReindexerOption() { return reindexerOption; }

    // -------------------------------------------------------------------------

    /** {@code getName} → {@code name}, {@code isActive} → {@code active}, else the name as-is. */
    private static String derivePropertyName(String getterName) {
        String base = stripAccessorPrefix(getterName);
        if (base == null) {
            return getterName;
        }
        return base.isEmpty() ? getterName
                : Character.toLowerCase(base.charAt(0)) + base.substring(1);
    }

    /** {@code getName} → {@code Name} (capitalised base, for building sibling accessor names). */
    private static String capitalisedBase(String getterName) {
        String base = stripAccessorPrefix(getterName);
        if (base == null || base.isEmpty()) {
            return Character.toUpperCase(getterName.charAt(0)) + getterName.substring(1);
        }
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    private static String stripAccessorPrefix(String name) {
        if (name.startsWith("get") && name.length() > 3) {
            return name.substring(3);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return name.substring(2);
        }
        return null;
    }

    // =========================================================================

    /**
     * One candidate accessor role in the dialog: a "also promote …" checkbox + a dropdown of the
     * signature-matching methods. {@link #show} gates the row; {@link #include} + {@link #method}
     * are read at {@code applyMutation}.
     */
    public static final class AccessorOption {

        private final String label;
        final Class<? extends Annotation> annotationType;

        private boolean show;
        private List<String> candidates = Collections.emptyList();
        private boolean include;
        private String method;

        AccessorOption(String label, Class<? extends Annotation> annotationType) {
            this.label = label;
            this.annotationType = annotationType;
        }

        /** Sets up this option from the matching candidates; default-selects/checks by name. */
        void configure(List<String> candidates, String[] preferredNames) {
            this.candidates = candidates;
            this.show = !candidates.isEmpty();
            String nameMatch = null;
            for (String preferred : preferredNames) {
                if (candidates.contains(preferred)) {
                    nameMatch = preferred;
                    break;
                }
            }
            this.method = nameMatch != null ? nameMatch
                    : (candidates.isEmpty() ? null : candidates.get(0));
            // Default-check only on a confident (name-based) match — otherwise the user opts in.
            this.include = nameMatch != null;
        }

        public String getLabel()          { return label; }
        public boolean isShow()           { return show; }
        public List<String> getCandidates() { return candidates; }

        public boolean getInclude()       { return include; }
        public void setInclude(boolean b) { this.include = b; }

        public String getMethod()         { return method; }
        public void setMethod(String m)   { this.method = m; }
    }
}
