package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.annotations.Getter.Cardinality;

/**
 * Tests for the {@link SourceMetaModel} layer using the {@code test/model1/} test model.
 *
 * <p>The model1 entity set (all in package {@code test.model1}):
 * <pre>
 *   Foo1  — one SINGLE property "foo2" of type Foo2 (another @ModelEntity)
 *   Foo2  — one SINGLE property "name" of type String
 * </pre>
 * Neither entity is abstract, has an {@code @ImplementationClass}, or declares
 * any {@code @Initializer}.</p>
 *
 * <p>The BFS traversal follows super-interfaces and {@code @Getter} property
 * types upward (toward referenced types, not toward subtypes):
 * <ul>
 *   <li>Root = {@code Foo1} → property type {@code foo2} is {@code Foo2}
 *       ({@code @ModelEntity}) → total: <b>2 entities</b></li>
 *   <li>Root = {@code Foo2} only → property type {@code name} is {@code String}
 *       (not {@code @ModelEntity}) → total: <b>1 entity</b></li>
 * </ul>
 * </p>
 */
public class TestModel1 {

    /**
     * Meta-model built from {@code Foo1} as the sole root type.
     * Both {@code Foo1} and {@code Foo2} are reachable via the upward traversal
     * (the "foo2" getter returns {@code Foo2} which is itself {@code @ModelEntity}).
     */
    private static SourceMetaModel metaModelFromFoo1;

    /**
     * Meta-model built from {@code Foo2} as the sole root type.
     * Only {@code Foo2} is reachable: its "name" getter returns {@code String},
     * which is not {@code @ModelEntity}.
     */
    private static SourceMetaModel metaModelFromFoo2;

    @BeforeClass
    public static void buildMetaModels() {
        File sourceDir = new File(System.getProperty("user.dir") + "/src/test/java/test/model1");

        // --- meta-model rooted at Foo1 (discovers both Foo1 and Foo2) ---
        metaModelFromFoo1 = new SourceMetaModel();
        metaModelFromFoo1.addSourceDirectory(sourceDir);
        metaModelFromFoo1.addRootTypeName("test.model1.Foo1");
        metaModelFromFoo1.buildMetaModel();

        // --- meta-model rooted at Foo2 only (discovers Foo2 alone) ---
        metaModelFromFoo2 = new SourceMetaModel();
        metaModelFromFoo2.addSourceDirectory(sourceDir);
        metaModelFromFoo2.addRootTypeName("test.model1.Foo2");
        metaModelFromFoo2.buildMetaModel();
    }

    // =========================================================================
    // Entity count
    // =========================================================================

    /**
     * Starting from Foo1, the upward traversal reaches 2 entities:
     * Foo1 itself and Foo2 (referenced as the type of the "foo2" property).
     */
    @Test
    public void testEntityCountFromFoo1() {
        assertEquals("Starting from Foo1 should discover 2 entities",
                2, metaModelFromFoo1.getEntities().size());
        assertNotNull(metaModelFromFoo1.getEntity("test.model1.Foo1"));
        assertNotNull(metaModelFromFoo1.getEntity("test.model1.Foo2"));
    }

    /**
     * Starting from Foo2 only, the traversal discovers 1 entity:
     * Foo2 alone, because its "name" property type is String (not @ModelEntity).
     */
    @Test
    public void testEntityCountFromFoo2() {
        assertEquals("Starting from Foo2 alone should discover 1 entity",
                1, metaModelFromFoo2.getEntities().size());
        assertNotNull(metaModelFromFoo2.getEntity("test.model1.Foo2"));
    }

    // =========================================================================
    // Package
    // =========================================================================

    /**
     * The full meta-model (rooted at Foo1) must contain the package
     * {@code test.model1} with exactly 2 entities.
     */
    @Test
    public void testPackage() {
        SourcePackage pkg = metaModelFromFoo1.getPackage("test.model1");
        assertNotNull("Package test.model1 should be discovered", pkg);
        assertEquals("test.model1", pkg.getQualifiedName());
        assertFalse("test.model1 is not the default package", pkg.isDefault());
        assertEquals("Package test.model1 should contain 2 entities",
                2, pkg.getEntities().size());
    }

    // =========================================================================
    // Entity identity: Foo1
    // =========================================================================

