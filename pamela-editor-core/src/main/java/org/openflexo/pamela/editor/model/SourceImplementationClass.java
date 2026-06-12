package org.openflexo.pamela.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Initializer;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Reindexer;
import org.openflexo.pamela.annotations.Setter;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;

/**
 * Represents the abstract Java class that provides custom logic for a
 * {@link SourceModelEntity} interface.  Declared via
 * {@code @ImplementationClass(MyImpl.class)} on the interface.
 *
 * <p>The PAMELA proxy merges the framework-managed property behaviour
 * with the hand-written logic of this class.</p>
 *
 * <p>Validation rules fired during construction:
 * <ul>
 *   <li>Implementation class is not abstract → {@link Warning}</li>
 *   <li>Implementation class does not implement the {@code @ModelEntity}
 *       interface → {@link Error}</li>
 * </ul>
 * </p>
 */
public class SourceImplementationClass implements SourceElement {

    // Internal Spoon reference — never exposed in the public API
    private final CtClass<?> ctClass;

    private final SourceModelEntity entity;
    private final SourceCompilationUnit compilationUnit;
    private final String qualifiedName;
    private final String simpleName;
    private final boolean abstractClass;
    private final List<SourceCustomMethod> customMethods;

    /**
     * Constructs a {@code SourceImplementationClass}.
     *
     * @param ctClass           the Spoon class node (must not be {@code null})
     * @param entity            the {@code @ModelEntity} interface this class implements
     * @param compilationUnit   the {@code .java} file that declares this class
     */
    public SourceImplementationClass(CtClass<?> ctClass, SourceModelEntity entity, SourceCompilationUnit compilationUnit) {
        this.ctClass = ctClass;
        this.entity = entity;
        this.compilationUnit = compilationUnit;
        this.qualifiedName = ctClass.getQualifiedName();
        this.simpleName = ctClass.getSimpleName();
        this.abstractClass = ctClass.getModifiers().contains(ModifierKind.ABSTRACT);

        // Validation: warn if impl class is not abstract
        if (!abstractClass) {
            entity.getMetaModel().fireIssue(new Warning(
                    "Implementation class " + qualifiedName + " should be declared abstract"));
        }

        // Validation: check that the impl class implements the @ModelEntity interface
        // We verify by checking super-interfaces transitively via qualified names.
        // A simple check: the entity's qualified name should appear in the implemented interfaces.
        boolean implementsEntity = implementsInterface(ctClass, entity.getQualifiedName());
        if (!implementsEntity) {
            entity.getMetaModel().fireIssue(new Error(
                    "Implementation class " + qualifiedName
                    + " does not implement @ModelEntity interface " + entity.getQualifiedName()));
        }

        // Build custom methods: collect methods not annotated with PAMELA property annotations
        this.customMethods = buildCustomMethods();
    }

    /**
     * Checks (shallowly) whether {@code ctClass} or any of its super-types
     * implement the interface with the given qualified name.
     */
    private static boolean implementsInterface(CtClass<?> ctClass, String interfaceName) {
        // Check direct super-interfaces
        for (spoon.reflect.reference.CtTypeReference<?> ref : ctClass.getSuperInterfaces()) {
            if (ref.getQualifiedName().equals(interfaceName)) {
                return true;
            }
        }
        // Check super-class recursively
        if (ctClass.getSuperclass() != null) {
            spoon.reflect.declaration.CtType<?> superType = ctClass.getSuperclass().getTypeDeclaration();
            if (superType instanceof CtClass) {
                if (implementsInterface((CtClass<?>) superType, interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Builds the list of {@link SourceCustomMethod}s: methods declared
     * directly on this class that are not PAMELA property methods.
     */
    private List<SourceCustomMethod> buildCustomMethods() {
        Set<String> pamelaNames = buildPamelaMethodNames();
        Set<CtMethod<?>> methods = ctClass.getMethods();
        List<SourceCustomMethod> result = new ArrayList<>();
        for (CtMethod<?> method : methods) {
            if (!isPamelaPropertyMethod(method) && !pamelaNames.contains(method.getSimpleName())) {
                result.add(new SourceCustomMethod(method, this));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Builds the set of method names that are managed by PAMELA on the entity interface
     * and all its super-interfaces, by walking the Spoon type hierarchy directly.
     *
     * <p>This must use Spoon (not {@link SourceModelEntity#getAllProperties()}) because
     * it is called during Phase 2, before the {@code directSuperEntities} links are
     * established in Phase 3. An impl-class method whose name appears here is a PAMELA
     * property override (e.g. {@code @Override setStartNode()}) and must not be shown
     * as a custom method, even when it carries no PAMELA annotation itself.</p>
     */
    private Set<String> buildPamelaMethodNames() {
        Set<String> names = new java.util.HashSet<>();
        collectPamelaMethodNames(entity.getCtType(), names,
                new java.util.HashSet<>());
        return names;
    }

    /**
     * Recursively collects the names of PAMELA-annotated methods from {@code type}
     * and all its super-interfaces.
     */
    private static void collectPamelaMethodNames(spoon.reflect.declaration.CtType<?> type,
            Set<String> names, Set<String> visited) {
        if (type == null) return;
        String qn = type.getQualifiedName();
        if (!visited.add(qn)) return;
        for (CtMethod<?> method : type.getMethods()) {
            if (isPamelaPropertyMethod(method)) {
                names.add(method.getSimpleName());
            }
        }
        for (spoon.reflect.reference.CtTypeReference<?> superRef : type.getSuperInterfaces()) {
            spoon.reflect.declaration.CtType<?> superType = superRef.getTypeDeclaration();
            if (superType != null && !superType.isShadow()) {
                collectPamelaMethodNames(superType, names, visited);
            }
        }
    }

    /**
     * Returns {@code true} if the method carries any PAMELA property annotation.
     * This covers methods on the impl class that are themselves annotated (rare but
     * possible). The name-based check in {@link #buildPamelaMethodNames()} handles
     * {@code @Override} methods whose annotation lives on the interface.
     */
    private static boolean isPamelaPropertyMethod(CtMethod<?> method) {
        return method.getAnnotation(Getter.class) != null
                || method.getAnnotation(Setter.class) != null
                || method.getAnnotation(Adder.class) != null
                || method.getAnnotation(Remover.class) != null
                || method.getAnnotation(Reindexer.class) != null
                || method.getAnnotation(Initializer.class) != null;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** The {@code @ModelEntity} interface this class implements. */
    public SourceModelEntity getEntity() {
        return entity;
    }

    /** The {@code .java} file that declares this implementation class. */
    public SourceCompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    /**
     * Fully qualified class name,
     * e.g. {@code "org.example.PersonImpl"}.
     */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /**
     * Simple class name, e.g. {@code "PersonImpl"}.
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * {@code true} if the class is declared {@code abstract}.
     * Should always be {@code true}; a {@link Warning} is fired otherwise.
     */
    public boolean isAbstract() {
        return abstractClass;
    }

    /**
     * Methods declared in this class that are <em>not</em> managed by PAMELA
     * (no {@code @Getter}, {@code @Setter}, etc.).
     */
    public List<SourceCustomMethod> getCustomMethods() {
        return customMethods;
    }

    @Override
    public String toString() {
        return "SourceImplementationClass(" + qualifiedName + ")";
    }
}
