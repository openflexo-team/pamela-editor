package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;

/**
 * Tests for {@code @Import}/{@code @Imports} support: BFS discovery of entities only
 * reachable via {@code @Imports} (the {@code ConnectorView} shape), the resolved/raw
 * imported-entity lists, the single-element sugar form, and the "not a known
 * @ModelEntity" validation warning. See {@code imports-support-design.md}.
 */
public class TestImportedEntities {

    private static SourceMetaModel model4;

    @BeforeClass
    public static void buildMetaModel() throws IOException {
        File testDir = new File(System.getProperty("user.dir") + "/src/test/java/test");
        model4 = SourceMetaModelSerializer.load(new File(testDir, "model4.pamela"));
    }

    @Test
    public void testConcreteSubtypesOnlyReachableViaImportsAreDiscovered() {
        // Circle/Square are never referenced by any property type (ShapeContainer's only
        // entity-typed property is List<AbstractShape>) — without following @Imports the BFS
        // would never create SourceModelEntitys for them.
        assertNotNull("Circle must be discovered via AbstractShape's @Imports",
                model4.getEntity("test.model4.Circle"));
        assertNotNull("Square must be discovered via AbstractShape's @Imports",
                model4.getEntity("test.model4.Square"));
    }

    @Test
    public void testResolvedImportedEntities() {
        SourceModelEntity abstractShape = model4.getEntity("test.model4.AbstractShape");
        assertNotNull(abstractShape);
        List<SourceModelEntity> imports = abstractShape.getImportedEntities();
        assertEquals(2, imports.size());
        assertEquals("test.model4.Circle", imports.get(0).getQualifiedName());
        assertEquals("test.model4.Square", imports.get(1).getQualifiedName());

        // Raw declared names mirror the resolved list when everything resolves.
        assertEquals(java.util.Arrays.asList("test.model4.Circle", "test.model4.Square"),
                abstractShape.getImportedEntityNames());
    }

    @Test
    public void testSingleElementSugarFormIsParsed() {
        // @Imports(@Import(Circle.class)) — no array braces.
        SourceModelEntity singleImportShape = model4.getEntity("test.model4.SingleImportShape");
        assertNotNull(singleImportShape);
        List<SourceModelEntity> imports = singleImportShape.getImportedEntities();
        assertEquals(1, imports.size());
        assertEquals("test.model4.Circle", imports.get(0).getQualifiedName());
    }

    @Test
    public void testEntityWithoutImportsHasEmptyList() {
        SourceModelEntity shapeContainer = model4.getEntity("test.model4.ShapeContainer");
        assertNotNull(shapeContainer);
        assertTrue(shapeContainer.getImportedEntities().isEmpty());
        assertTrue(shapeContainer.getImportedEntityNames().isEmpty());
    }

    @Test
    public void testBadImportTargetsAreExcludedAndWarned() {
        SourceModelEntity badImportEntity = model4.getEntity("test.model4.BadImportEntity");
        assertNotNull(badImportEntity);

        // Neither NotAnEntity (not @ModelEntity) nor java.lang.String (external/unresolved)
        // resolve to a known entity.
        assertTrue(badImportEntity.getImportedEntities().isEmpty());
        // But the raw declaration is still captured, for diagnostics.
        assertEquals(2, badImportEntity.getImportedEntityNames().size());

        long warnings = model4.getIssues().stream()
                .filter(i -> i.getMessage().contains("does not resolve to a known @ModelEntity"))
                .count();
        assertEquals("expected one warning per bad @Import entry", 2, warnings);

        // Neither NotAnEntity nor String were ever registered as entities.
        assertNull(model4.getEntity("test.model4.NotAnEntity"));
    }
}
