package org.openflexo.pamela.editor.model;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
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
                    "Implementation class " + qualifiedName + " should be declared abstract", this));
        }

        // Validation: check that the impl class implements the @ModelEntity interface
        // We verify by checking super-interfaces transitively via qualified names.
        // A simple check: the entity's qualified name should appear in the implemented interfaces.
        boolean implementsEntity = implementsInterface(ctClass, entity.getQualifiedName());
        if (!implementsEntity) {
            entity.getMetaModel().fireIssue(new Error(
                    "Implementation class " + qualifiedName
                    + " does not implement @ModelEntity interface " + entity.getQualifiedName(), this));
        }
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
     * Finds the method on this implementation class that implements the given entity-interface
     * method, matched by simple name and parameter types. Used to source-navigate from an
     * operation to its hand-written body and to drive the
     * {@link CustomMethodFilter#IMPLEMENTED_IN_IMPL} filter.
     *
     * @param interfaceMethod a method declared on the entity interface
     * @return the matching impl method, or {@code null} if the class does not declare it
     */
    CtMethod<?> findImplementingMethod(CtMethod<?> interfaceMethod) {
        for (CtMethod<?> m : ctClass.getMethods()) {
            if (m.getSimpleName().equals(interfaceMethod.getSimpleName())
                    && sameParameterTypes(m, interfaceMethod)) {
                return m;
            }
        }
        return null;
    }

    /** Compares two methods' parameter lists by erased qualified type name. */
    private static boolean sameParameterTypes(CtMethod<?> a, CtMethod<?> b) {
        java.util.List<CtParameter<?>> pa = a.getParameters();
        java.util.List<CtParameter<?>> pb = b.getParameters();
        if (pa.size() != pb.size()) {
            return false;
        }
        for (int i = 0; i < pa.size(); i++) {
            spoon.reflect.reference.CtTypeReference<?> ta = pa.get(i).getType();
            spoon.reflect.reference.CtTypeReference<?> tb = pb.get(i).getType();
            String na = (ta != null) ? ta.getQualifiedName() : "";
            String nb = (tb != null) ? tb.getQualifiedName() : "";
            if (!na.equals(nb)) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** The {@code @ModelEntity} interface this class implements. */
    public SourceModelEntity getEntity() {
        return entity;
    }

    @Override
    public SourceMetaModel getMetaModel() {
        return entity.getMetaModel();
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

    @Override
    public String toString() {
        return "SourceImplementationClass(" + qualifiedName + ")";
    }
}
