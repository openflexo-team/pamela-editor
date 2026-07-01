package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;

/**
 * Tests for the per-property / per-entity {@link PropertyIdentifierStyle} detection
 * (property-identifier-constant-design.md, Lot 1 — detection).
 *
 * <p>{@code test/model2} is written entirely in the CONSTANT idiom; {@code test/model3} provides a
 * literal entity, a mixed entity, an empty entity and an inconsistent one to exercise the other
 * branches.</p>
 */
public class TestPropertyIdentifierStyle {

    private static SourceMetaModel model2;
    private static SourceMetaModel model3;

    @BeforeClass
    public static void buildMetaModels() throws IOException {
        File testDir = new File(System.getProperty("user.dir") + "/src/test/java/test");
        model2 = SourceMetaModelSerializer.load(new File(testDir, "model2-full.pamela"));
        model3 = SourceMetaModelSerializer.load(new File(testDir, "model3.pamela"));
    }

    // --- model2: pure CONSTANT idiom -----------------------------------------

    @Test
    public void testModel2EntityDetectedAsConstant() {
        SourceModelEntity abstractNode = model2.getEntity("test.model2.AbstractNode");
        assertEquals(PropertyIdentifierStyle.CONSTANT, abstractNode.getIdentifierStyle());
        assertEquals(PropertyIdentifierStyle.CONSTANT, abstractNode.effectiveIdentifierStyle());
    }

    @Test
    public void testModel2PropertyReferencedConstantName() {
        SourceModelProperty outgoing = model2.getEntity("test.model2.AbstractNode")
                .getDeclaredProperties().get("outgoingEdges");
        assertEquals(PropertyIdentifierStyle.CONSTANT, outgoing.getIdentifierStyle());
        assertEquals("OUTGOING_EDGES", outgoing.getReferencedConstantName());
    }

    // --- model3: LITERAL / MIXED / UNDETERMINED ------------------------------

    @Test
    public void testLiteralEntity() {
        SourceModelEntity literal = model3.getEntity("test.model3.LiteralEntity");
        assertEquals(PropertyIdentifierStyle.LITERAL, literal.getIdentifierStyle());
        assertEquals(PropertyIdentifierStyle.LITERAL, literal.effectiveIdentifierStyle());

        SourceModelProperty name = literal.getDeclaredProperties().get("name");
        assertEquals(PropertyIdentifierStyle.LITERAL, name.getIdentifierStyle());
        assertNull(name.getReferencedConstantName());
    }

    @Test
    public void testMixedEntityFollowsDominant() {
        SourceModelEntity mixed = model3.getEntity("test.model3.MixedEntity");
        assertEquals(PropertyIdentifierStyle.MIXED, mixed.getIdentifierStyle());
        // 1 literal (name) + 1 constant (age) → tie → CONSTANT
        assertEquals(PropertyIdentifierStyle.CONSTANT, mixed.effectiveIdentifierStyle());

        assertEquals(PropertyIdentifierStyle.LITERAL,
                mixed.getDeclaredProperties().get("name").getIdentifierStyle());
        assertEquals(PropertyIdentifierStyle.CONSTANT,
                mixed.getDeclaredProperties().get("age").getIdentifierStyle());
        assertEquals("AGE",
                mixed.getDeclaredProperties().get("age").getReferencedConstantName());
    }

    @Test
    public void testEmptyEntityIsUndeterminedAndFallsBackToPreference() {
        SourceModelEntity empty = model3.getEntity("test.model3.EmptyEntity");
        assertEquals(PropertyIdentifierStyle.UNDETERMINED, empty.getIdentifierStyle());
        // No declared property → effective style is the meta-model default (CONSTANT by default).
        assertEquals(PropertyIdentifierStyle.CONSTANT, empty.effectiveIdentifierStyle());

        model2.setDefaultPropertyIdentifierStyle(PropertyIdentifierStyle.LITERAL);
        try {
            model3.setDefaultPropertyIdentifierStyle(PropertyIdentifierStyle.LITERAL);
            assertEquals(PropertyIdentifierStyle.LITERAL, empty.effectiveIdentifierStyle());
        } finally {
            model3.setDefaultPropertyIdentifierStyle(PropertyIdentifierStyle.CONSTANT);
        }
    }

    // --- consistency warning -------------------------------------------------

    @Test
    public void testInconsistentAccessorsFireWarning() {
        SourceModelEntity inconsistent = model3.getEntity("test.model3.InconsistentEntity");
        // Getter is authoritative: a constant getter → CONSTANT, despite the literal setter.
        assertEquals(PropertyIdentifierStyle.CONSTANT, inconsistent.getIdentifierStyle());

        boolean warned = model3.getIssues().stream()
                .anyMatch(i -> i.getMessage().contains("mixes constant and literal identifiers"));
        assertTrue("expected a mixed-identifier-style warning", warned);
    }
}
