package org.openflexo.pamela.editor.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Finder;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.Initializer;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.ModelEntity.InitPolicy;
import org.openflexo.pamela.annotations.Operation;
import org.openflexo.pamela.annotations.Parameter;
import org.openflexo.pamela.annotations.Reindexer;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.Updater;
import org.openflexo.pamela.annotations.XMLElement;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Corresponds to PAMELA's {@code ModelEntity<I>}.
 * Represents one {@code @ModelEntity}-annotated type (expected to be an
 * interface; a {@link Error} is fired if it is a class).
 *
 * <p>This class stores only the properties <em>declared directly</em> on this
 * type.  Inherited properties are accessed via {@link #getAllProperties()},
 * which walks {@link #getDirectSuperEntities()} recursively.</p>
 *
 * <p>Validation rules (fired during construction / Phase 2):
 * <ul>
 *   <li>{@code @ModelEntity} on a class → {@link Error}</li>
 *   <li>Non-abstract entity with no {@code @Initializer} and
 *       {@code initPolicy = REQUIRED} → {@link Warning}</li>
 * </ul>
 * </p>
 */
public class SourceModelEntity implements SourceElement,
        org.openflexo.toolbox.HasPropertyChangeSupport {

    // Internal Spoon reference — never exposed in the public API
    private final CtType<?> ctType;

    private final java.beans.PropertyChangeSupport pcSupport =
            new java.beans.PropertyChangeSupport(this);

    // Identity — non-final to allow rename
    private String qualifiedName;
    private String simpleName;
    private SourcePackage sourcePackage;
    private final SourceMetaModel metaModel;

    // @ModelEntity annotation parameters
    private boolean abstractEntity;
    private final InitPolicy initPolicy;
    private final boolean inheritInitializers;

    // Source file
    private SourceCompilationUnit compilationUnit;

    // Implementation class (set during Phase 2; null if @ImplementationClass is absent)
    private SourceImplementationClass implementationClass;

    // Properties declared on this type only
    private final Map<String, SourceModelProperty> declaredProperties;

    // Aggregate identifier style (literal vs constant), computed from the declared properties at
    // the end of Phase 2. Derived data, never serialized. See property-identifier-constant-design.md.
    private PropertyIdentifierStyle identifierStyle = PropertyIdentifierStyle.UNDETERMINED;

    // Hierarchy — direct parents only (no transitive closure stored)
    private final List<SourceModelEntity> directSuperEntities;

    // Lifecycle
    private final List<SourceModelInitializer> initializers;

    // Operations — custom methods sourced from the interface (custom-method-design.md)
    private final List<SourceCustomMethod> declaredCustomMethods;

    // Serialization
    private final String xmlTag;

    // Diagnostics
    private final List<Issue> issues;

    /**
     * Constructs a {@code SourceModelEntity} from a Spoon type node.
     *
     * @param ctType    the Spoon type (interface or class annotated {@code @ModelEntity})
     * @param metaModel the owning meta-model
     */
    public SourceModelEntity(CtType<?> ctType, SourceMetaModel metaModel) {
        this.ctType = ctType;
        this.metaModel = metaModel;
        this.qualifiedName = ctType.getQualifiedName();
        this.simpleName = ctType.getSimpleName();
        this.declaredProperties = new LinkedHashMap<>();
        this.directSuperEntities = new ArrayList<>();
        this.initializers = new ArrayList<>();
        this.declaredCustomMethods = new ArrayList<>();
        this.issues = new ArrayList<>();

        // Read @ModelEntity annotation parameters
        ModelEntity annotation = ctType.getAnnotation(ModelEntity.class);
        this.abstractEntity = (annotation != null) && annotation.isAbstract();
        this.initPolicy = (annotation != null) ? annotation.initPolicy() : InitPolicy.REQUIRED;
        this.inheritInitializers = (annotation != null) && annotation.inheritInitializers();

        // Validation: @ModelEntity on a class
        if (!(ctType instanceof CtInterface)) {
            addIssue(new Error("@ModelEntity " + qualifiedName + " must be an interface, not a class", this));
        }

        // @XMLElement on the type itself
        XMLElement xmlElement = ctType.getAnnotation(XMLElement.class);
        this.xmlTag = (xmlElement != null && !xmlElement.xmlTag().isEmpty())
                ? xmlElement.xmlTag()
                : null;
    }

    // -------------------------------------------------------------------------
    // Internal setters called by SourceMetaModel during construction
    // -------------------------------------------------------------------------

    void setSourcePackage(SourcePackage pkg) {
        this.sourcePackage = pkg;
    }

    void setCompilationUnit(SourceCompilationUnit cu) {
        this.compilationUnit = cu;
    }

    void setImplementationClass(SourceImplementationClass impl) {
        this.implementationClass = impl;
    }

    void addDeclaredProperty(SourceModelProperty property) {
        declaredProperties.put(property.getPropertyIdentifier(), property);
    }

    void addDirectSuperEntity(SourceModelEntity superEntity) {
        if (!directSuperEntities.contains(superEntity)) {
            directSuperEntities.add(superEntity);
        }
    }

    void addInitializer(SourceModelInitializer initializer) {
        initializers.add(initializer);
    }

    void addCustomMethod(SourceCustomMethod method) {
        declaredCustomMethods.add(method);
    }

    private void addIssue(Issue issue) {
        issues.add(issue);
        metaModel.fireIssue(issue);
    }

    // -------------------------------------------------------------------------
    // Package-private helpers used by mutation operations
    // -------------------------------------------------------------------------

    /**
     * Returns the internal Spoon type.
     * Package-private — used by mutation operations and by {@link SourceModelProperty#remove()}.
     */
    CtType<?> getCtType() {
        return ctType;
    }

    /**
     * Removes a property from the declared properties map.
     * Called by {@link SourceModelProperty#remove()}.
     *
     * @param id the property identifier to remove
     */
    void removeDeclaredProperty(String id) {
        declaredProperties.remove(id);
    }

    /**
     * Removes a direct super-entity link.
     * Called by {@link #removeSuperEntity(SourceModelEntity)}.
     *
     * @param e the super-entity to remove
     */
    void removeDirectSuperEntity(SourceModelEntity e) {
        directSuperEntities.remove(e);
    }

    // -------------------------------------------------------------------------
    // Key methods
    // -------------------------------------------------------------------------

    /**
     * Returns all properties visible on this entity: those declared by ancestor
     * entities (parents-first, depth-first) merged with the properties declared
     * directly on this entity.  Child properties override parent properties with
     * the same identifier.
     *
     * @return an unmodifiable, insertion-ordered map keyed by property identifier
     */
    public Map<String, SourceModelProperty> getAllProperties() {
        Map<String, SourceModelProperty> result = new LinkedHashMap<>();
        // Parents first (depth-first)
        for (SourceModelEntity parent : directSuperEntities) {
            result.putAll(parent.getAllProperties());
        }
        // Own properties override parent properties
        result.putAll(declaredProperties);
        return Collections.unmodifiableMap(result);
    }

    // =========================================================================
    // Mutation operations
    // =========================================================================

    /**
     * Renames this entity to {@code newSimpleName}.
     *
     * <p>This operation:
     * <ol>
     *   <li>Updates all type references in the Spoon model to use the new name.</li>
     *   <li>Renames the Spoon type itself.</li>
     *   <li>Renames the backing {@code .java} file on disk.</li>
     *   <li>Saves all affected compilation units.</li>
     *   <li>Updates this object's {@code simpleName} and {@code qualifiedName} fields.</li>
     *   <li>Updates the meta-model's entity map key.</li>
     * </ol>
     * </p>
     *
     * @param newSimpleName the new simple name for this entity
     * @throws IOException if a file write fails
     */
    public void rename(String newSimpleName) throws IOException {
        String oldQualifiedName = this.qualifiedName;
        String packagePrefix = sourcePackage != null && !sourcePackage.isDefault()
                ? sourcePackage.getQualifiedName() + "."
                : "";
        String newQualifiedName = packagePrefix + newSimpleName;

        // 1. Scan all types in the model and update every reference to this type
        Set<SourceCompilationUnit> affectedCUs = new HashSet<>();
        for (CtType<?> type : metaModel.getCtModel().getAllTypes()) {
            boolean changed = false;
            List<CtTypeReference<?>> refs = type.getElements(new TypeFilter<>(CtTypeReference.class));
            for (CtTypeReference<?> ref : refs) {
                if (oldQualifiedName.equals(ref.getQualifiedName())) {
                    ref.setSimpleName(newSimpleName);
                    changed = true;
                }
            }
            if (changed && !type.getQualifiedName().equals(oldQualifiedName)) {
                // Look up the compilation unit for this other type and queue it for save
                SourceCompilationUnit otherCU = metaModel.getCompilationUnit(type.getQualifiedName());
                if (otherCU != null) {
                    affectedCUs.add(otherCU);
                }
            }
        }

        // 2. Rename the Spoon type itself
        ctType.setSimpleName(newSimpleName);

        // 2b. Refresh the CU's declared type reference so the pretty-printer finds the
        //     renamed type and not the old compiled shadow class on the classpath.
        if (compilationUnit != null) {
            compilationUnit.updateDeclaredType(ctType);
        }

        // 3. Defer the file move: point the compilation unit at the new file/name,
        //    regenerate its buffer (dirty, keyed by the new qualified name), drop any
        //    stale pre-rename buffer, and mark the old file for deletion on flush.
        if (compilationUnit != null && compilationUnit.getFile() != null) {
            File oldFile = compilationUnit.getFile();
            File parentDir = oldFile.getParentFile();
            File newFile = new File(parentDir, newSimpleName + ".java");
            compilationUnit.setFile(newFile);
            compilationUnit.setPrimaryTypeName(newQualifiedName);
            metaModel.clearPendingSource(oldQualifiedName);
            compilationUnit.regenerateFromAST(); // registers a fresh buffer under newQualifiedName
            if (!oldFile.equals(newFile)) {
                metaModel.registerPendingDeletion(oldFile);
            }
        }

        // 4. Regenerate all other affected CUs (their references were rewritten), deferred.
        for (SourceCompilationUnit cu : affectedCUs) {
            cu.regenerateFromAST();
        }

        // 5. Update fields
        this.simpleName = newSimpleName;
        this.qualifiedName = newQualifiedName;

        // 6. Update the meta-model's entity key
        metaModel.renameEntityKey(oldQualifiedName, newQualifiedName, this);
    }

    /**
     * Adds a SINGLE-cardinality property to this entity.
     *
     * <p>Generates getter and setter methods with the standard naming convention:
     * {@code "get" + capitalize(identifier)} and {@code "set" + capitalize(identifier)}.
     * Writes the change back to disk via {@link SourceCompilationUnit#save()}.</p>
     *
     * @param identifier        the property identifier (e.g. {@code "name"})
     * @param qualifiedTypeName the fully qualified type name (e.g. {@code "java.lang.String"})
     * @return the newly created {@link SourceModelProperty}
     * @throws IOException if the file write fails
     */
    public SourceModelProperty addSingleProperty(String identifier, String qualifiedTypeName) throws IOException {
        String cap = capitalise(identifier);
        return createProperty(identifier, qualifiedTypeName, false, java.util.Arrays.asList(
                AccessorSpec.generate(AccessorSpec.Role.GETTER, "get" + cap),
                AccessorSpec.generate(AccessorSpec.Role.SETTER, "set" + cap)));
    }

    /**
     * Creates a property (SINGLE or LIST) materialising the given accessors, each either
     * <b>generated</b> (a fresh conventional method) or <b>attached</b> (the PAMELA annotation
     * added to an existing developer-written method) — see {@link AccessorSpec}. This is the
     * unified primitive behind {@link #addSingleProperty}/{@link #addListProperty} and the
     * "New property" / "Promote" dialogs, so the user can explicitly choose, per role, which
     * methods are used and how (model-editing-design.md §3.3).
     *
     * <p>The {@code GETTER} spec is mandatory. For a SINGLE property the value type is the getter
     * return type and feeds the setter/updater parameter; for a LIST property it is the element
     * type ({@code List<T>}) and feeds the adder/remover/reindexer parameter. Written back through
     * the AST pretty-printer (like {@link #addSingleProperty}); a brand-new property restructures
     * the file anyway, and attaching annotates the existing method in place.</p>
     *
     * @param identifier        the property key
     * @param qualifiedTypeName the (element) type qualified name
     * @param list              {@code true} for a LIST property
     * @param accessors         the accessors to materialise (must contain exactly one GETTER)
     */
    public SourceModelProperty createProperty(String identifier, String qualifiedTypeName,
            boolean list, List<AccessorSpec> accessors) throws IOException {
        Factory factory = ctType.getFactory();
        CtTypeReference<?> valueType = factory.Type().createReference(qualifiedTypeName);

        CtMethod<?> getter = null;
        CtMethod<?> setter = null, updater = null, adder = null, remover = null, reindexer = null;

        for (AccessorSpec spec : accessors) {
            switch (spec.getRole()) {
                case GETTER:
                    getter = realiseGetter(factory, spec, identifier, valueType, list);
                    break;
                case SETTER:
                    setter = realiseAccessor(factory, spec, identifier, valueType, Setter.class);
                    break;
                case UPDATER:
                    updater = realiseAccessor(factory, spec, identifier, valueType, Updater.class);
                    break;
                case ADDER:
                    adder = realiseAccessor(factory, spec, identifier, valueType, Adder.class);
                    break;
                case REMOVER:
                    remover = realiseAccessor(factory, spec, identifier, valueType, Remover.class);
                    break;
                case REINDEXER:
                    reindexer = realiseReindexer(factory, spec, identifier, valueType);
                    break;
            }
        }
        if (getter == null) {
            throw new IllegalArgumentException("createProperty requires a GETTER accessor");
        }

        SourceModelProperty prop = new SourceModelProperty(getter, this);
        if (setter != null) {
            prop.registerSetter(setter);
        }
        if (updater != null) {
            prop.registerUpdater(updater);
        }
        if (adder != null) {
            prop.registerAdder(adder);
        }
        if (remover != null) {
            prop.registerRemover(remover);
        }
        if (reindexer != null) {
            prop.registerReindexer(reindexer);
        }
        addDeclaredProperty(prop);

        compilationUnit.regenerateFromAST();
        return prop;
    }

    /** Generates or annotates the getter ({@code T getX()} / {@code List<T> getX()}). */
    private CtMethod<?> realiseGetter(Factory factory, AccessorSpec spec, String identifier,
            CtTypeReference<?> valueType, boolean list) {
        CtTypeReference<?> returnType;
        if (list) {
            CtTypeReference<Object> listRef =
                    (CtTypeReference<Object>) factory.Type().createReference("java.util.List");
            listRef.addActualTypeArgument(valueType.clone());
            returnType = listRef;
        } else {
            returnType = valueType.clone();
        }
        CtMethod<?> method;
        if (spec.isGenerate()) {
            CtMethod<Object> m = factory.Core().createMethod();
            m.setSimpleName(spec.getMethodName());
            m.setType((CtTypeReference<Object>) returnType);
            m.addModifier(ModifierKind.PUBLIC);
            addGeneratedAccessor(m, identifier);
            method = m;
        } else {
            method = requireDeclaredMethod(spec.getMethodName(), 0);
        }
        CtAnnotation<?> ann = factory.Core().createAnnotation();
        ann.setAnnotationType(factory.Type().createReference(Getter.class));
        ann.addValue("value", identifierValueExpr(factory, identifier));
        if (list) {
            ann.addValue("cardinality", Cardinality.LIST);
        }
        method.addAnnotation(ann);
        return method;
    }

    /** Generates or annotates a single-parameter accessor ({@code void name(T value)}). */
    private CtMethod<?> realiseAccessor(Factory factory, AccessorSpec spec, String identifier,
            CtTypeReference<?> paramType, Class<? extends java.lang.annotation.Annotation> annotationType) {
        CtMethod<?> method;
        if (spec.isGenerate()) {
            CtMethod<Void> m = factory.Core().createMethod();
            m.setSimpleName(spec.getMethodName());
            m.setType((CtTypeReference<Void>) factory.Type().VOID_PRIMITIVE);
            m.addModifier(ModifierKind.PUBLIC);
            CtParameter<Object> p = factory.Core().createParameter();
            p.setSimpleName("value");
            p.setType((CtTypeReference<Object>) paramType.clone());
            m.addParameter(p);
            addGeneratedAccessor(m, identifier);
            method = m;
        } else {
            method = requireDeclaredMethod(spec.getMethodName(), 1);
        }
        CtAnnotation<?> ann = factory.Core().createAnnotation();
        ann.setAnnotationType(factory.Type().createReference(annotationType));
        ann.addValue("value", identifierValueExpr(factory, identifier));
        method.addAnnotation(ann);
        return method;
    }

    /** Generates or annotates a reindexer ({@code void name(T value, int index)}). */
    private CtMethod<?> realiseReindexer(Factory factory, AccessorSpec spec, String identifier,
            CtTypeReference<?> elementType) {
        CtMethod<?> method;
        if (spec.isGenerate()) {
            CtMethod<Void> m = factory.Core().createMethod();
            m.setSimpleName(spec.getMethodName());
            m.setType((CtTypeReference<Void>) factory.Type().VOID_PRIMITIVE);
            m.addModifier(ModifierKind.PUBLIC);
            CtParameter<Object> value = factory.Core().createParameter();
            value.setSimpleName("value");
            value.setType((CtTypeReference<Object>) elementType.clone());
            m.addParameter(value);
            CtParameter<Object> index = factory.Core().createParameter();
            index.setSimpleName("index");
            index.setType((CtTypeReference) factory.Type().INTEGER_PRIMITIVE);
            m.addParameter(index);
            addGeneratedAccessor(m, identifier);
            method = m;
        } else {
            method = requireDeclaredMethod(spec.getMethodName(), 2);
        }
        CtAnnotation<?> ann = factory.Core().createAnnotation();
        ann.setAnnotationType(factory.Type().createReference(Reindexer.class));
        ann.addValue("value", identifierValueExpr(factory, identifier));
        method.addAnnotation(ann);
        return method;
    }

    /** Finds an already-declared method by name + parameter count (for the attach mode). */
    private CtMethod<?> requireDeclaredMethod(String name, int paramCount) {
        CtMethod<?> m = findDeclaredMethod(name, paramCount);
        if (m == null) {
            throw new IllegalStateException("No method " + name + " with " + paramCount
                    + " parameter(s) on " + qualifiedName + " to attach");
        }
        return m;
    }

    /**
     * Adds a freshly generated accessor {@code method} to this entity's interface at a
     * <em>smart</em> position: right after the last existing type member that already concerns
     * property {@code propertyId} (so all the methods backing one property stay grouped). Falls
     * back to appending at the end of the type when the property has no other accessor yet.
     *
     * <p>This is the single insertion point for every generated accessor. The default
     * {@link CtType#addMethod} appends at the end of the class, which scatters a property's
     * methods; this keeps them contiguous.</p>
     */
    void addGeneratedAccessor(CtMethod<?> method, String propertyId) {
        List<CtTypeMember> members = ctType.getTypeMembers();
        int insertAfter = -1;
        for (int i = 0; i < members.size(); i++) {
            CtTypeMember member = members.get(i);
            if (member instanceof CtMethod
                    && propertyId.equals(accessorPropertyId((CtMethod<?>) member))) {
                insertAfter = i;
            }
        }
        if (insertAfter >= 0) {
            ctType.addTypeMemberAt(insertAfter + 1, method);
        } else {
            ctType.addMethod(method);
        }
    }

    // =========================================================================
    // Convention-aware identifier value (literal vs static-final-String constant)
    // property-identifier-constant-design.md §6
    // =========================================================================

    /**
     * The annotation {@code value} expression to emit for {@code propertyId}, honouring this
     * entity's {@link #effectiveIdentifierStyle()}: a string literal in {@code LITERAL} mode, or a
     * reference to a {@code static final String} constant (declared if absent — {@link
     * #ensureConstant}) in {@code CONSTANT} mode.
     */
    private CtExpression<?> identifierValueExpr(Factory factory, String propertyId) {
        if (effectiveIdentifierStyle() == PropertyIdentifierStyle.CONSTANT) {
            return buildConstantRead(factory, ensureConstant(factory, propertyId));
        }
        return factory.createLiteral(propertyId);
    }

    /**
     * Returns the simple name of a {@code static final String} constant holding {@code propertyId},
     * reusing a visible one (declared here, then inherited) of the same value (Q3) or declaring a
     * new local one ({@code SCREAMING_SNAKE_CASE}, collision-suffixed — Q5) inserted after the last
     * existing field.
     */
    private String ensureConstant(Factory factory, String propertyId) {
        String existing = findVisibleConstantName(propertyId);
        if (existing != null) {
            return existing;
        }
        String name = uniqueConstantName(screamingSnake(propertyId));

        CtField<String> field = factory.Core().createField();
        field.setSimpleName(name);
        // Print "String", not "java.lang.String", to match the source idiom.
        CtTypeReference<String> stringType = factory.Type().createReference(String.class);
        stringType.setSimplyQualified(true);
        field.setType(stringType);
        field.addModifier(ModifierKind.PUBLIC);
        field.addModifier(ModifierKind.STATIC);
        field.addModifier(ModifierKind.FINAL);
        field.setDefaultExpression(factory.createLiteral(propertyId));

        List<CtTypeMember> members = ctType.getTypeMembers();
        int insertAfter = -1;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i) instanceof CtField) {
                insertAfter = i;
            }
        }
        ctType.addTypeMemberAt(insertAfter + 1, field);
        return name;
    }

    /**
     * The constant name to use for {@code propertyId} <em>without</em> mutating anything: a visible
     * existing one of the same value, else a fresh unique {@code SCREAMING_SNAKE_CASE} name. Used by
     * the text-edit generation paths (which add the field via
     * {@link SourceAnnotationEditor#ensureConstantField}, not the AST).
     */
    String resolveConstantName(String propertyId) {
        String existing = findVisibleConstantName(propertyId);
        return existing != null ? existing : uniqueConstantName(screamingSnake(propertyId));
    }

    /** {@code true} if generating {@code propertyId} in CONSTANT mode needs a new constant declared. */
    boolean isConstantGenerationMode() {
        return effectiveIdentifierStyle() == PropertyIdentifierStyle.CONSTANT;
    }

    /** {@code true} if no {@code static final String} of {@code propertyId}'s value is visible yet. */
    boolean constantIsUndeclared(String propertyId) {
        return findVisibleConstantName(propertyId) == null;
    }

    /** A {@code static final String} field on this type or a super-entity whose value equals {@code propertyId}. */
    private String findVisibleConstantName(String propertyId) {
        for (CtField<?> f : ctType.getFields()) {
            if (isStringConstantWithValue(f, propertyId)) {
                return f.getSimpleName();
            }
        }
        for (SourceModelEntity superEntity : directSuperEntities) {
            String inherited = superEntity.findVisibleConstantName(propertyId);
            if (inherited != null) {
                return inherited;
            }
        }
        return null;
    }

    private static boolean isStringConstantWithValue(CtField<?> f, String value) {
        if (!f.getModifiers().contains(ModifierKind.STATIC)
                || !f.getModifiers().contains(ModifierKind.FINAL)
                || f.getType() == null
                || !"java.lang.String".equals(f.getType().getQualifiedName())) {
            return false;
        }
        return f.getDefaultExpression() instanceof spoon.reflect.code.CtLiteral<?>
                && value.equals(((spoon.reflect.code.CtLiteral<?>) f.getDefaultExpression()).getValue());
    }

    /** Makes {@code base} unique among the field names visible from this type. */
    private String uniqueConstantName(String base) {
        Set<String> taken = new HashSet<>();
        collectFieldNames(taken);
        if (!taken.contains(base)) {
            return base;
        }
        for (int i = 2; ; i++) {
            String candidate = base + "_" + i;
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
    }

    private void collectFieldNames(Set<String> into) {
        for (CtField<?> f : ctType.getFields()) {
            into.add(f.getSimpleName());
        }
        for (SourceModelEntity superEntity : directSuperEntities) {
            superEntity.collectFieldNames(into);
        }
    }

    /** Builds an unqualified read of a {@code static final String} constant declared on this type. */
    private CtExpression<?> buildConstantRead(Factory factory, String constantName) {
        CtFieldReference<String> ref = factory.Field()
                .createReference(ctType.getReference(), factory.Type().STRING, constantName);
        ref.setStatic(true);
        ref.setFinal(true);
        CtFieldRead<String> read = factory.Core().createFieldRead();
        read.setVariable(ref);
        // Implicit type access → the constant is printed unqualified (NAME, not Entity.NAME).
        read.setTarget(factory.Code().createTypeAccess(ctType.getReference(), true));
        return read;
    }

    /** {@code "startNode"} → {@code "START_NODE"} (insert {@code _} before each upper that follows a lower/digit). */
    private static String screamingSnake(String identifier) {
        StringBuilder sb = new StringBuilder(identifier.length() + 4);
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (i > 0 && Character.isUpperCase(c)
                    && (Character.isLowerCase(identifier.charAt(i - 1))
                            || Character.isDigit(identifier.charAt(i - 1)))) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    /**
     * The property identifier a method backs (the {@code value} of its
     * {@code @Getter}/{@code @Setter}/{@code @Adder}/{@code @Remover}/{@code @Reindexer}/
     * {@code @Updater} annotation), or {@code null} if the method is not a property accessor.
     */
    private static String accessorPropertyId(CtMethod<?> method) {
        Getter getter = method.getAnnotation(Getter.class);
        if (getter != null) {
            return getter.value();
        }
        Setter setter = method.getAnnotation(Setter.class);
        if (setter != null) {
            return setter.value();
        }
        Adder adder = method.getAnnotation(Adder.class);
        if (adder != null) {
            return adder.value();
        }
        Remover remover = method.getAnnotation(Remover.class);
        if (remover != null) {
            return remover.value();
        }
        Reindexer reindexer = method.getAnnotation(Reindexer.class);
        if (reindexer != null) {
            return reindexer.value();
        }
        Updater updater = method.getAnnotation(Updater.class);
        if (updater != null) {
            return updater.value();
        }
        return null;
    }

    /**
     * Adds a LIST-cardinality property to this entity.
     *
     * <p>Generates getter, adder, and remover methods with the standard naming
     * convention. Writes the change back to disk via
     * {@link SourceCompilationUnit#save()}.</p>
     *
     * @param identifier               the property identifier (e.g. {@code "children"})
     * @param qualifiedElementTypeName the fully qualified element type name
     *                                 (e.g. {@code "org.example.Child"})
     * @return the newly created {@link SourceModelProperty}
     * @throws IOException if the file write fails
     */
    public SourceModelProperty addListProperty(String identifier, String qualifiedElementTypeName)
            throws IOException {
        String cap = capitalise(identifier);
        return createProperty(identifier, qualifiedElementTypeName, true, java.util.Arrays.asList(
                AccessorSpec.generate(AccessorSpec.Role.GETTER, "get" + cap),
                AccessorSpec.generate(AccessorSpec.Role.ADDER, "addTo" + cap),
                AccessorSpec.generate(AccessorSpec.Role.REMOVER, "removeFrom" + cap)));
    }

    /**
     * Returns the names of this entity's methods that can be <em>promoted</em> to
     * a PAMELA property: public, no-argument, non-{@code void} getters
     * ({@code getXxx} / {@code isXxx}) that are not already annotated with a PAMELA
     * accessor ({@code @Getter}/{@code @Setter}/{@code @Adder}/{@code @Remover}).
     *
     * <p>LIST-shaped getters ({@code List<T>}) are excluded for now — only SINGLE
     * properties can be promoted in this first slice.</p>
     */
    public List<String> getPromotableGetterNames() {
        List<String> result = new ArrayList<>();
        for (CtMethod<?> m : ctType.getMethods()) {
            if (isPromotableGetter(m)) {
                result.add(m.getSimpleName());
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Promotes an existing plain getter method into a PAMELA property by inserting
     * an {@code @Getter} annotation (and, optionally, an {@code @Setter} on the
     * matching {@code setXxx} method) — the "promote" path of
     * {@code model-editing-design.md §1.1} for C4.
     *
     * <p>The edit is a targeted text insertion (like
     * {@link SourceMetaModel#declareAsEntity}), not an AST re-print, so it is
     * minimal-diff and immune to the Sniper annotation-insertion defect. The new
     * {@link SourceModelProperty} materialises on the next meta-model rebuild,
     * which the caller is responsible for triggering.</p>
     *
     * @param getterName         the existing getter method name (must be promotable)
     * @param propertyIdentifier the PAMELA property key to assign
     * @param includeSetter      also annotate the matching {@code setXxx} method if present
     * @throws IOException if the source file cannot be written
     */
    public void promoteMethodToProperty(String getterName, String propertyIdentifier,
                                        boolean includeSetter) throws IOException {
        CtMethod<?> getter = findDeclaredMethod(getterName, 0);
        if (getter == null || !isPromotableGetter(getter)) {
            throw new IllegalStateException("Not a promotable getter: " + getterName);
        }
        if (getter.getPosition() == null || !getter.getPosition().isValidPosition()) {
            throw new IllegalStateException("No source position for method " + getterName);
        }

        String source = compilationUnit.getText();

        // Honour the entity's identifier convention (property-identifier-constant-design.md §6):
        // a constant reference in CONSTANT mode (declaring the constant if absent), else a literal.
        boolean constMode = isConstantGenerationMode();
        String valueExpr = constMode ? resolveConstantName(propertyIdentifier)
                : "\"" + propertyIdentifier + "\"";
        boolean declareConstant = constMode && constantIsUndeclared(propertyIdentifier);

        // Collect insertions; apply by DESCENDING offset so earlier offsets stay valid.
        List<int[]> offsets = new ArrayList<>();   // [offset] paired with texts below
        List<String> texts = new ArrayList<>();
        offsets.add(new int[] { getter.getPosition().getSourceStart() });
        texts.add("@Getter(value = " + valueExpr + ")");

        CtMethod<?> setter = null;
        if (includeSetter) {
            setter = findSetterFor(getterName, getter.getType());
            if (setter != null && setter.getPosition() != null
                    && setter.getPosition().isValidPosition()) {
                offsets.add(new int[] { setter.getPosition().getSourceStart() });
                texts.add("@Setter(value = " + valueExpr + ")");
            } else {
                setter = null;
            }
        }

        // Sort the (offset, text) pairs by descending offset.
        Integer[] order = new Integer[offsets.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) ->
                Integer.compare(offsets.get(b)[0], offsets.get(a)[0]));
        for (int i : order) {
            source = SourceAnnotationEditor.insertAnnotationLine(source, offsets.get(i)[0], texts.get(i));
        }

        source = SourceAnnotationEditor.ensureImport(source, Getter.class.getName());
        if (setter != null) {
            source = SourceAnnotationEditor.ensureImport(source, Setter.class.getName());
        }
        if (declareConstant) {
            source = SourceAnnotationEditor.ensureConstantField(source,
                    ctType.getPosition().getSourceStart(), valueExpr, propertyIdentifier);
        }

        compilationUnit.setText(source);
    }

    // --- promote helpers -----------------------------------------------------

    /**
     * Signature-based check (no name convention — model-editing-design.md §3.3): a SINGLE
     * getter candidate is any no-arg, non-{@code void}, non-{@code List} method not already
     * carrying a PAMELA accessor annotation. Used by the entity-level "Promote method to
     * property" dialog (which creates a SINGLE property); LIST getters are promoted from the
     * method-level action via {@link #promoteGetterToProperty}.
     */
    private boolean isPromotableGetter(CtMethod<?> m) {
        if (!isUnannotatedNoArgValueMethod(m)) {
            return false;
        }
        return !"java.util.List".equals(m.getType().getQualifiedName());
    }

    /** Signature test: no-arg, non-{@code void} return, no existing PAMELA accessor annotation. */
    private static boolean isUnannotatedNoArgValueMethod(CtMethod<?> m) {
        if (!m.getParameters().isEmpty()) {
            return false;
        }
        CtTypeReference<?> returnType = m.getType();
        if (returnType == null || "void".equals(returnType.getQualifiedName())) {
            return false;
        }
        return m.getAnnotation(Getter.class) == null
                && m.getAnnotation(Setter.class) == null
                && m.getAnnotation(Adder.class) == null
                && m.getAnnotation(Remover.class) == null;
    }

    /**
     * Promotes an existing no-arg getter into a new PAMELA property by inserting an
     * {@code @Getter} annotation — the method-level "promote" path
     * (model-editing-design.md §3.3). When {@code list} is true the property is declared
     * {@code cardinality = Getter.Cardinality.LIST} (the element type is read by PAMELA from
     * the {@code List<T>} return type). Mutators (setter / adder / remover…) are attached
     * separately via the dedicated method-level actions.
     *
     * <p>Targeted text edit (insert annotation line + import), like
     * {@link #promoteMethodToProperty}; minimal-diff and Sniper-safe. The new
     * {@link SourceModelProperty} materialises on the next rebuild (caller-triggered).</p>
     */
    public void promoteGetterToProperty(String getterName, String propertyIdentifier, boolean list)
            throws IOException {
        promoteGetterToProperty(getterName, propertyIdentifier, list,
                java.util.Collections.emptyMap());
    }

    /**
     * Promotes a getter into a new property and, in the <strong>same</strong> targeted text edit,
     * attaches existing sibling methods as the property's other accessors
     * ({@code extraAccessors}: annotation class → method name) — the "promote a getter and pull in
     * its setter/adder/remover/reindexer" path (model-editing-design.md §3.3a). Each accessor's
     * parameter count is derived from its role ({@code @Reindexer} = 2, the others = 1). All
     * annotations are inserted in one descending-offset pass (earlier offsets stay valid) with the
     * imports ensured once; minimal-diff and Sniper-safe.
     */
    public void promoteGetterToProperty(String getterName, String propertyIdentifier, boolean list,
            java.util.Map<Class<? extends java.lang.annotation.Annotation>, String> extraAccessors)
            throws IOException {
        CtMethod<?> getter = findDeclaredMethod(getterName, 0);
        if (getter == null || !isUnannotatedNoArgValueMethod(getter)) {
            throw new IllegalStateException("Not a promotable getter: " + getterName);
        }
        if (getter.getPosition() == null || !getter.getPosition().isValidPosition()) {
            throw new IllegalStateException("No source position for method " + getterName);
        }

        // Honour the entity's identifier convention (property-identifier-constant-design.md §6).
        boolean constMode = isConstantGenerationMode();
        String valueExpr = constMode ? resolveConstantName(propertyIdentifier)
                : "\"" + propertyIdentifier + "\"";
        boolean declareConstant = constMode && constantIsUndeclared(propertyIdentifier);

        // Collect (offset, annotation-text) insertions; apply by DESCENDING offset.
        List<Integer> offsets = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        java.util.Set<String> imports = new java.util.LinkedHashSet<>();

        offsets.add(getter.getPosition().getSourceStart());
        texts.add(list
                ? "@Getter(value = " + valueExpr + ", cardinality = Getter.Cardinality.LIST)"
                : "@Getter(value = " + valueExpr + ")");
        imports.add(Getter.class.getName());

        for (java.util.Map.Entry<Class<? extends java.lang.annotation.Annotation>, String> e
                : extraAccessors.entrySet()) {
            Class<? extends java.lang.annotation.Annotation> annClass = e.getKey();
            String methodName = e.getValue();
            if (methodName == null) {
                continue;
            }
            int paramCount = annClass == Reindexer.class ? 2 : 1;
            CtMethod<?> accessor = findDeclaredMethod(methodName, paramCount);
            if (accessor == null || accessor.getPosition() == null
                    || !accessor.getPosition().isValidPosition()) {
                continue;
            }
            offsets.add(accessor.getPosition().getSourceStart());
            texts.add("@" + annClass.getSimpleName() + "(value = " + valueExpr + ")");
            imports.add(annClass.getName());
        }

        String source = compilationUnit.getText();

        Integer[] order = new Integer[offsets.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(offsets.get(b), offsets.get(a)));
        for (int i : order) {
            source = SourceAnnotationEditor.insertAnnotationLine(source, offsets.get(i), texts.get(i));
        }
        for (String imp : imports) {
            source = SourceAnnotationEditor.ensureImport(source, imp);
        }
        if (declareConstant) {
            source = SourceAnnotationEditor.ensureConstantField(source,
                    ctType.getPosition().getSourceStart(), valueExpr, propertyIdentifier);
        }
        compilationUnit.setText(source);
    }

    /**
     * Declared methods that carry <em>no</em> PAMELA accessor annotation, as Spoon-free
     * {@link MethodSignature}s (name + parameter type qualified names). Used by the UI to find,
     * by signature, the candidate setter/adder/remover/reindexer methods to pull in when a getter
     * is promoted (model-editing-design.md §3.3a).
     */
    public List<MethodSignature> getCandidateAccessorMethods() {
        List<MethodSignature> result = new ArrayList<>();
        for (CtMethod<?> m : ctType.getMethods()) {
            if (m.getAnnotation(Getter.class) != null || m.getAnnotation(Setter.class) != null
                    || m.getAnnotation(Adder.class) != null || m.getAnnotation(Remover.class) != null
                    || m.getAnnotation(Reindexer.class) != null
                    || m.getAnnotation(Updater.class) != null) {
                continue;
            }
            List<String> paramTypes = new ArrayList<>();
            for (CtParameter<?> p : m.getParameters()) {
                paramTypes.add(p.getType() != null ? p.getType().getQualifiedName() : null);
            }
            result.add(new MethodSignature(m.getSimpleName(), paramTypes));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Lot 6b — declare an existing method as an operation / initializer / finder
    // (model-editing-design.md §3.3a). All targeted text edits (annotation line +
    // import), minimal-diff and Sniper-safe; the concept materialises on rebuild.
    // -------------------------------------------------------------------------

    /** Marks an existing method as an {@code @Operation} (editor-facing marker). */
    public void declareOperation(String methodName, int paramCount) throws IOException {
        CtMethod<?> m = requireMethodWithPosition(methodName, paramCount);
        String source = compilationUnit.getText();
        source = SourceAnnotationEditor.insertAnnotationLine(source,
                m.getPosition().getSourceStart(), "@Operation");
        source = SourceAnnotationEditor.ensureImport(source, Operation.class.getName());
        compilationUnit.setText(source);
    }

    /**
     * Declares an existing method as an {@code @Initializer} (a factory), mapping each of its
     * parameters to a property via an inline {@code @Parameter("propId")}. {@code parameterPropertyIds}
     * is ordered by parameter (size = the method's parameter count). The {@code @Initializer} line and
     * every {@code @Parameter} are written in one descending-offset pass.
     */
    public void declareInitializer(String methodName, List<String> parameterPropertyIds)
            throws IOException {
        int paramCount = parameterPropertyIds.size();
        CtMethod<?> m = requireMethodWithPosition(methodName, paramCount);

        // (offset, isLineInsertion, text), applied by DESCENDING offset so earlier offsets stay valid.
        List<int[]> meta = new ArrayList<>();   // [offset, isLine?1:0]
        List<String> texts = new ArrayList<>();

        meta.add(new int[] { m.getPosition().getSourceStart(), 1 });
        texts.add("@Initializer");

        for (int i = 0; i < paramCount; i++) {
            CtParameter<?> p = m.getParameters().get(i);
            if (p.getPosition() == null || !p.getPosition().isValidPosition()) {
                throw new IllegalStateException("No source position for parameter " + i
                        + " of " + methodName);
            }
            meta.add(new int[] { p.getPosition().getSourceStart(), 0 });
            texts.add("@Parameter(\"" + parameterPropertyIds.get(i) + "\") ");
        }

        String source = compilationUnit.getText();

        Integer[] order = new Integer[meta.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(meta.get(b)[0], meta.get(a)[0]));
        for (int i : order) {
            int offset = meta.get(i)[0];
            if (meta.get(i)[1] == 1) {
                source = SourceAnnotationEditor.insertAnnotationLine(source, offset, texts.get(i));
            } else {
                source = source.substring(0, offset) + texts.get(i) + source.substring(offset);
            }
        }
        source = SourceAnnotationEditor.ensureImport(source, Initializer.class.getName());
        source = SourceAnnotationEditor.ensureImport(source, Parameter.class.getName());
        compilationUnit.setText(source);
    }

    /**
     * Declares an existing one-argument method as an {@code @Finder} over a LIST property
     * ({@code collectionPropertyId}), matching the argument against the element property
     * {@code attributeName}. {@code multiValued} mirrors the return type ({@code List<E>} → true).
     */
    public void declareFinder(String methodName, String collectionPropertyId, String attributeName,
            boolean multiValued) throws IOException {
        CtMethod<?> m = requireMethodWithPosition(methodName, 1);
        StringBuilder ann = new StringBuilder("@Finder(collection = \"")
                .append(collectionPropertyId).append("\", attribute = \"").append(attributeName)
                .append("\"");
        if (multiValued) {
            ann.append(", isMultiValued = true");
        }
        ann.append(")");
        String source = compilationUnit.getText();
        source = SourceAnnotationEditor.insertAnnotationLine(source,
                m.getPosition().getSourceStart(), ann.toString());
        source = SourceAnnotationEditor.ensureImport(source, Finder.class.getName());
        compilationUnit.setText(source);
    }

    private CtMethod<?> requireMethodWithPosition(String methodName, int paramCount) {
        CtMethod<?> m = findDeclaredMethod(methodName, paramCount);
        if (m == null) {
            throw new IllegalStateException("No method " + methodName + " with " + paramCount
                    + " param(s) on " + qualifiedName);
        }
        if (m.getPosition() == null || !m.getPosition().isValidPosition()) {
            throw new IllegalStateException("No source position for method " + methodName);
        }
        return m;
    }

    /** Finds a declared method by name with exactly {@code paramCount} parameters. */
    private CtMethod<?> findDeclaredMethod(String name, int paramCount) {
        for (CtMethod<?> m : ctType.getMethods()) {
            if (m.getSimpleName().equals(name) && m.getParameters().size() == paramCount) {
                return m;
            }
        }
        return null;
    }

    /** Finds the {@code setXxx(T)} matching {@code getXxx}/{@code isXxx}, or null. */
    private CtMethod<?> findSetterFor(String getterName, CtTypeReference<?> valueType) {
        String base = getterName.startsWith("is")
                ? getterName.substring(2) : getterName.substring(3);
        String setterName = "set" + base;
        for (CtMethod<?> m : ctType.getMethods()) {
            if (m.getSimpleName().equals(setterName)
                    && m.getParameters().size() == 1
                    && m.getAnnotation(Setter.class) == null) {
                CtTypeReference<?> paramType = m.getParameters().get(0).getType();
                if (paramType != null && valueType != null
                        && paramType.getQualifiedName().equals(valueType.getQualifiedName())) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * Deletes this entity from the meta-model and removes its backing {@code .java}
     * file from disk.
     *
     * @throws IOException if the file deletion fails
     */
    public void delete() throws IOException {
        // 1. Defer the file removal to the next flush.
        if (compilationUnit != null && compilationUnit.getFile() != null) {
            metaModel.registerPendingDeletion(compilationUnit.getFile());
        }

        // 2. Drop any unsaved buffer for this unit (it is being deleted) and
        //    unregister it from the meta-model.
        metaModel.clearPendingSource(qualifiedName);
        metaModel.unregisterEntity(qualifiedName);
        metaModel.unregisterCompilationUnit(qualifiedName);

        // 3. Remove from package
        if (sourcePackage != null) {
            sourcePackage.removeEntity(this);
        }
    }

    /**
     * Adds a super-entity to this entity's {@code extends} clause.
     *
     * @param superEntity the entity to inherit from
     * @throws IOException if saving the compilation unit fails
     * @throws IllegalStateException if this entity is not backed by a {@link CtInterface}
     */
    public void addSuperEntity(SourceModelEntity superEntity) throws IOException {
        if (!(ctType instanceof CtInterface)) {
            throw new IllegalStateException("addSuperEntity is only supported on interfaces");
        }
        CtInterface<?> ctInterface = (CtInterface<?>) ctType;
        ctInterface.addSuperInterface(superEntity.getCtType().getReference());
        addDirectSuperEntity(superEntity);
        compilationUnit.regenerateFromAST();
    }

    /**
     * Removes a super-entity from this entity's {@code extends} clause.
     *
     * @param superEntity the super-entity to remove
     * @throws IOException if saving the compilation unit fails
     * @throws IllegalStateException if this entity is not backed by a {@link CtInterface}
     */
    public void removeSuperEntity(SourceModelEntity superEntity) throws IOException {
        if (!(ctType instanceof CtInterface)) {
            throw new IllegalStateException("removeSuperEntity is only supported on interfaces");
        }
        CtInterface<?> ctInterface = (CtInterface<?>) ctType;

        // Find the matching CtTypeReference in the super-interface set
        CtTypeReference<?> toRemove = null;
        String targetName = superEntity.getQualifiedName();
        for (CtTypeReference<?> ref : ctInterface.getSuperInterfaces()) {
            if (targetName.equals(ref.getQualifiedName())) {
                toRemove = ref;
                break;
            }
        }

        if (toRemove != null) {
            ctInterface.removeSuperInterface(toRemove);
        }

        removeDirectSuperEntity(superEntity);
        compilationUnit.regenerateFromAST();
    }

    // =========================================================================
    // Direct super-entities — editable inspector (FlatDesign table).
    //
    // The inspector lists getDirectSuperEntities() in a FlatDesign table whose
    // footer "+" adds a link and "-" removes the selected one. The add is delegated
    // to the existing AddSuperEntityAction (its own entity-selection dialog), reached
    // via the UI-intent signal requestAddSuperEntity() (the model fires, the app runs
    // the action — like requestRename/requestChangeType, model-editing-design.md §6).
    // The remove goes through unlinkSuperEntity(), which mutates via the existing
    // removeSuperEntity primitive and fires "directSuperEntities" so the app rebuilds.
    // =========================================================================

    /**
     * Removes an inheritance link from the inspector table and fires
     * {@code "directSuperEntities"} so the application rebuilds. No-op if the
     * argument is not a current direct super-entity.
     */
    public void unlinkSuperEntity(SourceModelEntity superEntity) throws IOException {
        if (superEntity == null || !directSuperEntities.contains(superEntity)) {
            return;
        }
        removeSuperEntity(superEntity);
        pcSupport.firePropertyChange("directSuperEntities", superEntity, null);
    }

    /**
     * Fires a UI-intent signal that the user asked to add a super-entity (the table's
     * "+" footer). The model opens no dialog; the application observes the inspected
     * element and runs {@code AddSuperEntityAction} in response (model-editing-design.md §6).
     */
    public void requestAddSuperEntity() {
        pcSupport.firePropertyChange("addSuperEntityRequested", null, this);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Capitalises the first character of a string, e.g. {@code "name"} → {@code "Name"}.
     */
    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Fully qualified name of the interface/class,
     * e.g. {@code "org.example.Person"}.
     */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /**
     * Simple name, e.g. {@code "Person"}.
     */
    public String getSimpleName() {
        return simpleName;
    }

    /** The package containing this entity. */
    public SourcePackage getSourcePackage() {
        return sourcePackage;
    }

    /** The owning meta-model. */
    public SourceMetaModel getMetaModel() {
        return metaModel;
    }

    /**
     * {@code true} if {@code @ModelEntity.isAbstract()} is set, meaning the
     * entity cannot be instantiated directly.
     */
    public boolean isAbstract() {
        return abstractEntity;
    }

    /**
     * Editable setter (Lot 4 inspector): toggles {@code @ModelEntity(isAbstract=…)}
     * in the source and fires a {@code PropertyChange}. Per the editable-inspector
     * contract it mutates the source and updates the in-memory field but does
     * <b>not</b> rebuild — the UI observes the event and triggers the rebuild
     * (see {@code model-editing-design.md §6}).
     *
     * @throws IOException if the source file cannot be written
     */
    public void setAbstract(boolean isAbstract) throws IOException {
        if (isAbstract == this.abstractEntity) {
            return;
        }
        if (ctType.getPosition() == null || !ctType.getPosition().isValidPosition()) {
            throw new IllegalStateException("No source position for " + qualifiedName);
        }
        String source = compilationUnit.getText();
        int declStart = ctType.getPosition().getSourceStart();
        // isAbstract defaults to false in PAMELA, so removing the parameter (null)
        // is the canonical way to express the non-abstract case.
        String edited = SourceAnnotationEditor.setAnnotationParameter(
                source, declStart, "ModelEntity", "isAbstract", isAbstract ? "true" : null);
        compilationUnit.setText(edited);

        boolean old = this.abstractEntity;
        this.abstractEntity = isAbstract;
        pcSupport.firePropertyChange("abstract", old, isAbstract);
    }

    /** Initialization policy declared on {@code @ModelEntity}. */
    public InitPolicy getInitPolicy() {
        return initPolicy;
    }

    /** {@code true} if {@code @ModelEntity.inheritInitializers()} is set. */
    public boolean isInheritInitializers() {
        return inheritInitializers;
    }

    /**
     * The {@code .java} file that declares this type, or {@code null} if
     * not yet resolved.
     */
    public SourceCompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    /**
     * The {@code @ImplementationClass} for this entity, or {@code null} if
     * the annotation is absent.
     */
    public SourceImplementationClass getImplementationClass() {
        return implementationClass;
    }

    /**
     * Properties declared directly on this type (does not include inherited
     * properties).
     *
     * @return an unmodifiable map keyed by property identifier
     */
    public Map<String, SourceModelProperty> getDeclaredProperties() {
        return Collections.unmodifiableMap(declaredProperties);
    }

    // =========================================================================
    // Property-identifier style (literal vs static-final-String constant)
    // property-identifier-constant-design.md
    // =========================================================================

    /**
     * Aggregates the identifier style over the <em>declared</em> properties (Q1/Q4):
     * all-constant → {@link PropertyIdentifierStyle#CONSTANT}, all-literal →
     * {@link PropertyIdentifierStyle#LITERAL}, both → {@link PropertyIdentifierStyle#MIXED},
     * none → {@link PropertyIdentifierStyle#UNDETERMINED}. Called at the end of Phase 2.
     */
    void computeIdentifierStyle() {
        int constants = 0;
        int literals = 0;
        for (SourceModelProperty p : declaredProperties.values()) {
            if (p.getIdentifierStyle() == PropertyIdentifierStyle.CONSTANT) {
                constants++;
            } else {
                literals++;
            }
        }
        if (constants == 0 && literals == 0) {
            identifierStyle = PropertyIdentifierStyle.UNDETERMINED;
        } else if (literals == 0) {
            identifierStyle = PropertyIdentifierStyle.CONSTANT;
        } else if (constants == 0) {
            identifierStyle = PropertyIdentifierStyle.LITERAL;
        } else {
            identifierStyle = PropertyIdentifierStyle.MIXED;
        }
    }

    /**
     * The detected aggregate identifier style of this entity (may be {@code MIXED} or
     * {@code UNDETERMINED}). For the style to actually <em>use</em> when generating, see
     * {@link #effectiveIdentifierStyle()}.
     */
    public PropertyIdentifierStyle getIdentifierStyle() {
        return identifierStyle;
    }

    /**
     * The identifier style to use when generating a new property/accessor on this entity (Q2/Q5):
     * <ul>
     *   <li>{@code CONSTANT}/{@code LITERAL} → that style;</li>
     *   <li>{@code MIXED} → the entity's dominant style (most-local wins; tie → {@code CONSTANT});</li>
     *   <li>{@code UNDETERMINED} → the meta-model's configured default
     *       ({@link SourceMetaModel#getDefaultPropertyIdentifierStyle()}).</li>
     * </ul>
     * Always returns {@code CONSTANT} or {@code LITERAL}.
     */
    public PropertyIdentifierStyle effectiveIdentifierStyle() {
        switch (identifierStyle) {
            case CONSTANT:
            case LITERAL:
                return identifierStyle;
            case MIXED:
                return dominantIdentifierStyle();
            case UNDETERMINED:
            default:
                return metaModel.getDefaultPropertyIdentifierStyle();
        }
    }

    /** Majority style over the declared properties; ties resolve to {@code CONSTANT}. */
    private PropertyIdentifierStyle dominantIdentifierStyle() {
        int constants = 0;
        int literals = 0;
        for (SourceModelProperty p : declaredProperties.values()) {
            if (p.getIdentifierStyle() == PropertyIdentifierStyle.CONSTANT) {
                constants++;
            } else {
                literals++;
            }
        }
        return literals > constants ? PropertyIdentifierStyle.LITERAL : PropertyIdentifierStyle.CONSTANT;
    }

    /**
     * Direct (non-transitive) super-entities,
     * i.e. the {@code @ModelEntity} interfaces that this type extends directly.
     *
     * @return an unmodifiable list in declaration order
     */
    public List<SourceModelEntity> getDirectSuperEntities() {
        return Collections.unmodifiableList(directSuperEntities);
    }

    /**
     * Initializer methods declared on this entity.
     *
     * @return an unmodifiable list in discovery order
     */
    public List<SourceModelInitializer> getInitializers() {
        return Collections.unmodifiableList(initializers);
    }

    /**
     * Custom methods (operations) declared on this entity's interface, after applying the
     * meta-model's {@link CustomMethodFilter}. See {@code custom-method-design.md}.
     *
     * @return an unmodifiable list in declaration order
     */
    public List<SourceCustomMethod> getDeclaredCustomMethods() {
        return Collections.unmodifiableList(declaredCustomMethods);
    }

    /**
     * The XML tag from {@code @XMLElement} on this type, or {@code null} if
     * the annotation is absent or has no explicit tag.
     */
    public String getXmlTag() {
        return xmlTag;
    }

    /**
     * Diagnostics detected on this entity during construction and validation.
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
        return "SourceModelEntity(" + qualifiedName + ")";
    }
}
