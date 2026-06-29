package org.openflexo.pamela.editor.ui.widget;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;

import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.model.SourceCustomMethod;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.ui.PamelaEditorApplication;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.pamela.editor.ui.PamelaEditorIconLibrary;
import org.openflexo.pamela.editor.ui.action.PromotableMethod;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * FIB controller for the Gina-based {@link SpoonOutlineView}.
 *
 * <p>Provides label formatting, icon resolution, and the click-to-navigate
 * callback for the Spoon-based Java Outline panel (ui-design.md §19.3).</p>
 *
 * <p>All logic is also exposed as <strong>static</strong> methods so that
 * {@link SpoonOutlineViewSwing} (the pure-Swing backup implementation) can
 * reuse the same label and icon computation without going through a FIBController
 * instance.</p>
 */
public class SpoonOutlineController extends PamelaEditorFIBController<SpoonOutlineModel> {

    private PamelaEditorApplication application;

    /**
     * Selection state for the outline browser.
     *
     * <p>Typed as {@code Object} (not {@code FIBModelObject}) because the tree
     * contains Spoon elements ({@code CtType}, {@code CtMethod}, etc.) which are
     * not {@code FIBModelObject}s.  The FIB uses
     * {@code selected="controller.outlineSelection"} to bind to this property.</p>
     */
    private Object outlineSelection;

    /**
     * The {@code Source*} element whose outline is currently shown
     * ({@link SourceModelEntity} or {@link org.openflexo.pamela.editor.model.SourceJavaFile}).
     * Set by {@link SpoonOutlineView} on every {@code showEntity}/{@code showJavaFile}/{@code clear};
     * the authoritative facet a right-clicked outline node maps to (see {@link #rightClick}).
     */
    private Object activeSourceFacet;

    public SpoonOutlineController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    public void setApplication(PamelaEditorApplication application) {
        this.application = application;
    }

    public void setActiveSourceFacet(Object facet) {
        this.activeSourceFacet = facet;
    }

    public Object getOutlineSelection() {
        return outlineSelection;
    }

    public void setOutlineSelection(Object value) {
        Object old = this.outlineSelection;
        this.outlineSelection = value;
        getPropertyChangeSupport().firePropertyChange("outlineSelection", old, value);
    }

    // -------------------------------------------------------------------------
    // Label helpers — instance API (called from FIB bindings via Connie)
    // -------------------------------------------------------------------------

    /** Returns a short display label for any Spoon element. */
    public String labelForObject(Object obj) {
        return labelFor(obj);
    }

    // -------------------------------------------------------------------------
    // Children helpers — instance API (called from FIB <Children> binding)
    // -------------------------------------------------------------------------

    /** Returns type members in source order. Works for any CtType. */
    public List<CtTypeMember> membersOf(CtType<?> type) {
        return memberList(type);
    }

    // -------------------------------------------------------------------------
    // Icon resolution — instance API (called from FIB icon binding)
    // -------------------------------------------------------------------------

    @Override
    protected ImageIcon retrieveIconForObject(Object object) {
        ImageIcon icon = iconFor(object);
        return icon != null ? icon : super.retrieveIconForObject(object);
    }

    // -------------------------------------------------------------------------
    // Click-to-navigate (called from FIB clickAction)
    // -------------------------------------------------------------------------

    /**
     * Called when the user clicks an element in the outline browser.
     * Scrolls the active {@link org.openflexo.pamela.editor.ui.widget.SourceCodeView}
     * to the element's line.
     */
    public void onElementSelected(Object element) {
        if (application == null || !(element instanceof CtElement)) return;
        SourcePosition pos = ((CtElement) element).getPosition();
        if (pos != null && pos.isValidPosition()) {
            application.scrollSourceViewToLine(pos.getLine());
        }
    }

    // -------------------------------------------------------------------------
    // Contextual menu (called from FIB rightClickAction)
    // -------------------------------------------------------------------------

