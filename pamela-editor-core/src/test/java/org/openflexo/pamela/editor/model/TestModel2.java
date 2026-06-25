package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;

/**
 * Tests for the {@link SourceMetaModel} layer using the {@code test/model2/} test model.
 *
 * <p>The model2 entity hierarchy (all in package {@code test.model2}):
 * <pre>
 *   TestModelObject (abstract, @ImplementationClass(FlexoModelObjectImpl))
 *     WKFObject (abstract)
 *       FlexoProcess (@ImplementationClass(FlexoProcessImpl))
 *       AbstractNode (abstract)
 *         ActivityNode
 *         EventNode (abstract)
 *           StartNode
 *           EndNode
 *         MyNode
 *       Edge (abstract, @ImplementationClass(EdgeImpl))
 *         TokenEdge (@ImplementationClass(TokenEdgeImpl))
 *     WKFAnnotation
 * </pre>
 *
 * <p>The bottom-up BFS traversal follows <em>super-interfaces</em> and
 * {@code @Getter} property types upward — it does <em>not</em> traverse
 * downward to subtypes. Starting from {@code FlexoProcess} alone therefore
 * reaches 6 entities: FlexoProcess, WKFObject, TestModelObject, AbstractNode,
 * Edge, WKFAnnotation.</p>
 *
 * <p>To cover leaf entities like {@code ActivityNode}, {@code StartNode},
 * {@code TokenEdge} etc., we provide them as additional root types.</p>
 */
public class TestModel2 {

    /**
     * Meta-model built from {@code FlexoProcess} as the sole root type.
     * Only 6 entities are reachable via the upward traversal.
     */
    private static SourceMetaModel metaModelFromFlexoProcess;

    /**
     * Meta-model built from all leaf entities as roots, so that all 12
     * {@code @ModelEntity} interfaces are discovered.
     */
    private static SourceMetaModel metaModelFull;

    @BeforeClass
    public static void buildMetaModels() throws IOException {
        File testDir = new File(System.getProperty("user.dir") + "/src/test/java/test");

        metaModelFromFlexoProcess = SourceMetaModelSerializer.load(
                new File(testDir, "model2-from-flexoprocess.pamela"));

        metaModelFull = SourceMetaModelSerializer.load(
                new File(testDir, "model2-full.pamela"));
    }

    // =========================================================================
    // Entity count — FlexoProcess-only traversal
    // =========================================================================

    /**
     * Starting from FlexoProcess alone, the upward traversal reaches 6 entities:
     * FlexoProcess, WKFObject, TestModelObject, AbstractNode, Edge, WKFAnnotation.
     * Leaf entities (ActivityNode, StartNode, etc.) are subtypes, not reachable
     * by following super-interfaces or @Getter property types upward.
     */
    @Test
    public void testEntityCountFromFlexoProcessOnly() {
        assertEquals(6, metaModelFromFlexoProcess.getEntities().size());

        // The 6 expected entities
        assertNotNull(metaModelFromFlexoProcess.getEntity("test.model2.FlexoProcess"));
        assertNotNull(metaModelFromFlexoProcess.getEntity("test.model2.WKFObject"));
        assertNotNull(metaModelFromFlexoProcess.getEntity("test.model2.TestModelObject"));
        assertNotNull(metaModelFromFlexoProcess.getEntity("test.model2.AbstractNode"));
        assertNotNull(metaModelFromFlexoProcess.getEntity("test.model2.Edge"));
        assertNotNull(metaModelFromFlexoProcess.getEntity("test.model2.WKFAnnotation"));
    }

    /**
     * With all leaf entities added as roots, all 12 @ModelEntity interfaces
     * in the model2 directory are discovered.
     */
    @Test
    public void testEntityCountFull() {
        Map<String, SourceModelEntity> entities = metaModelFull.getEntities();
        assertEquals(12, entities.size());
    }

    // =========================================================================
    // Package
    // =========================================================================

    @Test
    public void testPackageFull() {
        SourcePackage pkg = metaModelFull.getPackage("test.model2");
        assertNotNull("Package test.model2 should be discovered", pkg);
        assertEquals("test.model2", pkg.getQualifiedName());
        assertFalse(pkg.isDefault());
        assertEquals(12, pkg.getEntities().size());
    }