    /**
     * Foo1 must be discoverable by qualified name, report simpleName "Foo1",
     * and not be abstract (no isAbstract=true on @ModelEntity).
     */
    @Test
    public void testFoo1Exists() {
        SourceModelEntity foo1 = metaModelFromFoo1.getEntity("test.model1.Foo1");
        assertNotNull("Foo1 should be discoverable by qualified name", foo1);
        assertEquals("Foo1", foo1.getSimpleName());
        assertFalse("Foo1 should not be abstract", foo1.isAbstract());
    }

    // =========================================================================
    // Entity identity: Foo2
    // =========================================================================

    /**
     * Foo2 must be discoverable by qualified name, report simpleName "Foo2",
     * and not be abstract.
     */
    @Test
    public void testFoo2Exists() {
        SourceModelEntity foo2 = metaModelFromFoo1.getEntity("test.model1.Foo2");
        assertNotNull("Foo2 should be discoverable by qualified name", foo2);
        assertEquals("Foo2", foo2.getSimpleName());
        assertFalse("Foo2 should not be abstract", foo2.isAbstract());
    }

    // =========================================================================
    // Property: Foo1.foo2
    // =========================================================================

    /**
     * Foo1 declares exactly one property "foo2":
     * cardinality SINGLE, type Foo2, getter + setter present, not embedded,
     * and the type's modelEntity resolves to the Foo2 SourceModelEntity.
     */
    @Test
    public void testFoo1Foo2Property() {
        SourceModelEntity foo1 = metaModelFromFoo1.getEntity("test.model1.Foo1");
        assertNotNull(foo1);

        Map<String, SourceModelProperty> props = foo1.getDeclaredProperties();
        SourceModelProperty foo2Prop = props.get("foo2");
        assertNotNull("Foo1 should declare a 'foo2' property", foo2Prop);

        // Cardinality
        assertEquals("'foo2' should have cardinality SINGLE", Cardinality.SINGLE, foo2Prop.getCardinality());

        // Type qualified name
        assertNotNull("'foo2' property type should not be null", foo2Prop.getType());
        assertEquals("'foo2' type should be test.model1.Foo2",
                "test.model1.Foo2", foo2Prop.getType().getQualifiedName());

        // Getter is present
        assertNotNull("'foo2' should have a getter method name", foo2Prop.getGetterMethodName());

        // Setter is present
        assertNotNull("'foo2' should have a setter method name", foo2Prop.getSetterMethodName());

        // Not embedded (no @Embedded on the getter)
        assertFalse("'foo2' should not be @Embedded", foo2Prop.isEmbedded());

        // The type's modelEntity should have been resolved to the Foo2 entity (Phase 3)
        assertNotNull("'foo2' type.modelEntity should be resolved to Foo2",
                foo2Prop.getType().getModelEntity());
        assertEquals("'foo2' type.modelEntity should be the Foo2 entity",
                "test.model1.Foo2", foo2Prop.getType().getModelEntity().getQualifiedName());
    }

    // =========================================================================
    // Property: Foo2.name
    // =========================================================================

    /**
     * Foo2 declares exactly one property "name":
     * cardinality SINGLE, type java.lang.String (not primitive), getter + setter
     * present, type.modelEntity is null (String is not @ModelEntity).
     */
    @Test
    public void testFoo2NameProperty() {
        SourceModelEntity foo2 = metaModelFromFoo1.getEntity("test.model1.Foo2");
        assertNotNull(foo2);

        Map<String, SourceModelProperty> props = foo2.getDeclaredProperties();
        SourceModelProperty nameProp = props.get("name");
        assertNotNull("Foo2 should declare a 'name' property", nameProp);

        // Cardinality
        assertEquals("'name' should have cardinality SINGLE", Cardinality.SINGLE, nameProp.getCardinality());

        // Type
        assertNotNull("'name' property type should not be null", nameProp.getType());
        assertEquals("'name' type should be java.lang.String",
                "java.lang.String", nameProp.getType().getQualifiedName());

        // String is not a Java primitive
        assertFalse("String is not a primitive type", nameProp.getType().isPrimitive());

        // String is not @ModelEntity, so modelEntity must remain null after Phase 3
        assertNull("'name' type.modelEntity should be null (String is not @ModelEntity)",
                nameProp.getType().getModelEntity());

        // Getter is present
        assertNotNull("'name' should have a getter method name", nameProp.getGetterMethodName());

        // Setter is present
        assertNotNull("'name' should have a setter method name", nameProp.getSetterMethodName());
    }

