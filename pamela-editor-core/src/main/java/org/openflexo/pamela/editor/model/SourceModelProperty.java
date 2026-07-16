package org.openflexo.pamela.editor.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.lang.annotation.Annotation;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.CloningStrategy;
import org.openflexo.pamela.annotations.CloningStrategy.StrategyType;
import org.openflexo.pamela.annotations.Embedded;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.Reindexer;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.ReturnedValue;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.Updater;
import org.openflexo.pamela.annotations.XMLAttribute;
import org.openflexo.pamela.annotations.XMLElement;

import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

/**
 * Corresponds to PAMELA's {@code ModelProperty<I>}.
 * Represents one property (identified by a string key) declared on a
 * {@link SourceModelEntity}.
 *
 * <p>A property is centred on its {@code @Getter}-annotated method; all other
 * method roles ({@code @Setter}, {@code @Adder}, {@code @Remover},
 * {@code @Reindexer}, {@code @Updater}) are attached after the getter is
 * found.</p>
 *
 * <p>Validation rules (fire {@link Issue}s during Phase 2):
 * <ul>
 *   <li>{@code @Setter} on a LIST property → {@link Error}</li>
 *   <li>{@code @Adder} or {@code @Remover} on a SINGLE property → {@link Error}</li>
 *   <li>{@code @Adder} present but {@code @Remover} absent (or vice versa) → {@link Warning}</li>
 *   <li>{@code isDerived = true} but no {@code @ReturnedValue} annotation → {@link Warning}</li>
 *   <li>{@code inverse} declared but target property not found after Phase 3 → {@link Error}</li>
 * </ul>
 * </p>
 */