    // =========================================================================
    // AbstractNode: abstract flag
    // =========================================================================

    @Test
    public void testAbstractNodeIsAbstract() {
        SourceModelEntity abstractNode = metaModelFromFlexoProcess.getEntity("test.model2.AbstractNode");
        assertNotNull(abstractNode);
        assertTrue("AbstractNode should be abstract", abstractNode.isAbstract());
    }

    // =========================================================================
    // Inheritance: ActivityNode -> AbstractNode  (requires full meta-model)
    // =========================================================================

    @Test
    public void testActivityNodeDirectSuperEntity() {
        SourceModelEntity activityNode = metaModelFull.getEntity("test.model2.ActivityNode");
        assertNotNull("ActivityNode should be discovered in full meta-model", activityNode);
        assertEquals("ActivityNode", activityNode.getSimpleName());
        assertFalse("ActivityNode should not be abstract", activityNode.isAbstract());

        List<SourceModelEntity> supers = activityNode.getDirectSuperEntities();
        assertEquals("ActivityNode should have exactly 1 direct super entity", 1, supers.size());
        assertEquals("test.model2.AbstractNode", supers.get(0).getQualifiedName());
    }

    // =========================================================================
    // LIST property on FlexoProcess: "nodes"
    // =========================================================================

    @Test
    public void testFlexoProcessNodesProperty() {
        SourceModelEntity flexoProcess = metaModelFromFlexoProcess.getEntity("test.model2.FlexoProcess");
        assertNotNull("FlexoProcess should be discovered", flexoProcess);

        // getDeclaredProperties() returns only properties declared on FlexoProcess itself
        Map<String, SourceModelProperty> props = flexoProcess.getDeclaredProperties();
        SourceModelProperty nodesProp = props.get("nodes");
        assertNotNull("FlexoProcess should declare a 'nodes' property", nodesProp);

        // Cardinality
        assertEquals("'nodes' should have cardinality LIST", Cardinality.LIST, nodesProp.getCardinality());

        // Type: AbstractNode
        assertNotNull("'nodes' property type should not be null", nodesProp.getType());
        assertEquals("test.model2.AbstractNode", nodesProp.getType().getQualifiedName());

        // Adder and Remover are both present
        assertNotNull("'nodes' should have an adder", nodesProp.getAdderMethodName());
        assertNotNull("'nodes' should have a remover", nodesProp.getRemoverMethodName());

        // Embedded
        assertTrue("'nodes' should be @Embedded", nodesProp.isEmbedded());
    }

    @Test
    public void testFlexoProcessFooProperty() {
        SourceModelEntity flexoProcess = metaModelFromFlexoProcess.getEntity("test.model2.FlexoProcess");
        assertNotNull(flexoProcess);

        SourceModelProperty fooProp = flexoProcess.getDeclaredProperties().get("foo");
        assertNotNull("FlexoProcess should declare a 'foo' property", fooProp);
        assertEquals(Cardinality.SINGLE, fooProp.getCardinality());
        assertEquals("int", fooProp.getType().getSimpleName());
        assertTrue("'foo' type should be primitive", fooProp.getType().isPrimitive());
        assertNotNull("'foo' should have a setter", fooProp.getSetterMethodName());
        assertEquals("4", fooProp.getDefaultValue());
    }

    // =========================================================================
    // getAllProperties(): inherited properties visible on ActivityNode
    // =========================================================================

    @Test
    public void testActivityNodeGetAllProperties() {
        SourceModelEntity activityNode = metaModelFull.getEntity("test.model2.ActivityNode");
        assertNotNull("ActivityNode should be in full meta-model", activityNode);

        Map<String, SourceModelProperty> all = activityNode.getAllProperties();
        // ActivityNode declares nothing itself; all come from AbstractNode and its parents.
        // AbstractNode declares: outgoingEdges, incomingEdges, process(override), masterAnnotation, otherAnnotations
        // WKFObject declares: process
        // TestModelObject declares: flexoId, name
        assertTrue("'outgoingEdges' should be inherited by ActivityNode",
                all.containsKey("outgoingEdges"));
        assertTrue("'name' should be inherited by ActivityNode",
                all.containsKey("name"));
        assertTrue("'flexoId' should be inherited by ActivityNode",
                all.containsKey("flexoId"));
    }