    // =========================================================================
    // Inheritance: no super-entities
    // =========================================================================

    /**
     * Foo1 extends no @ModelEntity interface, so getDirectSuperEntities() must
     * be empty.
     */
    @Test
    public void testFoo1NoSuperEntities() {
        SourceModelEntity foo1 = metaModelFromFoo1.getEntity("test.model1.Foo1");
        assertNotNull(foo1);
        assertTrue("Foo1 should have no direct super-entities",
                foo1.getDirectSuperEntities().isEmpty());
    }

    /**
     * Foo2 extends no @ModelEntity interface, so getDirectSuperEntities() must
     * be empty.
     */
    @Test
    public void testFoo2NoSuperEntities() {
        SourceModelEntity foo2 = metaModelFromFoo1.getEntity("test.model1.Foo2");
        assertNotNull(foo2);
        assertTrue("Foo2 should have no direct super-entities",
                foo2.getDirectSuperEntities().isEmpty());
    }

    // =========================================================================
    // Implementation class
    // =========================================================================

    /**
     * Foo1 has no @ImplementationClass annotation, so getImplementationClass()
     * must return null.
     */
    @Test
    public void testFoo1NoImplementationClass() {
        SourceModelEntity foo1 = metaModelFromFoo1.getEntity("test.model1.Foo1");
        assertNotNull(foo1);
        assertNull("Foo1 should have no ImplementationClass",
                foo1.getImplementationClass());
    }

    // =========================================================================
    // Initializers
    // =========================================================================

    /**
     * Foo1 declares no @Initializer method, so getInitializers() must be empty.
     */
    @Test
    public void testFoo1NoInitializers() {
        SourceModelEntity foo1 = metaModelFromFoo1.getEntity("test.model1.Foo1");
        assertNotNull(foo1);
        assertTrue("Foo1 should have no initializers",
                foo1.getInitializers().isEmpty());
    }

    // =========================================================================
    // getAllProperties vs getDeclaredProperties (no inheritance)
    // =========================================================================

    /**
     * Because Foo1 has no super-entities, getAllProperties() must return exactly
     * the same set of property keys as getDeclaredProperties().
     */
    @Test
    public void testFoo1GetAllPropertiesEqualsDeclared() {
        SourceModelEntity foo1 = metaModelFromFoo1.getEntity("test.model1.Foo1");
        assertNotNull(foo1);

        Map<String, SourceModelProperty> declared = foo1.getDeclaredProperties();
        Map<String, SourceModelProperty> all = foo1.getAllProperties();

        assertEquals("getAllProperties() should return the same keys as getDeclaredProperties() for Foo1",
                declared.keySet(), all.keySet());
    }

    // =========================================================================
    // CompilationUnit
    // =========================================================================

    /**
     * Foo1's compilation unit must be non-null and its backing file must
     * exist on disk.
     */
    @Test
    public void testFoo1CompilationUnit() {
        SourceModelEntity foo1 = metaModelFromFoo1.getEntity("test.model1.Foo1");
        assertNotNull(foo1);

        SourceCompilationUnit cu = foo1.getCompilationUnit();
        assertNotNull("Foo1 should have a non-null SourceCompilationUnit", cu);
        assertNotNull("SourceCompilationUnit file should not be null", cu.getFile());
        assertTrue("SourceCompilationUnit file should exist on disk",
                cu.getFile().exists());
    }

    // =========================================================================
    // Isolation: Foo1 is not discovered when rooting at Foo2 only
    // =========================================================================

    /**
     * When the meta-model is built from Foo2 as the only root, Foo1 must not
     * appear in the entity map: the traversal does not follow subtypes downward.
     */
    @Test
    public void testFoo1IsNotDiscoveredFromFoo2Root() {
        assertNull("Foo1 should NOT be discovered when the root is Foo2 only",
                metaModelFromFoo2.getEntity("test.model1.Foo1"));
    }

    // =========================================================================
    // Pretty-print: human-readable model dump
    // =========================================================================

    /**
     * Dumps the meta-model rooted at Foo1 to the console in a readable,
     * indented tree-style format.  No assertions — the test passes as long as
     * {@link SourceMetaModel#prettyPrint()} does not throw.
     */
    @Test
    public void testPrettyPrint() {
        System.out.println("\n========== TestModel1 — metaModelFromFoo1 ==========");
        System.out.println(metaModelFromFoo1.prettyPrint());
    }
}
