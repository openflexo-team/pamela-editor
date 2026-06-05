package org.openflexo.pamela.editor.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openflexo.pamela.annotations.CloningStrategy;
import org.openflexo.pamela.annotations.CloningStrategy.StrategyType;
import org.openflexo.pamela.annotations.Embedded;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.ReturnedValue;
import org.openflexo.pamela.annotations.XMLAttribute;
import org.openflexo.pamela.annotations.XMLElement;

import spoon.reflect.declaration.CtMethod;
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
public class SourceModelProperty implements SourceElement {

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

    // @Getter parameters
    private final String defaultValue;
    private final boolean derived;
    private final boolean ignoreType;
    private final boolean ignoreForEquality;
    private final boolean allowsMultipleOccurrences;
    private final boolean stringConvertable;

    // Relationships
    private final String inversePropertyIdentifier;
    private SourceModelProperty inverseProperty; // resolved during Phase 3

    // Embedding (@Embedded)
    private final boolean embedded;
    private final List<String> embeddedClosureConditions;
    private final List<String> embeddedDeletionConditions;

    // Serialization
    private final String xmlAttributeName;     // from @XMLAttribute, null if absent
    private final String xmlElementTag;        // from @XMLElement on getter, null if absent
    private final boolean primaryXmlElement;   // from @XMLElement.primary()

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
        this.stringConvertable = getterAnnotation.isStringConvertable();
        this.inversePropertyIdentifier = getterAnnotation.inverse();

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
                        + "' has a raw List return type — no element type argument found"));
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
        this.xmlAttributeName = (xmlAttr != null)
                ? (xmlAttr.xmlTag().isEmpty() ? propertyIdentifier : xmlAttr.xmlTag())
                : null;

        // @XMLElement on getter
        XMLElement xmlElem = ctGetter.getAnnotation(XMLElement.class);
        if (xmlElem != null) {
            this.xmlElementTag = xmlElem.xmlTag().isEmpty() ? null : xmlElem.xmlTag();
            this.primaryXmlElement = xmlElem.primary();
        } else {
            this.xmlElementTag = null;
            this.primaryXmlElement = false;
        }

        // @CloningStrategy
        CloningStrategy cloningStrategyAnnotation = ctGetter.getAnnotation(CloningStrategy.class);
        this.cloningStrategyType = (cloningStrategyAnnotation != null)
                ? cloningStrategyAnnotation.value()
                : null;

        // Validate isDerived without @ReturnedValue
        if (derived && ctGetter.getAnnotation(ReturnedValue.class) == null) {
            addIssue(new Warning("Property '" + propertyIdentifier
                    + "' is declared isDerived=true but has no @ReturnedValue annotation"));
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
                    + "' is not allowed on LIST property '" + propertyIdentifier + "'"));
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
                    + "' is not allowed on SINGLE property '" + propertyIdentifier + "'"));
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
                    + "' is not allowed on SINGLE property '" + propertyIdentifier + "'"));
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
                        + "' has an @Adder but no @Remover"));
            }
            if (ctAdder == null && ctRemover != null) {
                addIssue(new Warning("Property '" + propertyIdentifier
                        + "' has a @Remover but no @Adder"));
            }
        }
    }

    /**
     * Called during Phase 3 to establish the resolved inverse-property link.
     *
     * @param inverse the resolved inverse property (must not be {@code null})
     */
    void setInverseProperty(SourceModelProperty inverse) {
        this.inverseProperty = inverse;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    /** {@code SINGLE} or {@code LIST}. */
    public Cardinality getCardinality() {
        return cardinality;
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

    /** {@code true} if the value type can be converted to/from a String. */
    public boolean isStringConvertable() {
        return stringConvertable;
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
    public String toString() {
        return "SourceModelProperty(" + propertyIdentifier + " : " + cardinality + " " + type + ")";
    }
}