    // =========================================================================
    // SourceImplementationClass for FlexoProcess
    // =========================================================================

    @Test
    public void testFlexoProcessImplementationClass() {
        SourceModelEntity flexoProcess = metaModelFromFlexoProcess.getEntity("test.model2.FlexoProcess");
        assertNotNull(flexoProcess);

        SourceImplementationClass implClass = flexoProcess.getImplementationClass();
        assertNotNull("FlexoProcess should have an ImplementationClass", implClass);
        assertEquals("test.model2.FlexoProcessImpl", implClass.getQualifiedName());
        assertEquals("FlexoProcessImpl", implClass.getSimpleName());
        assertTrue("FlexoProcessImpl should be abstract", implClass.isAbstract());
    }

    // =========================================================================
    // Custom methods (operations) — sourced from the interface, filter-driven
    // =========================================================================

    /**
     * Under the default {@code PAMELA_ANNOTATED} filter, FlexoProcess surfaces only its two
     * {@code @Finder} operations (getNodeNamed, getNodesNamed). The unannotated interface
     * method getEdgeNamed is hidden, and the impl-only {@code toString} is never a candidate
     * (it is not declared on the interface). See {@code custom-method-design.md}.
     */
    @Test
    public void testFlexoProcessOperationsDefaultFilter() {
        SourceModelEntity flexoProcess = metaModelFromFlexoProcess.getEntity("test.model2.FlexoProcess");
        java.util.Set<String> names = new java.util.HashSet<>();
        for (SourceCustomMethod m : flexoProcess.getDeclaredCustomMethods()) {
            names.add(m.getMethodName());
        }
        assertTrue("getNodeNamed (@Finder) should be an operation", names.contains("getNodeNamed"));
        assertTrue("getNodesNamed (@Finder) should be an operation", names.contains("getNodesNamed"));
        assertFalse("getEdgeNamed (unannotated) must be hidden under PAMELA_ANNOTATED",
                names.contains("getEdgeNamed"));
        assertFalse("toString must never be a candidate (impl-only, not on interface)",
                names.contains("toString"));

        for (SourceCustomMethod m : flexoProcess.getDeclaredCustomMethods()) {
            if (m.getMethodName().startsWith("getNode")) {
                assertEquals(SourceCustomMethod.Kind.FINDER, m.getKind());
            }
        }
    }

    /**
     * Under {@code ALL_INTERFACE_METHODS}, the unannotated interface method getEdgeNamed is
     * also surfaced, but the impl-only toString still is not (it is not on the interface).
     */
    @Test
    public void testFlexoProcessOperationsAllInterfaceMethods() throws IOException {
        File testDir = new File(System.getProperty("user.dir") + "/src/test/java/test");
        SourceMetaModel mm = SourceMetaModelSerializer.load(
                new File(testDir, "model2-from-flexoprocess.pamela"), null,
                CustomMethodFilter.ALL_INTERFACE_METHODS);
        SourceModelEntity flexoProcess = mm.getEntity("test.model2.FlexoProcess");
        java.util.Set<String> names = new java.util.HashSet<>();
        for (SourceCustomMethod m : flexoProcess.getDeclaredCustomMethods()) {
            names.add(m.getMethodName());
        }
        assertTrue("getEdgeNamed should be visible under ALL_INTERFACE_METHODS",
                names.contains("getEdgeNamed"));
        assertTrue("getNodeNamed should still be visible", names.contains("getNodeNamed"));
        assertFalse("toString is impl-only and never a candidate", names.contains("toString"));
    }

    /**
     * Edge: {@code toString} is overridden in EdgeImpl only (not declared on the Edge
     * interface), so it must not appear as an operation under any filter.
     */
    @Test
    public void testEdgeHasNoToStringOperation() {
        SourceModelEntity edge = metaModelFromFlexoProcess.getEntity("test.model2.Edge");
        for (SourceCustomMethod m : edge.getDeclaredCustomMethods()) {
            assertFalse("Edge must not surface toString as an operation",
                    "toString".equals(m.getMethodName()));
        }
    }

