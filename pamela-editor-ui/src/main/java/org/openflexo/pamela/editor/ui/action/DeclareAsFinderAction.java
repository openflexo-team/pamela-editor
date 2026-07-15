package org.openflexo.pamela.editor.ui.action;

import javax.swing.ImageIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.openflexo.icon.IconMarker;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.PamelaProject;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Declares an existing one-argument method as an {@code @Finder} (model-editing-design.md §3.3a):
 * it searches a LIST property ({@code collection}) by matching the argument against an element
 * property ({@code attribute}). Applicable (by signature) when the method has exactly one parameter
 * and returns an element entity {@code E} (single finder) or {@code List<E>} (multi-valued), the
 * owning entity has a LIST property of element type {@code E}, and {@code E} has a property whose
 * type matches the argument.
 */
public class DeclareAsFinderAction extends ParameteredAction {

    public static final Resource FORM_FIB =
            ResourceLocator.locateResource("Fib/dialogs/DeclareFinderForm.fib");

    private SourceModelEntity entity;
    private SourceModelEntity elementEntity;
    private String methodName;
    private boolean multiValued;

    private List<SourceModelProperty> collectionChoices = Collections.emptyList();
    private SourceModelProperty collection;
    private List<SourceModelProperty> attributeChoices = Collections.emptyList();
    private SourceModelProperty attribute;

    @Override
    public String getLabel() {
        return loc("declare_as_finder_action");
    }

    @Override
    public ActionGroup getGroup() {
        return ActionGroup.PROMOTE;
    }

    @Override
    protected ImageIcon getBaseIcon() {
        return PamelaEditorIconLibrary.METHOD_ICON;
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
        if (pm.getParameterCount() != 1) {
            return false;
        }
        SourceModelEntity element = elementEntity(pm);
        return element != null
                && !collectionsFor(pm.getEntity(), element).isEmpty()
                && !attributesFor(element, pm.getParameterTypeQualifiedName(0)).isEmpty();
    }

    @Override
    public Resource getFormFib() {
        return FORM_FIB;
    }

    @Override
    protected String getDialogTitle() {
        return loc("declare_method_as_finder_title");
    }

    @Override
    protected boolean prepareDialog(Object target, PamelaEditorApplication app) {
        PromotableMethod pm = (PromotableMethod) target;
        entity = pm.getEntity();
        methodName = pm.getMethodName();
        multiValued = pm.returnsList();
        elementEntity = elementEntity(pm);
        if (elementEntity == null) {
            return false;
        }
        collectionChoices = collectionsFor(entity, elementEntity);
        attributeChoices = attributesFor(elementEntity, pm.getParameterTypeQualifiedName(0));
        if (collectionChoices.isEmpty() || attributeChoices.isEmpty()) {
            return false;
        }
        collection = collectionChoices.get(0);
        // PAMELA's conventional finder key is "name" — default to it when present.
        attribute = firstNamed(attributeChoices, "name");
        return true;
    }

    @Override
    public boolean isInputValid() {
        return collection != null && attribute != null;
    }

    @Override
    protected Supplier<Object> applyMutation(Object target, PamelaEditorApplication app,
            PamelaProject project) throws Exception {
        SourceMetaModel model = entity.getMetaModel();
        final String entityQN = entity.getQualifiedName();
        entity.declareFinder(methodName, collection.getPropertyIdentifier(),
                attribute.getPropertyIdentifier(), multiValued);
        return () -> model.getEntity(entityQN);
    }

    private static SourceModelProperty firstNamed(List<SourceModelProperty> props, String id) {
        for (SourceModelProperty p : props) {
            if (id.equals(p.getPropertyIdentifier())) {
                return p;
            }
        }
        return props.get(0);
    }

    // --- resolution helpers (by signature) ----------------------------------

    /** The element entity the finder returns ({@code E} for {@code E} or {@code List<E>}), or null. */
    private static SourceModelEntity elementEntity(PromotableMethod pm) {
        String elementQN = pm.returnsList()
                ? pm.getListElementTypeQualifiedName() : pm.getReturnTypeQualifiedName();
        if (elementQN == null) {
            return null;
        }
        SourceMetaModel model = pm.getEntity().getMetaModel();
        return model != null ? model.getEntity(elementQN) : null;
    }

    /** LIST properties of {@code owner} whose element type is {@code element}. */
    private static List<SourceModelProperty> collectionsFor(SourceModelEntity owner, SourceModelEntity element) {
        List<SourceModelProperty> result = new ArrayList<>();
        String elementQN = element.getQualifiedName();
        for (Map.Entry<String, SourceModelProperty> e : owner.getDeclaredProperties().entrySet()) {
            SourceModelProperty p = e.getValue();
            if (p.getCardinality() == Cardinality.LIST && p.getType() != null
                    && elementQN.equals(p.getType().getQualifiedName())) {
                result.add(p);
            }
        }
        return result;
    }

    /** Properties of the element entity (declared + inherited) whose type matches the search key. */
    private static List<SourceModelProperty> attributesFor(SourceModelEntity element, String keyTypeQN) {
        List<SourceModelProperty> result = new ArrayList<>();
        if (keyTypeQN == null) {
            return result;
        }
        for (Map.Entry<String, SourceModelProperty> e : element.getAllProperties().entrySet()) {
            SourceModelProperty p = e.getValue();
            if (p.getType() != null && keyTypeQN.equals(p.getType().getQualifiedName())) {
                result.add(p);
            }
        }
        return result;
    }

    // --- bound by DeclareFinderForm.fib -------------------------------------

    /** Owner entity — the collection {@code ModelPropertySelector} context. */
    public SourceModelEntity getEntity()                       { return entity; }
    /** Element entity — the attribute {@code ModelPropertySelector} context. */
    public SourceModelEntity getElementEntity()               { return elementEntity; }

    public List<SourceModelProperty> getCollectionChoices()    { return collectionChoices; }
    public SourceModelProperty getCollection()                 { return collection; }
    public void setCollection(SourceModelProperty c)           { this.collection = c; fireInputValidChanged(); }

    public List<SourceModelProperty> getAttributeChoices()     { return attributeChoices; }
    public SourceModelProperty getAttribute()                  { return attribute; }
    public void setAttribute(SourceModelProperty a)            { this.attribute = a; fireInputValidChanged(); }

    public boolean getMultiValued()            { return multiValued; }

    /** Read-only summary of the finder's return cardinality. */
    public String getMultiValuedLabel()        { return multiValued ? "List (multi-valued)" : "single"; }
}