    /**
     * Builds the contextual menu for a right-clicked outline node, mapping the Spoon node
     * to the {@code Source*} facet(s) it represents and delegating to the application's
     * shared menu builder — so the outline offers the same "promote" / refactor / delete
     * actions as the browsers and the diagram (model-editing-design.md §3.3, ui-design.md §18.4).
     */
    public void rightClick(Object selected, Object event) {
        if (application == null) return;
        List<Object> facets = facetsFor(selected);
        if (facets.isEmpty()) return;
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            application.showContextualMenuFor(facets, me.getComponent(), me.getX(), me.getY());
        } else {
            Point p = MouseInfo.getPointerInfo().getLocation();
            application.showContextualMenuFor(facets, null, p.x, p.y);
        }
    }

    /**
     * Maps a right-clicked outline node to the {@code Source*} facet(s) for the menu.
     *
     * <ul>
     *   <li>A {@code CtMethod} clicked inside an entity → the <em>underlying PAMELA concept</em>
     *       the method backs (its {@link SourceModelProperty}, {@link SourceModelInitializer} or
     *       {@link SourceCustomMethod}), so the menu offers the actions of <em>that member</em>
     *       (rename/retype/delete property, hide on diagram…) — <strong>not</strong> the entity's
     *       actions. A plain, not-yet-PAMELA method maps to a {@link PromotableMethod} facet, on
     *       which the method-level "promote" actions decide applicability by <strong>signature</strong>
     *       (getter→new property, declare as setter/adder/remover/reindexer/updater of an existing
     *       property — §3.3). This mirrors a diagram compartment row, which maps to its member
     *       facet only (ui-design.md §18.4).</li>
     *   <li>The type node ({@code CtType}) → the source element whose outline is shown
     *       ({@link SourceModelEntity} for entity actions, or a {@code SourceJavaFile} for
     *       "Declare as PAMELA entity" / "Add as root type").</li>
     *   <li>Any other node (field, constructor, enum value…) → no menu: these back no PAMELA
     *       concept.</li>
     * </ul>
     */
    private List<Object> facetsFor(Object selected) {
        if (activeSourceFacet == null) {
            return Collections.emptyList();
        }
        if (selected instanceof CtMethod && activeSourceFacet instanceof SourceModelEntity) {
            Object concept = resolveMethodConcept((SourceModelEntity) activeSourceFacet,
                    (CtMethod<?>) selected);
            return concept != null ? Collections.singletonList(concept) : Collections.emptyList();
        }
        if (selected instanceof CtType) {
            return Collections.singletonList(activeSourceFacet);
        }
        // Fields, constructors, enum values… have no underlying PAMELA concept → no menu.
        return Collections.emptyList();
    }

    /**
     * Resolves the PAMELA concept a clicked interface method backs, within {@code entity}:
     * a property (matched by accessor method name — unique within an entity), an initializer or
     * a custom method (matched by declaration line — disambiguates overloads). When the method
     * backs no existing concept it maps to a {@link PromotableMethod} facet (carrying the
     * signature) so the method-level "promote" actions can decide applicability by signature.
     */
    private static Object resolveMethodConcept(SourceModelEntity entity, CtMethod<?> m) {
        String name = m.getSimpleName();

        // 1. Property accessor (getter/setter/adder/remover/reindexer/updater)
        for (SourceModelProperty p : entity.getDeclaredProperties().values()) {
            if (name.equals(p.getGetterMethodName())
                    || name.equals(p.getSetterMethodName())
                    || name.equals(p.getAdderMethodName())
                    || name.equals(p.getRemoverMethodName())
                    || name.equals(p.getReindexerMethodName())
                    || name.equals(p.getUpdaterMethodName())) {
                return p;
            }
        }

        int line = (m.getPosition() != null && m.getPosition().isValidPosition())
                ? m.getPosition().getLine() : -1;

        // 2. Initializer (overloads disambiguated by declaration line)
        if (line > 0) {
            for (SourceModelInitializer init : entity.getInitializers()) {
                if (init.getDeclarationLine() == line) {
                    return init;
                }
            }
            // 3. Custom method / operation (finder, deleter, operation, plain)
            for (SourceCustomMethod cm : entity.getDeclaredCustomMethods()) {
                if (cm.getDeclarationLine() == line) {
                    return cm;
                }
            }
        }

        // 4. A plain, not-yet-PAMELA method → a promote facet (applicability decided by signature).
        return new PromotableMethod(entity, m);
    }

    // =========================================================================
    // Static API — shared with SpoonOutlineViewSwing (no Connie, no proxy)
    // =========================================================================

    /** Returns a short display label for any Spoon element. */
    @SuppressWarnings("unchecked")
    public static String labelFor(Object obj) {
        if (obj instanceof CtMethod) {
            CtMethod<?> m = (CtMethod<?>) obj;
            return m.getSimpleName() + "(" + shortParams(m.getParameters()) + ")"
                    /*+ " : " + m.getType().getSimpleName()*/;
        }
        if (obj instanceof CtConstructor) {
            CtConstructor<?> c = (CtConstructor<?>) obj;
            return c.getSimpleName() + "(" + shortParams(c.getParameters()) + ")";
        }
        if (obj instanceof CtField) {
            CtField<?> f = (CtField<?>) obj;
            return f.getSimpleName() + " : " + f.getType().getSimpleName();
        }
        if (obj instanceof CtEnumValue) {
            return ((CtEnumValue<?>) obj).getSimpleName();
        }
        if (obj instanceof CtType) {
            return ((CtType<?>) obj).getSimpleName();
        }
        return obj != null ? obj.toString() : "";
    }

    private static String shortParams(List<CtParameter<?>> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i).getType().getSimpleName());
        }
        return sb.toString();
    }

    /** Returns type members in source order. Works for any CtType. */
    public static List<CtTypeMember> memberList(CtType<?> type) {
        if (type == null) return Collections.emptyList();
        return new ArrayList<>(type.getTypeMembers());
    }

    /** Returns the appropriate Eclipse/JDT icon for any Spoon element. */
    @SuppressWarnings("unchecked")
    public static ImageIcon iconFor(Object object) {
        if (object instanceof CtInterface)    return iconForInterface((CtInterface<?>) object);
        if (object instanceof CtEnum)         return iconForEnum((CtEnum<?>) object);
        if (object instanceof CtAnnotationType) return PamelaEditorIconLibrary.JAVA_ANNOTATION_TYPE_ICON;
        if (object instanceof CtClass)        return iconForClass((CtClass<?>) object);
        if (object instanceof CtMethod)       return iconForMethod((CtMethod<?>) object);
        if (object instanceof CtConstructor)  return iconForConstructor((CtConstructor<?>) object);
        if (object instanceof CtField)        return iconForField((CtField<?>) object);
        if (object instanceof CtEnumValue)    return PamelaEditorIconLibrary.JAVA_FIELD_PUBLIC_ICON;
        return null;
    }

    private static ImageIcon iconForInterface(CtInterface<?> iface) {
        boolean inner = iface.getDeclaringType() != null;
        if (inner) {
            if (iface.isPublic())    return PamelaEditorIconLibrary.JAVA_INNER_INTERFACE_PUBLIC_ICON;
            if (iface.isProtected()) return PamelaEditorIconLibrary.JAVA_INNER_INTERFACE_PROTECTED_ICON;
            if (iface.isPrivate())   return PamelaEditorIconLibrary.JAVA_INNER_INTERFACE_PRIVATE_ICON;
            return PamelaEditorIconLibrary.JAVA_INNER_INTERFACE_DEFAULT_ICON;
        }
        return iface.isPublic()
                ? PamelaEditorIconLibrary.JAVA_INTERFACE_ICON
                : PamelaEditorIconLibrary.JAVA_INTERFACE_DEFAULT_ICON;
    }

    private static ImageIcon iconForClass(CtClass<?> cls) {
        boolean inner = cls.getDeclaringType() != null;
        if (inner) {
            if (cls.isPublic())    return PamelaEditorIconLibrary.JAVA_INNER_CLASS_PUBLIC_ICON;
            if (cls.isProtected()) return PamelaEditorIconLibrary.JAVA_INNER_CLASS_PROTECTED_ICON;
            if (cls.isPrivate())   return PamelaEditorIconLibrary.JAVA_INNER_CLASS_PRIVATE_ICON;
            return PamelaEditorIconLibrary.JAVA_INNER_CLASS_DEFAULT_ICON;
        }
        return cls.isPublic()
                ? PamelaEditorIconLibrary.JAVA_CLASS_ICON
                : PamelaEditorIconLibrary.JAVA_CLASS_DEFAULT_ICON;
    }

    private static ImageIcon iconForEnum(CtEnum<?> e) {
        if (e.isPrivate())   return PamelaEditorIconLibrary.JAVA_ENUM_PRIVATE_ICON;
        if (e.isProtected()) return PamelaEditorIconLibrary.JAVA_ENUM_PROTECTED_ICON;
        if (e.isPublic())    return PamelaEditorIconLibrary.JAVA_ENUM_ICON;
        return PamelaEditorIconLibrary.JAVA_ENUM_DEFAULT_ICON;
    }

    private static ImageIcon iconForMethod(CtMethod<?> m) {
        if (m.isPrivate())   return PamelaEditorIconLibrary.JAVA_METHOD_PRIVATE_ICON;
        if (m.isProtected()) return PamelaEditorIconLibrary.JAVA_METHOD_PROTECTED_ICON;
        if (m.isPublic())    return PamelaEditorIconLibrary.JAVA_METHOD_PUBLIC_ICON;
        return PamelaEditorIconLibrary.JAVA_METHOD_DEFAULT_ICON;
    }

    private static ImageIcon iconForConstructor(CtConstructor<?> c) {
        if (c.isPrivate())   return PamelaEditorIconLibrary.JAVA_METHOD_PRIVATE_ICON;
        if (c.isProtected()) return PamelaEditorIconLibrary.JAVA_METHOD_PROTECTED_ICON;
        if (c.isPublic())    return PamelaEditorIconLibrary.JAVA_METHOD_PUBLIC_ICON;
        return PamelaEditorIconLibrary.JAVA_METHOD_DEFAULT_ICON;
    }

    private static ImageIcon iconForField(CtField<?> f) {
        if (f.isPrivate())   return PamelaEditorIconLibrary.JAVA_FIELD_PRIVATE_ICON;
        if (f.isProtected()) return PamelaEditorIconLibrary.JAVA_FIELD_PROTECTED_ICON;
        if (f.isPublic())    return PamelaEditorIconLibrary.JAVA_FIELD_PUBLIC_ICON;
        return PamelaEditorIconLibrary.JAVA_FIELD_DEFAULT_ICON;
    }
}