    @Test
    public void testTestModelObjectImplementationClass() {
        SourceModelEntity tmo = metaModelFromFlexoProcess.getEntity("test.model2.TestModelObject");
        assertNotNull(tmo);
        // TestModelObject has @ImplementationClass(FlexoModelObjectImpl.class)
        SourceImplementationClass impl = tmo.getImplementationClass();
        assertNotNull("TestModelObject should have an ImplementationClass", impl);
        assertEquals("test.model2.FlexoModelObjectImpl", impl.getQualifiedName());
    }

    // =========================================================================
    // Edge: inverse properties
    // =========================================================================

    @Test
    public void testEdgeStartNodeProperty() {
        SourceModelEntity edge = metaModelFromFlexoProcess.getEntity("test.model2.Edge");
        assertNotNull(edge);

        SourceModelProperty startNode = edge.getDeclaredProperties().get("startNode");
        assertNotNull("Edge should declare 'startNode'", startNode);
        assertEquals(Cardinality.SINGLE, startNode.getCardinality());
        assertEquals("test.model2.AbstractNode", startNode.getType().getQualifiedName());
        // After Phase 3, inversePropertyIdentifier should be "outgoingEdges"
        assertEquals("outgoingEdges", startNode.getInversePropertyIdentifier());
        // And the resolved inverse should point to AbstractNode.outgoingEdges
        assertNotNull("inverseProperty should be resolved", startNode.getInverseProperty());
        assertEquals("outgoingEdges", startNode.getInverseProperty().getPropertyIdentifier());
    }

    // =========================================================================
    // WKFAnnotation: no implementation class
    // =========================================================================

    @Test
    public void testWKFAnnotationNoImplementationClass() {
        SourceModelEntity wkfAnnotation = metaModelFromFlexoProcess.getEntity("test.model2.WKFAnnotation");
        assertNotNull(wkfAnnotation);
        assertNull("WKFAnnotation should have no ImplementationClass",
                wkfAnnotation.getImplementationClass());
    }

    // =========================================================================
    // XMLTag
    // =========================================================================

    @Test
    public void testXmlTagOnFlexoProcess() {
        SourceModelEntity flexoProcess = metaModelFromFlexoProcess.getEntity("test.model2.FlexoProcess");
        assertNotNull(flexoProcess);
        assertEquals("FlexoProcess", flexoProcess.getXmlTag());
    }

    @Test
    public void testXmlTagOnActivityNode() {
        SourceModelEntity activityNode = metaModelFull.getEntity("test.model2.ActivityNode");
        assertNotNull("ActivityNode should be in full meta-model", activityNode);
        assertEquals("ActivityNode", activityNode.getXmlTag());
    }

    // =========================================================================
    // TokenEdge: implementation class
    // =========================================================================

    @Test
    public void testTokenEdgeImplementationClass() {
        SourceModelEntity tokenEdge = metaModelFull.getEntity("test.model2.TokenEdge");
        assertNotNull("TokenEdge should be in full meta-model", tokenEdge);

        SourceImplementationClass impl = tokenEdge.getImplementationClass();
        assertNotNull("TokenEdge should have @ImplementationClass", impl);
        assertEquals("test.model2.TokenEdgeImpl", impl.getQualifiedName());
    }

    // =========================================================================
    // EventNode: abstract, no properties
    // =========================================================================

    @Test
    public void testEventNodeIsAbstract() {
        SourceModelEntity eventNode = metaModelFull.getEntity("test.model2.EventNode");
        assertNotNull("EventNode should be in full meta-model", eventNode);
        assertTrue("EventNode should be abstract", eventNode.isAbstract());
        assertTrue("EventNode should declare no properties",
                eventNode.getDeclaredProperties().isEmpty());
    }

    // =========================================================================
    // Pretty-print: human-readable model dumps
    // =========================================================================

    /**
     * Dumps both meta-models to the console in readable, indented tree-style
     * format.  No assertions — the tests pass as long as
     * {@link SourceMetaModel#prettyPrint()} does not throw.
     */
    @Test
    public void testPrettyPrintFromFlexoProcess() {
        System.out.println("\n========== TestModel2 — metaModelFromFlexoProcess ==========");
        System.out.println(metaModelFromFlexoProcess.prettyPrint());
    }

    @Test
    public void testPrettyPrintFull() {
        System.out.println("\n========== TestModel2 — metaModelFull ==========");
        System.out.println(metaModelFull.prettyPrint());
    }
}