public class SourceModelProperty implements SourceElement,
        org.openflexo.toolbox.HasPropertyChangeSupport {

    private final java.beans.PropertyChangeSupport pcSupport =
            new java.beans.PropertyChangeSupport(this);

    // Internal Spoon references — never exposed in the public API
    private final CtMethod<?> ctGetter;
    private CtMethod<?> ctSetter;
    private CtMethod<?> ctAdder;
    private CtMethod<?> ctRemover;
    private CtMethod<?> ctReindexer;
    private CtMethod<?> ctUpdater;

    // Identity
    private final String propertyIdentifier;
    private final SourceModelEntity modelEntity;

    // Identifier source style (literal vs static-final-String constant) — derived from the
    // raw @Getter.value AST. See property-identifier-constant-design.md.
    private final PropertyIdentifierStyle identifierStyle;
    private final String referencedConstantName; // constant simple name when CONSTANT, else null

    // Cardinality
    private final Cardinality cardinality;

    // Type
    private final SourceType type;

    // Method names (public, derived from Spoon)
    private final String getterMethodName;
    private String setterMethodName;
    private String adderMethodName;
    private String removerMethodName;
    private String reindexerMethodName;
    private String updaterMethodName;

    // @Getter parameters — non-final: editable via the inspector (Lot 4)
    private String defaultValue;
    private boolean derived;
    private boolean ignoreType;
    private boolean ignoreForEquality;
    private boolean allowsMultipleOccurrences;

    // Relationships
    private final String inversePropertyIdentifier;
    private SourceModelProperty inverseProperty; // resolved during Phase 3

    // Embedding (@Embedded) — non-final: editable via the inspector (Lot 4)
    private boolean embedded;
    private final List<String> embeddedClosureConditions;
    private final List<String> embeddedDeletionConditions;

    // Serialization
    private boolean xmlAttributePresent;       // @XMLAttribute on the getter
    private String xmlAttributeName;           // from @XMLAttribute, null if absent (effective name)
    private String xmlAttributeTag;            // raw @XMLAttribute.xmlTag(), null if empty/absent
    private boolean xmlElementPresent;         // @XMLElement on the getter
    private String xmlElementTag;              // raw @XMLElement.xmlTag(), null if empty/absent
    private boolean primaryXmlElement;         // from @XMLElement.primary()
    private String xmlElementContext;          // raw @XMLElement.context(), null if empty/absent

    // Cloning
    private final StrategyType cloningStrategyType; // null if @CloningStrategy is absent

    // Issues detected during construction
    private final List<Issue> issues;

    /**
     * Constructs a {@code SourceModelProperty} from its {@code @Getter} method.
     * The setter/adder/remover/reindexer/updater are registered afterwards via
     * the {@code set*()} mutators.
     *
     * @param ctGetter    the {@code @Getter}-annotated method (must not be {@code null})
     * @param modelEntity the entity where this property is declared
     */
    public SourceModelProperty(CtMethod<?> ctGetter, SourceModelEntity modelEntity) {
        this.ctGetter = ctGetter;
        this.modelEntity = modelEntity;
        this.issues = new ArrayList<>();

        Getter getterAnnotation = ctGetter.getAnnotation(Getter.class);
        this.propertyIdentifier = getterAnnotation.value();
        this.defaultValue = getterAnnotation.defaultValue();
        this.derived = getterAnnotation.isDerived();
        this.ignoreType = getterAnnotation.ignoreType();
        this.ignoreForEquality = getterAnnotation.ignoreForEquality();
        this.allowsMultipleOccurrences = getterAnnotation.allowsMultipleOccurences();
        this.inversePropertyIdentifier = getterAnnotation.inverse();

        // Detect the identifier style from the raw @Getter.value AST (not the proxy, which
        // resolves the constant to its value). A CtFieldRead is a constant reference; a
        // CtLiteral is a string literal. See property-identifier-constant-design.md §2/§4.
        PropertyIdentifierStyle style = PropertyIdentifierStyle.LITERAL;
        String constantName = null;
        CtAnnotation<?> rawGetter = ctGetter.getAnnotation(
                ctGetter.getFactory().Type().createReference(Getter.class));
        if (rawGetter != null
                && rawGetter.getValue("value") instanceof spoon.reflect.code.CtFieldRead<?>) {
            style = PropertyIdentifierStyle.CONSTANT;
            constantName = ((spoon.reflect.code.CtFieldRead<?>) rawGetter.getValue("value"))
                    .getVariable().getSimpleName();
        }
        this.identifierStyle = style;
        this.referencedConstantName = constantName;

        this.getterMethodName = ctGetter.getSimpleName();

        // Determine cardinality and extract element type
        CtTypeReference<?> getterReturnType = ctGetter.getType();
        boolean isList = isListType(getterReturnType);
        this.cardinality = isList ? Cardinality.LIST : Cardinality.SINGLE;

        CtTypeReference<?> elementTypeRef;
        if (isList) {
            List<CtTypeReference<?>> args = getterReturnType.getActualTypeArguments();
            if (args != null && !args.isEmpty()) {
                elementTypeRef = args.get(0);
            } else {
                // Raw List — use the list type itself, record a warning
                elementTypeRef = getterReturnType;
                addIssue(new Warning("Property '" + propertyIdentifier
                        + "' has a raw List return type — no element type argument found", this));
            }
        } else {
            elementTypeRef = getterReturnType;
        }
        this.type = new SourceType(elementTypeRef);

        // @Embedded
        Embedded embeddedAnnotation = ctGetter.getAnnotation(Embedded.class);
        if (embeddedAnnotation != null) {
            this.embedded = true;
            this.embeddedClosureConditions = Collections.unmodifiableList(
                    Arrays.asList(embeddedAnnotation.closureConditions()));
            this.embeddedDeletionConditions = Collections.unmodifiableList(
                    Arrays.asList(embeddedAnnotation.deletionConditions()));
        } else {
            this.embedded = false;
            this.embeddedClosureConditions = Collections.emptyList();
            this.embeddedDeletionConditions = Collections.emptyList();
        }

        // @XMLAttribute
        XMLAttribute xmlAttr = ctGetter.getAnnotation(XMLAttribute.class);
        this.xmlAttributePresent = xmlAttr != null;
        this.xmlAttributeTag = (xmlAttr != null && !xmlAttr.xmlTag().isEmpty()) ? xmlAttr.xmlTag() : null;
        this.xmlAttributeName = (xmlAttr != null)
                ? (xmlAttr.xmlTag().isEmpty() ? propertyIdentifier : xmlAttr.xmlTag())
                : null;

        // @XMLElement on getter
        XMLElement xmlElem = ctGetter.getAnnotation(XMLElement.class);
        this.xmlElementPresent = xmlElem != null;
        if (xmlElem != null) {
            this.xmlElementTag = xmlElem.xmlTag().isEmpty() ? null : xmlElem.xmlTag();
            this.primaryXmlElement = xmlElem.primary();
            this.xmlElementContext = xmlElem.context().isEmpty() ? null : xmlElem.context();
        } else {
            this.xmlElementTag = null;
            this.primaryXmlElement = false;
            this.xmlElementContext = null;
        }

        // @CloningStrategy
        CloningStrategy cloningStrategyAnnotation = ctGetter.getAnnotation(CloningStrategy.class);
        this.cloningStrategyType = (cloningStrategyAnnotation != null)
                ? cloningStrategyAnnotation.value()
                : null;

        // Validate isDerived without @ReturnedValue
        if (derived && ctGetter.getAnnotation(ReturnedValue.class) == null) {
            addIssue(new Warning("Property '" + propertyIdentifier
                    + "' is declared isDerived=true but has no @ReturnedValue annotation", this));
        }
    }

    // -------------------------------------------------------------------------
    // Secondary-role setters (called by Phase 2 of meta-model construction)
    // -------------------------------------------------------------------------

    /**
     * Registers the {@code @Setter} method for this property.
     * Fires an {@link Error} if cardinality is LIST.
     */
    void registerSetter(CtMethod<?> method) {
        if (cardinality == Cardinality.LIST) {
            addIssue(new Error("@Setter '" + method.getSimpleName()
                    + "' is not allowed on LIST property '" + propertyIdentifier + "'", this));
        }
        this.ctSetter = method;
        this.setterMethodName = method.getSimpleName();
    }

    /**
     * Registers the {@code @Adder} method for this property.
     * Fires an {@link Error} if cardinality is SINGLE.
     */
    void registerAdder(CtMethod<?> method) {
        if (cardinality == Cardinality.SINGLE) {
            addIssue(new Error("@Adder '" + method.getSimpleName()
                    + "' is not allowed on SINGLE property '" + propertyIdentifier + "'", this));
        }
        this.ctAdder = method;
        this.adderMethodName = method.getSimpleName();
    }

    /**
     * Registers the {@code @Remover} method for this property.
     * Fires an {@link Error} if cardinality is SINGLE.
     */
    void registerRemover(CtMethod<?> method) {
        if (cardinality == Cardinality.SINGLE) {
            addIssue(new Error("@Remover '" + method.getSimpleName()
                    + "' is not allowed on SINGLE property '" + propertyIdentifier + "'", this));
        }
        this.ctRemover = method;
        this.removerMethodName = method.getSimpleName();
    }

    /** Registers the {@code @Reindexer} method for this property. */
    void registerReindexer(CtMethod<?> method) {
        this.ctReindexer = method;
        this.reindexerMethodName = method.getSimpleName();
    }

    /** Registers the {@code @Updater} method for this property. */
    void registerUpdater(CtMethod<?> method) {
        this.ctUpdater = method;
        this.updaterMethodName = method.getSimpleName();
    }

    /**
     * Validates that adder/remover consistency holds after all methods have
     * been registered.  Should be called at the end of Phase 2 for each
     * property.
     */
    void validateAdderRemoverConsistency() {
        if (cardinality == Cardinality.LIST) {
            if (ctAdder != null && ctRemover == null) {
                addIssue(new Warning("Property '" + propertyIdentifier
                        + "' has an @Adder but no @Remover", this));
            }
            if (ctAdder == null && ctRemover != null) {
                addIssue(new Warning("Property '" + propertyIdentifier
                        + "' has a @Remover but no @Adder", this));
            }
        }
    }

    /**
     * Checks that the secondary accessors ({@code @Setter}/{@code @Adder}/{@code @Remover}/
     * {@code @Reindexer}/{@code @Updater}) write their identifier in the same style as the getter
     * (the getter is authoritative — {@code property-identifier-constant-design.md §4}). Fires a
     * single {@link Warning} on the first mismatch; does <b>not</b> change the retained style.
     * Must be called after all accessors have been registered (end of Phase 2).
     */
    void validateIdentifierStyleConsistency() {
        if (rawStyleDiffers(ctSetter, Setter.class)
                || rawStyleDiffers(ctAdder, Adder.class)
                || rawStyleDiffers(ctRemover, Remover.class)
                || rawStyleDiffers(ctReindexer, Reindexer.class)
                || rawStyleDiffers(ctUpdater, Updater.class)) {
            addIssue(new Warning("Property '" + propertyIdentifier
                    + "' mixes constant and literal identifiers across its accessors", this));
        }
    }

    /** {@code true} if the accessor is present and writes its identifier in a different style. */
    private boolean rawStyleDiffers(CtMethod<?> method, Class<? extends Annotation> annotationType) {
        if (method == null) {
            return false;
        }
        CtAnnotation<?> ann = method.getAnnotation(
                method.getFactory().Type().createReference(annotationType));
        if (ann == null) {
            return false;
        }
        PropertyIdentifierStyle style =
                (ann.getValue("value") instanceof spoon.reflect.code.CtFieldRead<?>)
                        ? PropertyIdentifierStyle.CONSTANT : PropertyIdentifierStyle.LITERAL;
        return style != identifierStyle;
    }

    /**
     * Called during Phase 3 to establish the resolved inverse-property link.
     *
     * @param inverse the resolved inverse property (must not be {@code null})
     */
    void setInverseProperty(SourceModelProperty inverse) {
        this.inverseProperty = inverse;
    }

    // =========================================================================
    // Mutation operation
    // =========================================================================

    /**
     * Removes this property from its declaring entity.
     *
     * <p>All Spoon method nodes associated with this property (getter, setter,
     * adder, remover, reindexer, updater) are removed from the owning
     * {@link CtType}.  The property is also removed from
     * {@link SourceModelEntity#getDeclaredProperties()}.
     * The compilation unit is saved to disk afterwards.</p>
     *
     * @throws IOException if saving the compilation unit fails
     */
    public void remove() throws IOException {
        spoon.reflect.declaration.CtType<?> ct = modelEntity.getCtType();
        if (ctGetter != null) {
            ct.removeMethod(ctGetter);
        }
        if (ctSetter != null) {
            ct.removeMethod(ctSetter);
        }
        if (ctAdder != null) {
            ct.removeMethod(ctAdder);
        }
        if (ctRemover != null) {
            ct.removeMethod(ctRemover);
        }
        if (ctReindexer != null) {
            ct.removeMethod(ctReindexer);
        }
        if (ctUpdater != null) {
            ct.removeMethod(ctUpdater);
        }
        modelEntity.removeDeclaredProperty(propertyIdentifier);
        modelEntity.getCompilationUnit().regenerateFromAST();
    }

    /**
     * Changes the value type of this property (C19 retype).
     *
     * <p>For a SINGLE property the getter return type, the setter parameter and
     * the updater parameter are retyped. For a LIST property the getter's
     * {@code List<T>} element type and the adder/remover/reindexer parameters are
     * retyped. Writes the change back through the entity's compilation unit.</p>
     *
     * <p>The in-memory {@link SourceType} is not updated — the property is
     * recreated from source on the next rebuild (the caller's responsibility).</p>
     *
     * @param newQualifiedTypeName fully qualified type name (SINGLE) or element
     *                             type name (LIST), e.g. {@code "java.lang.String"}
     */
    public void changeType(String newQualifiedTypeName) throws IOException {
        Factory factory = ctGetter.getFactory();
        CtTypeReference<?> newType = factory.Type().createReference(newQualifiedTypeName);

        if (cardinality == Cardinality.LIST) {
            // getter returns List<newType>
            CtTypeReference<Object> listRef = (CtTypeReference<Object>) factory.Type()
                    .createReference("java.util.List");
            listRef.addActualTypeArgument(newType.clone());
            ((CtMethod<Object>) ctGetter).setType(listRef);
            setSingleParamType(ctAdder, newType);
            setSingleParamType(ctRemover, newType);
            setSingleParamType(ctReindexer, newType);
        } else {
            ((CtMethod<Object>) ctGetter).setType((CtTypeReference) newType.clone());
            setSingleParamType(ctSetter, newType);
            setSingleParamType(ctUpdater, newType);
        }
        modelEntity.getCompilationUnit().regenerateFromAST();
    }

    /**
     * Renames this property (C19): changes the PAMELA key (the {@code value} of
     * every accessor annotation) and renames the accessor methods to the matching
     * convention ({@code getXxx}/{@code isXxx}, {@code setXxx}, {@code addToXxx},
     * {@code removeFromXxx}). If this property has a resolved inverse, the inverse
     * property's {@code @Getter(inverse = …)} is updated to the new key.
     *
     * <p>Convenience form that renames <em>every</em> present accessor method to its
     * convention (getter/setter/updater/adder/remover/reindexer). Equivalent to
     * {@link #rename(String, Map)} with {@link #conventionalMethodNames}.</p>
     *
     * @param newIdentifier the new property key
     */
    public void rename(String newIdentifier) throws IOException {
        rename(newIdentifier, conventionalMethodNames(newIdentifier));
    }

    /**
     * Renames this property (C19), giving explicit control over which accessor
     * methods are also renamed. The PAMELA key (the {@code value} of every present
     * accessor annotation) is always updated to {@code newIdentifier}. Each entry
     * of {@code methodRenames} additionally renames the corresponding accessor
     * <em>method</em> to the supplied name; an accessor whose {@link AccessorSpec.Role}
     * is absent from the map keeps its current method name (only its annotation
     * value changes). If this property has a resolved inverse, the inverse
     * property's {@code @Getter(inverse = …)} is updated to the new key.
     *
     * <p>Limitations: direct calls to the renamed methods in implementation classes
     * / custom code are not rewritten.</p>
     *
     * @param newIdentifier the new property key
     * @param methodRenames per-role new method names (only the present accessors
     *        whose role appears here are renamed); {@code null} is treated as empty
     */
    public void rename(String newIdentifier, Map<AccessorSpec.Role, String> methodRenames)
            throws IOException {
        Factory factory = ctGetter.getFactory();
        Map<AccessorSpec.Role, String> renames =
                methodRenames == null ? Collections.emptyMap() : methodRenames;

        renameAccessor(ctGetter, Getter.class, AccessorSpec.Role.GETTER, newIdentifier, renames, factory);
        renameAccessor(ctSetter, Setter.class, AccessorSpec.Role.SETTER, newIdentifier, renames, factory);
        renameAccessor(ctAdder, Adder.class, AccessorSpec.Role.ADDER, newIdentifier, renames, factory);
        renameAccessor(ctRemover, Remover.class, AccessorSpec.Role.REMOVER, newIdentifier, renames, factory);
        renameAccessor(ctReindexer, Reindexer.class, AccessorSpec.Role.REINDEXER, newIdentifier, renames, factory);
        renameAccessor(ctUpdater, Updater.class, AccessorSpec.Role.UPDATER, newIdentifier, renames, factory);

        modelEntity.getCompilationUnit().regenerateFromAST();

        // Update the other side of an inverse relationship, if any.
        if (inverseProperty != null) {
            setAnnotationValue(inverseProperty.ctGetter, Getter.class, "inverse",
                    newIdentifier, factory);
            inverseProperty.modelEntity.getCompilationUnit().regenerateFromAST();
        }
    }

    /**
     * Updates one accessor's PAMELA key ({@code value}) and, when a new name is
     * supplied for its role, renames the method. No-op when the accessor is absent.
     */
    private static void renameAccessor(CtMethod<?> accessor,
            Class<? extends Annotation> annotationType, AccessorSpec.Role role,
            String newIdentifier, Map<AccessorSpec.Role, String> renames, Factory factory) {
        if (accessor == null) {
            return;
        }
        setAnnotationValue(accessor, annotationType, "value", newIdentifier, factory);
        String newName = renames.get(role);
        if (newName != null && !newName.isEmpty()) {
            accessor.setSimpleName(newName);
        }
    }

    /**
     * The conventional accessor-method names for a rename to {@code newIdentifier}:
     * {@code getXxx}/{@code isXxx}, {@code setXxx}, {@code updateXxx}, {@code addToXxx},
     * {@code removeFromXxx}, {@code reindexXxx} for every present accessor. Used by the
     * convenience {@link #rename(String)} form, which renames all present accessor methods
     * to their convention.
     */
    private Map<AccessorSpec.Role, String> conventionalMethodNames(String newIdentifier) {
        String cap = capitalise(newIdentifier);
        Map<AccessorSpec.Role, String> names = new LinkedHashMap<>();
        String prefix = getterMethodName.startsWith("is") ? "is" : "get";
        names.put(AccessorSpec.Role.GETTER, prefix + cap);
        if (ctSetter != null) {
            names.put(AccessorSpec.Role.SETTER, "set" + cap);
        }
        if (ctUpdater != null) {
            names.put(AccessorSpec.Role.UPDATER, "update" + cap);
        }
        if (ctAdder != null) {
            names.put(AccessorSpec.Role.ADDER, "addTo" + cap);
        }
        if (ctRemover != null) {
            names.put(AccessorSpec.Role.REMOVER, "removeFrom" + cap);
        }
        if (ctReindexer != null) {
            names.put(AccessorSpec.Role.REINDEXER, "reindex" + cap);
        }
        return names;
    }

    /**
     * Adds or removes the {@code @Embedded} annotation on this property's getter,
     * switching it between composition ({@code @Embedded}) and a plain reference
     * (C10). Uses a targeted text edit (like {@link SourceMetaModel#declareAsEntity}),
     * not an AST re-print. The change materialises on the next rebuild.
     *
     * @param embedded {@code true} to add {@code @Embedded}, {@code false} to remove it
     */
    public void setEmbedded(boolean embedded) throws IOException {
        if (embedded == this.embedded) {
            return;
        }
        requireGetterPosition();
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String source = cu.getText();
        int start = ctGetter.getPosition().getSourceStart();
        String edited = embedded
                ? SourceAnnotationEditor.addAnnotation(source, start, "Embedded", Embedded.class.getName())
                : SourceAnnotationEditor.removeAnnotation(source, start, "Embedded");
        cu.setText(edited);
        boolean old = this.embedded;
        this.embedded = embedded;
        pcSupport.firePropertyChange("embedded", old, embedded);
    }

    /**
     * Editable inspector setters for {@code @Getter} parameters (Lot 4). Each
     * mutates the source via a targeted annotation-parameter text edit, updates
     * the in-memory field and fires a {@code PropertyChange} — but does <b>not</b>
     * rebuild (the UI reacts to the event). See {@code model-editing-design.md §6}.
     */
    public void setDerived(boolean derived) throws IOException {
        if (derived == this.derived) {
            return;
        }
        editGetterParameter("isDerived", derived ? "true" : null);
        boolean old = this.derived;
        this.derived = derived;
        pcSupport.firePropertyChange("derived", old, derived);
    }

    public void setIgnoreType(boolean ignoreType) throws IOException {
        if (ignoreType == this.ignoreType) {
            return;
        }
        editGetterParameter("ignoreType", ignoreType ? "true" : null);
        boolean old = this.ignoreType;
        this.ignoreType = ignoreType;
        pcSupport.firePropertyChange("ignoreType", old, ignoreType);
    }

    public void setIgnoreForEquality(boolean ignoreForEquality) throws IOException {
        if (ignoreForEquality == this.ignoreForEquality) {
            return;
        }
        editGetterParameter("ignoreForEquality", ignoreForEquality ? "true" : null);
        boolean old = this.ignoreForEquality;
        this.ignoreForEquality = ignoreForEquality;
        pcSupport.firePropertyChange("ignoreForEquality", old, ignoreForEquality);
    }

    public void setAllowsMultipleOccurrences(boolean allows) throws IOException {
        if (allows == this.allowsMultipleOccurrences) {
            return;
        }
        // Note the PAMELA annotation spelling: allowsMultipleOccurences.
        editGetterParameter("allowsMultipleOccurences", allows ? "true" : null);
        boolean old = this.allowsMultipleOccurrences;
        this.allowsMultipleOccurrences = allows;
        pcSupport.firePropertyChange("allowsMultipleOccurrences", old, allows);
    }

    public void setDefaultValue(String defaultValue) throws IOException {
        String normalized = defaultValue == null ? "" : defaultValue;
        if (normalized.equals(this.defaultValue)) {
            return;
        }
        String valueExpr = normalized.isEmpty() ? null : "\"" + normalized + "\"";
        editGetterParameter("defaultValue", valueExpr);
        String old = this.defaultValue;
        this.defaultValue = normalized;
        pcSupport.firePropertyChange("defaultValue", old, normalized);
    }

    /** Targeted text edit of a {@code @Getter} parameter on this property's getter. */
    private void editGetterParameter(String parameter, String valueExpr) throws IOException {
        requireGetterPosition();
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String source = cu.getText();
        String edited = SourceAnnotationEditor.setAnnotationParameter(
                source, ctGetter.getPosition().getSourceStart(), "Getter", parameter, valueExpr);
        cu.setText(edited);
    }

    /** Package-private: whether the getter has a usable source position (for bulk edits). */
    boolean hasValidGetterPosition() {
        return ctGetter.getPosition() != null && ctGetter.getPosition().isValidPosition();
    }

    /** Package-private: source offset of the getter declaration (for ordered bulk edits). */
    int getterSourceStart() {
        requireGetterPosition();
        return ctGetter.getPosition().getSourceStart();
    }

    private void requireGetterPosition() {
        if (ctGetter.getPosition() == null || !ctGetter.getPosition().isValidPosition()) {
            throw new IllegalStateException("No source position for getter " + getterMethodName);
        }
    }

    /** The editable counterpart of {@link #getInverseProperty()} (bound by the inspector). */
    public SourceModelProperty getInverse() {
        return inverseProperty;
    }

    /**
     * Sets (or clears, when {@code newInverse} is {@code null}) this property's inverse — the
     * editable counterpart of the build-time {@link #setInverseProperty}. Writes
     * {@code @Getter(inverse = …)} on <b>both</b> sides (this getter and the chosen property's
     * getter) and clears the previous inverse's parameter, then fires {@code "inverse"} so the
     * application reacts (rebuild — model-editing-design.md §6). Targeted text edits on the
     * in-memory buffer(s); edits that share a compilation unit (a self-referential entity, e.g.
     * {@code parent}/{@code children}) are applied in descending source order so offsets stay valid.
     */
    public void setInverse(SourceModelProperty newInverse) throws IOException {
        if (newInverse == inverseProperty) {
            return;
        }
        if (newInverse == this) {
            throw new IllegalArgumentException("A property cannot be its own inverse");
        }
        SourceModelProperty old = inverseProperty;

        List<InverseEdit> edits = new ArrayList<>();
        edits.add(new InverseEdit(this, newInverse == null ? null : literal(newInverse.propertyIdentifier)));
        if (newInverse != null) {
            edits.add(new InverseEdit(newInverse, literal(this.propertyIdentifier)));
        }
        if (old != null && old != newInverse) {
            edits.add(new InverseEdit(old, null));
        }
        applyInverseEdits(edits);

        // inversePropertyIdentifier is final (re-derived on the rebuild the event triggers).
        this.inverseProperty = newInverse;
        pcSupport.firePropertyChange("inverse", old, newInverse);
    }

    /**
     * Fires a UI-intent signal that the user asked to change this property's type. The model does
     * not open dialogs; the application observes the inspected element and runs the
     * {@code ChangePropertyType} action in response (model-editing-design.md §6).
     */
    public void requestChangeType() {
        pcSupport.firePropertyChange("changeTypeRequested", null, this);
    }

    /**
     * Fires a UI-intent signal that the user asked to rename this property identifier. The model does
     * not open dialogs; the application observes the inspected element and runs the
     * {@code RenameProperty} action in response .
     */
    public void requestRename() {
    	System.out.println("Hop on rename...");
        pcSupport.firePropertyChange("renameRequested", null, this);
    }

  private static String literal(String s) {
        return "\"" + s + "\"";
    }

    /** A single {@code @Getter(inverse=…)} parameter edit on one property's getter. */
    private static final class InverseEdit {
        final SourceModelProperty property;
        final String valueExpr; // null = remove the inverse parameter

        InverseEdit(SourceModelProperty property, String valueExpr) {
            this.property = property;
            this.valueExpr = valueExpr;
        }
    }

    /** Applies the inverse-parameter edits, batching per compilation unit (descending source order). */
    private static void applyInverseEdits(List<InverseEdit> edits) throws IOException {
        Map<SourceCompilationUnit, List<InverseEdit>> byCU = new LinkedHashMap<>();
        for (InverseEdit e : edits) {
            e.property.requireGetterPosition();
            byCU.computeIfAbsent(e.property.modelEntity.getCompilationUnit(), k -> new ArrayList<>()).add(e);
        }
        for (Map.Entry<SourceCompilationUnit, List<InverseEdit>> entry : byCU.entrySet()) {
            List<InverseEdit> list = entry.getValue();
            list.sort((a, b) -> Integer.compare(
                    b.property.ctGetter.getPosition().getSourceStart(),
                    a.property.ctGetter.getPosition().getSourceStart()));
            String src = entry.getKey().getText();
            for (InverseEdit e : list) {
                src = SourceAnnotationEditor.setAnnotationParameter(
                        src, e.property.ctGetter.getPosition().getSourceStart(),
                        "Getter", "inverse", e.valueExpr);
            }
            entry.getKey().setText(src);
        }
    }

    /**
     * Adds a {@code @Setter} accessor to a SINGLE property that lacks one (C8).
     * Generates {@code void setXxx(T)} and writes it back via the AST printer.
     * No-op if a setter already exists.
     */
    public void addSetter() throws IOException {
        if (cardinality != Cardinality.SINGLE) {
            throw new IllegalStateException("addSetter applies to SINGLE properties only");
        }
        if (ctSetter != null) {
            return;
        }
        Factory factory = ctGetter.getFactory();
        CtMethod<Void> setter = buildVoidAccessor(factory, "set" + capitalise(propertyIdentifier),
                ctGetter.getType(), Setter.class);
        modelEntity.addGeneratedAccessor(setter, propertyIdentifier);
        modelEntity.getCompilationUnit().regenerateFromAST();
    }

    /** Removes this property's {@code @Setter} accessor (C8). No-op if absent. */
    public void removeSetter() throws IOException {
        if (ctSetter == null) {
            return;
        }
        modelEntity.getCtType().removeMethod(ctSetter);
        modelEntity.getCompilationUnit().regenerateFromAST();
    }

    /**
     * Adds {@code @Adder}/{@code @Remover} accessors to a LIST property that lacks
     * them (C8). Generates {@code void addToXxx(T)} / {@code void removeFromXxx(T)}.
     * Only the missing ones are generated.
     */
    public void addAdderRemover() throws IOException {
        if (cardinality != Cardinality.LIST) {
            throw new IllegalStateException("addAdderRemover applies to LIST properties only");
        }
        Factory factory = ctGetter.getFactory();
        CtTypeReference<?> elementType = ctGetter.getType().getActualTypeArguments().isEmpty()
                ? factory.Type().OBJECT
                : ctGetter.getType().getActualTypeArguments().get(0);
        String cap = capitalise(propertyIdentifier);
        boolean changed = false;
        if (ctAdder == null) {
            modelEntity.addGeneratedAccessor(
                    buildVoidAccessor(factory, "addTo" + cap, elementType, Adder.class), propertyIdentifier);
            changed = true;
        }
        if (ctRemover == null) {
            modelEntity.addGeneratedAccessor(
                    buildVoidAccessor(factory, "removeFrom" + cap, elementType, Remover.class), propertyIdentifier);
            changed = true;
        }
        if (changed) {
            modelEntity.getCompilationUnit().regenerateFromAST();
        }
    }

    /** Removes this property's {@code @Adder}/{@code @Remover} accessors (C8). */
    public void removeAdderRemover() throws IOException {
        CtType<?> ct = modelEntity.getCtType();
        boolean changed = false;
        if (ctAdder != null) {
            ct.removeMethod(ctAdder);
            changed = true;
        }
        if (ctRemover != null) {
            ct.removeMethod(ctRemover);
            changed = true;
        }
        if (changed) {
            modelEntity.getCompilationUnit().regenerateFromAST();
        }
    }

    // -------------------------------------------------------------------------
    // Attach an EXISTING method as a PAMELA accessor (the "complete" promote path,
    // model-editing-design.md §3.3). Unlike addSetter/addAdderRemover (which generate
    // a fresh method), these annotate a developer-written method already on the
    // interface. Targeted text edit (insert annotation line + import), like
    // SourceModelEntity.promoteMethodToProperty — minimal-diff and immune to the Sniper
    // annotation-insertion defect (spoon-type-system-analysis.md §10). The accessor link
    // materialises on the next meta-model rebuild (the caller triggers it).
    // -------------------------------------------------------------------------

    /** Annotates an existing {@code method(T)} as this SINGLE property's {@code @Setter}. */
    public void attachSetter(String methodName) throws IOException {
        attachAccessor(methodName, 1, Setter.class);
    }

    /** Annotates an existing {@code method(T)} as this SINGLE property's {@code @Updater}. */
    public void attachUpdater(String methodName) throws IOException {
        attachAccessor(methodName, 1, Updater.class);
    }

    /** Annotates an existing {@code method(T)} as this LIST property's {@code @Adder}. */
    public void attachAdder(String methodName) throws IOException {
        attachAccessor(methodName, 1, Adder.class);
    }

    /** Annotates an existing {@code method(T)} as this LIST property's {@code @Remover}. */
    public void attachRemover(String methodName) throws IOException {
        attachAccessor(methodName, 1, Remover.class);
    }

    /** Annotates an existing {@code method(T, int)} as this LIST property's {@code @Reindexer}. */
    public void attachReindexer(String methodName) throws IOException {
        attachAccessor(methodName, 2, Reindexer.class);
    }

    /**
     * Inserts {@code @<Accessor>(value = "<propertyIdentifier>")} on the existing method
     * {@code methodName} with {@code paramCount} parameters, and ensures the annotation import.
     */
    private void attachAccessor(String methodName, int paramCount,
            Class<? extends Annotation> annotationType) throws IOException {
        CtType<?> ctType = modelEntity.getCtType();
        CtMethod<?> method = null;
        for (CtMethod<?> m : ctType.getMethods()) {
            if (m.getSimpleName().equals(methodName) && m.getParameters().size() == paramCount) {
                method = m;
                break;
            }
        }
        if (method == null || method.getPosition() == null
                || !method.getPosition().isValidPosition()) {
            throw new IllegalStateException("No source position for method " + methodName
                    + "(" + paramCount + " param(s)) on " + modelEntity.getQualifiedName());
        }

        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String source = cu.getText();
        source = SourceAnnotationEditor.insertAnnotationLine(source,
                method.getPosition().getSourceStart(),
                "@" + annotationType.getSimpleName() + "(value = " + identifierValueText() + ")");
        source = SourceAnnotationEditor.ensureImport(source, annotationType.getName());
        cu.setText(source);
    }

    /**
     * The annotation {@code value} <em>text</em> for an accessor attached to this existing property,
     * honouring its identifier style: in {@code CONSTANT} mode it reproduces the getter's own
     * {@code @Getter.value} reference verbatim (preserving local vs qualified, e.g.
     * {@code Edge.START_NODE}); otherwise a string literal. Text counterpart of
     * {@link #identifierValueExpr(Factory)}, used by the targeted-text-edit paths.
     */
    private String identifierValueText() {
        if (identifierStyle == PropertyIdentifierStyle.CONSTANT) {
            CtAnnotation<?> rawGetter =
                    ctGetter.getAnnotation(ctGetter.getFactory().Type().createReference(Getter.class));
            if (rawGetter != null
                    && rawGetter.getValue("value") instanceof spoon.reflect.code.CtFieldRead<?>) {
                return rawGetter.getValue("value").toString();
            }
        }
        return "\"" + propertyIdentifier + "\"";
    }

    // =========================================================================
    // Accessor roles — checkbox + JavaMethodSelector inspector support.
    //
    // Each secondary accessor (setter/updater for SINGLE; adder/remover/reindexer
    // for LIST) is exposed to the inspector as a triple:
    //   - <role>Present  : boolean checkbox (declared or not)
    //   - <role>Method   : the CtMethod playing that role (the selector value)
    //   - <role>Candidates : the signature-compatible methods (selector restriction)
    //
    // Semantics (model-editing-design.md §6 — the model mutates the source + fires
    // an event; the application reacts by rebuilding):
    //   - check    : smart attach-or-generate — annotate an existing conventional
    //                method if one exists, else generate a fresh stub;
    //   - uncheck  : detach (remove the PAMELA annotation only — never delete the
    //                method, so a hand-written method is preserved and a re-check
    //                re-attaches it);
    //   - select   : retarget — move the annotation from the current method to the
    //                chosen one (signature-compatible candidates only).
    //
    // Surfacing CtMethod in the public API is the documented Spoon-encapsulation
    // exception (the selector inherently manipulates Spoon nodes — see
    // SourceCompilationUnit.getRootTypes(), ui-design.md §19.3).
    // =========================================================================

    private enum AccessorRole { SETTER, UPDATER, ADDER, REMOVER, REINDEXER }

    /** The mandatory getter method (read-only — a property always has exactly one getter). */
    public CtMethod<?> getGetterMethod() {
        return ctGetter;
    }

    // --- Setter (SINGLE) -----------------------------------------------------
    public boolean isSetterPresent() { return ctSetter != null; }
    public void setSetterPresent(boolean present) throws IOException { setAccessorPresent(AccessorRole.SETTER, present); }
    public CtMethod<?> getSetterMethod() { return ctSetter; }
    public void setSetterMethod(CtMethod<?> method) throws IOException { setAccessorMethod(AccessorRole.SETTER, method); }
    public List<CtMethod<?>> getSetterCandidates() { return candidateMethods(AccessorRole.SETTER); }

    // --- Updater (SINGLE) ----------------------------------------------------
    public boolean isUpdaterPresent() { return ctUpdater != null; }
    public void setUpdaterPresent(boolean present) throws IOException { setAccessorPresent(AccessorRole.UPDATER, present); }
    public CtMethod<?> getUpdaterMethod() { return ctUpdater; }
    public void setUpdaterMethod(CtMethod<?> method) throws IOException { setAccessorMethod(AccessorRole.UPDATER, method); }
    public List<CtMethod<?>> getUpdaterCandidates() { return candidateMethods(AccessorRole.UPDATER); }

    // --- Adder (LIST) --------------------------------------------------------
    public boolean isAdderPresent() { return ctAdder != null; }
    public void setAdderPresent(boolean present) throws IOException { setAccessorPresent(AccessorRole.ADDER, present); }
    public CtMethod<?> getAdderMethod() { return ctAdder; }
    public void setAdderMethod(CtMethod<?> method) throws IOException { setAccessorMethod(AccessorRole.ADDER, method); }
    public List<CtMethod<?>> getAdderCandidates() { return candidateMethods(AccessorRole.ADDER); }

    // --- Remover (LIST) ------------------------------------------------------
    public boolean isRemoverPresent() { return ctRemover != null; }
    public void setRemoverPresent(boolean present) throws IOException { setAccessorPresent(AccessorRole.REMOVER, present); }
    public CtMethod<?> getRemoverMethod() { return ctRemover; }
    public void setRemoverMethod(CtMethod<?> method) throws IOException { setAccessorMethod(AccessorRole.REMOVER, method); }
    public List<CtMethod<?>> getRemoverCandidates() { return candidateMethods(AccessorRole.REMOVER); }

    // --- Reindexer (LIST) ----------------------------------------------------
    public boolean isReindexerPresent() { return ctReindexer != null; }
    public void setReindexerPresent(boolean present) throws IOException { setAccessorPresent(AccessorRole.REINDEXER, present); }
    public CtMethod<?> getReindexerMethod() { return ctReindexer; }
    public void setReindexerMethod(CtMethod<?> method) throws IOException { setAccessorMethod(AccessorRole.REINDEXER, method); }
    public List<CtMethod<?>> getReindexerCandidates() { return candidateMethods(AccessorRole.REINDEXER); }

    // --- generic checkbox / selector mutations -------------------------------

    /** Checkbox toggle: smart attach-or-generate when checked, detach when unchecked. */
    private void setAccessorPresent(AccessorRole role, boolean present) throws IOException {
        boolean current = currentAccessor(role) != null;
        if (present == current) {
            return;
        }
        if (present) {
            CtMethod<?> conventional = findConventional(role);
            if (conventional != null) {
                attachExisting(role, conventional);
            } else {
                generate(role);
            }
        } else {
            detach(role);
        }
        pcSupport.firePropertyChange("accessor:" + role, !present, present);
    }

    /** Selector commit: retarget the accessor annotation onto the chosen method (null = detach). */
    private void setAccessorMethod(AccessorRole role, CtMethod<?> chosen) throws IOException {
        CtMethod<?> current = currentAccessor(role);
        if (chosen == current) {
            return;
        }
        if (chosen == null) {
            if (current != null) {
                detach(role);
                pcSupport.firePropertyChange("accessor:" + role, true, false);
            }
            return;
        }
        retarget(role, current, chosen);
        pcSupport.firePropertyChange("accessor:" + role, null, chosen);
    }

    // --- role metadata -------------------------------------------------------

    private CtMethod<?> currentAccessor(AccessorRole role) {
        switch (role) {
            case SETTER:    return ctSetter;
            case UPDATER:   return ctUpdater;
            case ADDER:     return ctAdder;
            case REMOVER:   return ctRemover;
            case REINDEXER: return ctReindexer;
            default:        return null;
        }
    }

    private void setCurrentAccessor(AccessorRole role, CtMethod<?> method) {
        String name = (method == null) ? null : method.getSimpleName();
        switch (role) {
            case SETTER:    ctSetter = method;    setterMethodName = name;    break;
            case UPDATER:   ctUpdater = method;   updaterMethodName = name;   break;
            case ADDER:     ctAdder = method;     adderMethodName = name;     break;
            case REMOVER:   ctRemover = method;   removerMethodName = name;   break;
            case REINDEXER: ctReindexer = method; reindexerMethodName = name; break;
        }
    }

    private Class<? extends Annotation> annotationFor(AccessorRole role) {
        switch (role) {
            case SETTER:    return Setter.class;
            case UPDATER:   return Updater.class;
            case ADDER:     return Adder.class;
            case REMOVER:   return Remover.class;
            case REINDEXER: return Reindexer.class;
            default:        throw new IllegalArgumentException("Unknown role " + role);
        }
    }

    /** Conventional method name used when generating a missing accessor. */
    private String conventionalName(AccessorRole role) {
        String cap = capitalise(propertyIdentifier);
        switch (role) {
            case SETTER:    return "set" + cap;
            case UPDATER:   return "update" + cap;
            case ADDER:     return "addTo" + cap;
            case REMOVER:   return "removeFrom" + cap;
            case REINDEXER: return "reindex" + cap;
            default:        throw new IllegalArgumentException("Unknown role " + role);
        }
    }

    private int paramCountFor(AccessorRole role) {
        return role == AccessorRole.REINDEXER ? 2 : 1;
    }

    /** The value type of the role's first parameter: getter type (SINGLE) or element type (LIST). */
    private CtTypeReference<?> valueTypeRef() {
        if (cardinality == Cardinality.LIST) {
            List<CtTypeReference<?>> args = ctGetter.getType().getActualTypeArguments();
            return (args == null || args.isEmpty())
                    ? ctGetter.getFactory().Type().OBJECT
                    : args.get(0);
        }
        return ctGetter.getType();
    }

    /** Signature-compatible methods of the entity interface for the given role. */
    private List<CtMethod<?>> candidateMethods(AccessorRole role) {
        List<CtMethod<?>> result = new ArrayList<>();
        String valueQN = valueTypeRef().getQualifiedName();
        int paramCount = paramCountFor(role);
        for (CtMethod<?> m : modelEntity.getCtType().getMethods()) {
            if (m.getParameters().size() != paramCount) {
                continue;
            }
            if (!paramTypeMatches(m.getParameters().get(0), valueQN)) {
                continue;
            }
            if (role == AccessorRole.REINDEXER && !isIntParam(m.getParameters().get(1))) {
                continue;
            }
            result.add(m);
        }
        CtMethod<?> current = currentAccessor(role);
        if (current != null && !result.contains(current)) {
            result.add(0, current);
        }
        result.sort(java.util.Comparator.comparing(CtMethod::getSimpleName));
        return result;
    }

    /** A signature-compatible candidate whose name matches the convention, or {@code null}. */
    private CtMethod<?> findConventional(AccessorRole role) {
        String name = conventionalName(role);
        for (CtMethod<?> m : candidateMethods(role)) {
            if (name.equals(m.getSimpleName())) {
                return m;
            }
        }
        return null;
    }

    // --- source mutations ----------------------------------------------------

    /** Annotates an existing method as this property's accessor (annotation line + import). */
    private void attachExisting(AccessorRole role, CtMethod<?> method) throws IOException {
        requirePosition(method);
        Class<? extends Annotation> annotation = annotationFor(role);
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String source = cu.getText();
        source = SourceAnnotationEditor.insertAnnotationLine(source,
                method.getPosition().getSourceStart(), annotationLine(annotation));
        source = SourceAnnotationEditor.ensureImport(source, annotation.getName());
        cu.setText(source);
        setCurrentAccessor(role, method);
    }

    /** Generates a fresh conventional accessor stub and adds it to the entity interface. */
    private void generate(AccessorRole role) throws IOException {
        Factory factory = ctGetter.getFactory();
        CtMethod<Void> method = (role == AccessorRole.REINDEXER)
                ? buildReindexer(factory, conventionalName(role), valueTypeRef())
                : buildVoidAccessor(factory, conventionalName(role), valueTypeRef(), annotationFor(role));
        modelEntity.addGeneratedAccessor(method, propertyIdentifier);
        modelEntity.getCompilationUnit().regenerateFromAST();
        setCurrentAccessor(role, method);
    }

    /** Removes the PAMELA annotation from the current accessor method (keeps the method). */
    private void detach(AccessorRole role) throws IOException {
        CtMethod<?> current = currentAccessor(role);
        if (current == null) {
            return;
        }
        requirePosition(current);
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String source = SourceAnnotationEditor.removeAnnotation(cu.getText(),
                current.getPosition().getSourceStart(), annotationFor(role).getSimpleName());
        cu.setText(source);
        setCurrentAccessor(role, null);
    }

    /**
     * Moves the accessor annotation from {@code current} (if any) to {@code chosen}. Both edits are
     * in the entity's own compilation unit; the higher source offset is edited first so the lower
     * one stays valid.
     */
    private void retarget(AccessorRole role, CtMethod<?> current, CtMethod<?> chosen) throws IOException {
        requirePosition(chosen);
        Class<? extends Annotation> annotation = annotationFor(role);
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String source = cu.getText();
        int chosenOffset = chosen.getPosition().getSourceStart();
        Integer currentOffset = (current != null && current.getPosition() != null
                && current.getPosition().isValidPosition())
                ? current.getPosition().getSourceStart() : null;
        if (currentOffset != null && currentOffset > chosenOffset) {
            source = SourceAnnotationEditor.removeAnnotation(source, currentOffset, annotation.getSimpleName());
            source = SourceAnnotationEditor.insertAnnotationLine(source, chosenOffset, annotationLine(annotation));
        } else {
            source = SourceAnnotationEditor.insertAnnotationLine(source, chosenOffset, annotationLine(annotation));
            if (currentOffset != null) {
                source = SourceAnnotationEditor.removeAnnotation(source, currentOffset, annotation.getSimpleName());
            }
        }
        source = SourceAnnotationEditor.ensureImport(source, annotation.getName());
        cu.setText(source);
        setCurrentAccessor(role, chosen);
    }

    private String annotationLine(Class<? extends Annotation> annotation) {
        return "@" + annotation.getSimpleName() + "(value = " + identifierValueText() + ")";
    }

    private static void requirePosition(CtMethod<?> method) {
        if (method.getPosition() == null || !method.getPosition().isValidPosition()) {
            throw new IllegalStateException("No source position for method " + method.getSimpleName());
        }
    }

    private static boolean paramTypeMatches(CtParameter<?> param, String qualifiedName) {
        return param.getType() != null && qualifiedName.equals(param.getType().getQualifiedName());
    }

    private static boolean isIntParam(CtParameter<?> param) {
        String qn = (param.getType() == null) ? null : param.getType().getQualifiedName();
        return "int".equals(qn) || "java.lang.Integer".equals(qn);
    }

    /** Builds {@code public void name(T value, int index)} annotated with {@code @Reindexer}. */
    private CtMethod<Void> buildReindexer(Factory factory, String name, CtTypeReference<?> elementType) {
        CtMethod<Void> method = factory.Core().createMethod();
        method.setSimpleName(name);
        method.setType((CtTypeReference<Void>) factory.Type().VOID_PRIMITIVE);
        method.addModifier(ModifierKind.PUBLIC);

        CtParameter<Object> value = factory.Core().createParameter();
        value.setSimpleName("value");
        value.setType((CtTypeReference<Object>) elementType.clone());
        method.addParameter(value);

        CtParameter<Object> index = factory.Core().createParameter();
        index.setSimpleName("index");
        index.setType((CtTypeReference) factory.Type().INTEGER_PRIMITIVE);
        method.addParameter(index);

        CtAnnotation<?> annotation = factory.Core().createAnnotation();
        annotation.setAnnotationType(factory.Type().createReference(Reindexer.class));
        annotation.addValue("value", identifierValueExpr(factory));
        method.addAnnotation(annotation);
        return method;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * The annotation {@code value} expression for a generated accessor of this property, honouring
     * the property's existing identifier style: in {@code CONSTANT} mode it <b>clones the getter's
     * own {@code @Getter.value} reference</b> (preserving local vs qualified, e.g.
     * {@code Edge.START_NODE}); otherwise a string literal. See
     * {@code property-identifier-constant-design.md §6}.
     */
    private spoon.reflect.code.CtExpression<?> identifierValueExpr(Factory factory) {
        if (identifierStyle == PropertyIdentifierStyle.CONSTANT) {
            CtAnnotation<?> rawGetter =
                    ctGetter.getAnnotation(factory.Type().createReference(Getter.class));
            if (rawGetter != null
                    && rawGetter.getValue("value") instanceof spoon.reflect.code.CtFieldRead<?>) {
                return rawGetter.getValue("value").clone();
            }
        }
        return factory.createLiteral(propertyIdentifier);
    }

    /** Builds a {@code public void name(paramType value)} method annotated with the PAMELA key. */
    private CtMethod<Void> buildVoidAccessor(Factory factory, String name,
            CtTypeReference<?> paramType, Class<? extends java.lang.annotation.Annotation> annotationType) {
        CtMethod<Void> method = factory.Core().createMethod();
        method.setSimpleName(name);
        method.setType((CtTypeReference<Void>) factory.Type().VOID_PRIMITIVE);
        method.addModifier(ModifierKind.PUBLIC);

        CtParameter<Object> param = factory.Core().createParameter();
        param.setSimpleName("value");
        param.setType((CtTypeReference<Object>) paramType.clone());
        method.addParameter(param);

        CtAnnotation<?> annotation = factory.Core().createAnnotation();
        annotation.setAnnotationType(factory.Type().createReference(annotationType));
        annotation.addValue("value", identifierValueExpr(factory));
        method.addAnnotation(annotation);
        return method;
    }

    private static void setSingleParamType(CtMethod<?> method, CtTypeReference<?> type) {
        if (method != null && !method.getParameters().isEmpty()) {
            CtParameter<?> p = method.getParameters().get(0);
            ((CtParameter<Object>) p).setType((CtTypeReference) type.clone());
        }
    }

    /** Replaces (or adds) a String-valued element of a PAMELA accessor annotation. */
    private static void setAnnotationValue(CtMethod<?> method,
            Class<? extends Annotation> annotationType, String element, String value,
            Factory factory) {
        CtAnnotation<? extends Annotation> ann =
                method.getAnnotation(factory.Type().createReference(annotationType));
        if (ann != null) {
            // getValues() is an unmodifiable view and addValue() turns a repeated
            // element into an array, so rebuild the element-value map with a fresh
            // string literal for the target element and re-set it.
            java.util.Map<String, spoon.reflect.code.CtExpression> values =
                    new java.util.HashMap<>(ann.getValues());
            values.put(element, factory.createLiteral(value));
            ann.setValues(values);
        }
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void addIssue(Issue issue) {
        issues.add(issue);
        modelEntity.getMetaModel().fireIssue(issue);
    }

    /**
     * Returns {@code true} if the given type reference is a {@code java.util.List}
     * (or its fully qualified equivalent).
     */
    private static boolean isListType(CtTypeReference<?> typeRef) {
        String qn = typeRef.getQualifiedName();
        return "java.util.List".equals(qn) || "List".equals(qn);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** The string identifier of this property, e.g. {@code "name"}. */
    public String getPropertyIdentifier() {
        return propertyIdentifier;
    }

    /** The entity where this property is declared. */
    public SourceModelEntity getModelEntity() {
        return modelEntity;
    }

    @Override
    public SourceMetaModel getMetaModel() {
        return modelEntity.getMetaModel();
    }

    /** {@code SINGLE} or {@code LIST}. */
    public Cardinality getCardinality() {
        return cardinality;
    }

    /**
     * How this property's identifier is written in source: {@link PropertyIdentifierStyle#CONSTANT}
     * (a {@code static final String} reference) or {@link PropertyIdentifierStyle#LITERAL}.
     */
    public PropertyIdentifierStyle getIdentifierStyle() {
        return identifierStyle;
    }

    /**
     * The simple name of the {@code static final String} constant referenced by this property's
     * {@code @Getter.value} (e.g. {@code "OUTGOING_EDGES"}), or {@code null} when the identifier is
     * a string literal.
     */
    public String getReferencedConstantName() {
        return referencedConstantName;
    }

    /** {@code true} for a SINGLE property (drives setter/updater inspector rows). */
    public boolean isSingleCardinality() {
        return cardinality == Cardinality.SINGLE;
    }

    /** {@code true} for a LIST property (drives adder/remover/reindexer inspector rows). */
    public boolean isListCardinality() {
        return cardinality == Cardinality.LIST;
    }

    /**
     * The element type of this property.  For a SINGLE property this is
     * the return type of the getter.  For a LIST property this is the
     * element type parameter of {@code List<T>}.
     */
    public SourceType getType() {
        return type;
    }

    /** Simple name of the getter method, e.g. {@code "getName"}. */
    public String getGetterMethodName() {
        return getterMethodName;
    }

    /** Simple name of the setter method, or {@code null} if absent. */
    public String getSetterMethodName() {
        return setterMethodName;
    }

    /** Simple name of the adder method, or {@code null} if absent. */
    public String getAdderMethodName() {
        return adderMethodName;
    }

    /** Simple name of the remover method, or {@code null} if absent. */
    public String getRemoverMethodName() {
        return removerMethodName;
    }

    /** Simple name of the reindexer method, or {@code null} if absent. */
    public String getReindexerMethodName() {
        return reindexerMethodName;
    }

    /** Simple name of the updater method, or {@code null} if absent. */
    public String getUpdaterMethodName() {
        return updaterMethodName;
    }

    /**
     * The default value string from {@code @Getter.defaultValue()},
     * or {@code ""} ({@link Getter#UNDEFINED}) if not set.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /** {@code true} if the property is derived ({@code @Getter.isDerived = true}). */
    public boolean isDerived() {
        return derived;
    }

    /**
     * {@code true} if PAMELA should ignore the property type (the type is
     * external to the model, as declared by {@code @Getter.ignoreType = true}).
     */
    public boolean isIgnoreType() {
        return ignoreType;
    }

    /** {@code true} if the property is excluded from equality checks. */
    public boolean isIgnoreForEquality() {
        return ignoreForEquality;
    }

    /**
     * {@code true} if the same object may appear multiple times in the
     * collection (LIST only).
     */
    public boolean isAllowsMultipleOccurrences() {
        return allowsMultipleOccurrences;
    }

    /**
     * {@code true} if the property's type can be converted to/from a {@code String}
     * (a primitive, an enum, or a JDK type PAMELA has a converter for) — i.e. whether
     * a {@code defaultValue} is meaningful for this property. Delegates to
     * {@link SourceType#isStringConvertible()}; it is <em>not</em> the raw
     * {@code @Getter.isStringConvertable()} annotation flag (a developer-declared hint
     * that defaults to {@code false} and does not reflect the actual type).
     */
    public boolean isStringConvertable() {
        return type.isStringConvertible();
    }

    /**
     * The raw inverse-property identifier from {@code @Getter.inverse()},
     * or {@code ""} ({@link Getter#UNDEFINED}) if not set.
     */
    public String getInversePropertyIdentifier() {
        return inversePropertyIdentifier;
    }

    /**
     * The resolved inverse property, or {@code null} if there is no inverse
     * or if Phase 3 has not yet run.
     */
    public SourceModelProperty getInverseProperty() {
        return inverseProperty;
    }

    /** {@code true} if the {@code @Embedded} annotation is present on the getter. */
    public boolean isEmbedded() {
        return embedded;
    }

    /** Closure conditions from {@code @Embedded.closureConditions()}. */
    public List<String> getEmbeddedClosureConditions() {
        return embeddedClosureConditions;
    }

    /** Deletion conditions from {@code @Embedded.deletionConditions()}. */
    public List<String> getEmbeddedDeletionConditions() {
        return embeddedDeletionConditions;
    }

    /**
     * The XML attribute name from {@code @XMLAttribute}, or {@code null} if
     * the annotation is absent.
     */
    public String getXmlAttributeName() {
        return xmlAttributeName;
    }

    /**
     * The XML element tag from {@code @XMLElement} on the getter method,
     * or {@code null} if absent or empty.
     */
    public String getXmlElementTag() {
        return xmlElementTag;
    }

    /**
     * {@code true} if {@code @XMLElement.primary()} is set on the getter method.
     */
    public boolean isPrimaryXmlElement() {
        return primaryXmlElement;
    }

    // -------------------------------------------------------------------------
    // XML serialization — editable inspector (xml-serialization-design.md §4.2)
    // -------------------------------------------------------------------------

    /** The current XML serialization mode of this property (NONE / ATTRIBUTE / ELEMENT). */
    public XmlSerializationMode getXmlSerialization() {
        if (xmlAttributePresent) {
            return XmlSerializationMode.ATTRIBUTE;
        }
        if (xmlElementPresent) {
            return XmlSerializationMode.ELEMENT;
        }
        return XmlSerializationMode.NONE;
    }

    /**
     * The modes offered in the inspector {@code <DropDown>}: {@code ATTRIBUTE} is only
     * available for a string-convertible property (an entity/collection reference can only be
     * an XML element), so a non-convertible property offers just {@code NONE} / {@code ELEMENT}.
     */
    public XmlSerializationMode[] getXmlSerializationChoices() {
        if (isStringConvertable()) {
            return new XmlSerializationMode[] {
                    XmlSerializationMode.NONE, XmlSerializationMode.ATTRIBUTE, XmlSerializationMode.ELEMENT };
        }
        return new XmlSerializationMode[] { XmlSerializationMode.NONE, XmlSerializationMode.ELEMENT };
    }

    /**
     * Switches this property's XML serialization mode (mutually exclusive
     * {@code @XMLAttribute} / {@code @XMLElement}). Removes whichever annotation is not
     * wanted and adds the wanted one, on the getter position. Editable-inspector pattern:
     * mutate the buffer, update fields, fire {@code "xmlSerialization"} — no rebuild.
     */
    public void setXmlSerialization(XmlSerializationMode mode) throws IOException {
        XmlSerializationMode old = getXmlSerialization();
        if (mode == old) {
            return;
        }
        requireGetterPosition();
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String source = cu.getText();
        int start = ctGetter.getPosition().getSourceStart();

        boolean wantAttribute = mode == XmlSerializationMode.ATTRIBUTE;
        boolean wantElement = mode == XmlSerializationMode.ELEMENT;

        if (xmlAttributePresent && !wantAttribute) {
            source = SourceAnnotationEditor.removeAnnotation(source, start, "XMLAttribute");
        }
        if (xmlElementPresent && !wantElement) {
            source = SourceAnnotationEditor.removeAnnotation(source, start, "XMLElement");
        }
        if (wantAttribute && !xmlAttributePresent) {
            source = SourceAnnotationEditor.addAnnotation(source, start, "XMLAttribute", XMLAttribute.class.getName());
        }
        if (wantElement && !xmlElementPresent) {
            source = SourceAnnotationEditor.addAnnotation(source, start, "XMLElement", XMLElement.class.getName());
        }
        cu.setText(source);

        // Update in-memory state; a mode switch drops the old annotation's tag/primary.
        xmlAttributePresent = wantAttribute;
        xmlElementPresent = wantElement;
        if (!wantAttribute) {
            xmlAttributeTag = null;
            xmlAttributeName = null;
        } else {
            xmlAttributeName = (xmlAttributeTag == null) ? propertyIdentifier : xmlAttributeTag;
        }
        if (!wantElement) {
            xmlElementTag = null;
            primaryXmlElement = false;
            xmlElementContext = null;
        }
        pcSupport.firePropertyChange("xmlSerialization", old, mode);
    }

    /**
     * The XML tag of the currently-active annotation ({@code @XMLAttribute} or
     * {@code @XMLElement}), or empty if none/derived. Bound editable in the inspector.
     */
    public String getXmlTag() {
        if (xmlAttributePresent) {
            return xmlAttributeTag == null ? "" : xmlAttributeTag;
        }
        if (xmlElementPresent) {
            return xmlElementTag == null ? "" : xmlElementTag;
        }
        return "";
    }

    /**
     * Sets (or clears with an empty value) {@code xmlTag} on the active annotation. No-op
     * when the mode is NONE. An empty tag lets PAMELA derive it from the property name (Q4).
     */
    public void setXmlTag(String tag) throws IOException {
        XmlSerializationMode mode = getXmlSerialization();
        if (mode == XmlSerializationMode.NONE) {
            return;
        }
        String normalized = (tag == null || tag.isEmpty()) ? null : tag;
        String annotation = mode == XmlSerializationMode.ATTRIBUTE ? "XMLAttribute" : "XMLElement";
        String current = mode == XmlSerializationMode.ATTRIBUTE ? xmlAttributeTag : xmlElementTag;
        if (java.util.Objects.equals(normalized, current)) {
            return;
        }
        requireGetterPosition();
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String edited = SourceAnnotationEditor.setAnnotationParameter(
                cu.getText(), ctGetter.getPosition().getSourceStart(), annotation, "xmlTag",
                normalized == null ? null : "\"" + normalized + "\"");
        cu.setText(edited);
        String old = getXmlTag();
        if (mode == XmlSerializationMode.ATTRIBUTE) {
            xmlAttributeTag = normalized;
            xmlAttributeName = (normalized == null) ? propertyIdentifier : normalized;
        } else {
            xmlElementTag = normalized;
        }
        pcSupport.firePropertyChange("xmlTag", old, getXmlTag());
    }

    /** {@code true} when this property is XML-serialized (ATTRIBUTE or ELEMENT) — for {@code enable} bindings. */
    public boolean isXmlSerialized() {
        return getXmlSerialization() != XmlSerializationMode.NONE;
    }

    /** {@code true} when this property is serialized as an XML element — for {@code enable} bindings. */
    public boolean isXmlElementMode() {
        return getXmlSerialization() == XmlSerializationMode.ELEMENT;
    }

    /** {@code @XMLElement.primary()} editable flag (ELEMENT mode only). */
    public boolean isXmlPrimary() {
        return primaryXmlElement;
    }

    /** Sets/clears {@code @XMLElement(primary=…)}. No-op unless the mode is ELEMENT. */
    public void setXmlPrimary(boolean primary) throws IOException {
        if (!xmlElementPresent || primary == primaryXmlElement) {
            return;
        }
        requireGetterPosition();
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String edited = SourceAnnotationEditor.setAnnotationParameter(
                cu.getText(), ctGetter.getPosition().getSourceStart(), "XMLElement", "primary",
                primary ? "true" : null);
        cu.setText(edited);
        boolean old = primaryXmlElement;
        primaryXmlElement = primary;
        pcSupport.firePropertyChange("xmlPrimary", old, primary);
    }

    /** The {@code @XMLElement(context=…)} value (ELEMENT mode only), or empty otherwise. */
    public String getXmlContext() {
        return (xmlElementPresent && xmlElementContext != null) ? xmlElementContext : "";
    }

    /** Sets/clears {@code @XMLElement(context=…)}. No-op unless the mode is ELEMENT. */
    public void setXmlContext(String context) throws IOException {
        if (!xmlElementPresent) {
            return;
        }
        String normalized = (context == null || context.isEmpty()) ? null : context;
        if (java.util.Objects.equals(normalized, xmlElementContext)) {
            return;
        }
        requireGetterPosition();
        SourceCompilationUnit cu = modelEntity.getCompilationUnit();
        String edited = SourceAnnotationEditor.setAnnotationParameter(
                cu.getText(), ctGetter.getPosition().getSourceStart(), "XMLElement", "context",
                normalized == null ? null : "\"" + normalized + "\"");
        cu.setText(edited);
        String old = xmlElementContext;
        xmlElementContext = normalized;
        pcSupport.firePropertyChange("xmlContext", old, normalized);
    }

    /**
     * The cloning strategy type from {@code @CloningStrategy}, or {@code null}
     * if the annotation is absent.
     */
    public StrategyType getCloningStrategyType() {
        return cloningStrategyType;
    }

    /**
     * Diagnostics detected on this property during construction and validation.
     */
    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    @Override
    public java.beans.PropertyChangeSupport getPropertyChangeSupport() {
        return pcSupport;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }

    @Override
    public String toString() {
        return "SourceModelProperty(" + propertyIdentifier + " : " + cardinality + " " + type + ")";
    }
}
