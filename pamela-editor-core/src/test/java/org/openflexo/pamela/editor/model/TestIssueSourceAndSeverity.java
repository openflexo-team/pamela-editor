package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;

/**
 * Tests for {@link Issue}'s optional {@code source} back-reference and the per-severity counters
 * on {@link SourceMetaModel} (validation-log-panel-design.md).
 */
public class TestIssueSourceAndSeverity {

    private static SourceMetaModel model3;

    @BeforeClass
    public static void buildMetaModel() throws IOException {
        File testDir = new File(System.getProperty("user.dir") + "/src/test/java/test");
        model3 = SourceMetaModelSerializer.load(new File(testDir, "model3.pamela"));
    }

    @Test
    public void testIssueDefaultSourceIsNull() {
        Issue issue = new Warning("no source given");
        assertNull(issue.getSource());
    }

    @Test
    public void testIssueSourceIsRetained() {
        SourceModelEntity entity = model3.getEntity("test.model3.InconsistentEntity");
        Issue issue = new Error("with a source", entity);
        assertSame(entity, issue.getSource());
    }

    @Test
    public void testInformationIsConstructible() {
        Information info = new Information("just a notice");
        assertEquals("just a notice", info.getMessage());
        assertNull(info.getSource());

        SourceModelEntity entity = model3.getEntity("test.model3.InconsistentEntity");
        Information infoWithSource = new Information("just a notice", entity);
        assertSame(entity, infoWithSource.getSource());
    }

    @Test
    public void testConsistencyWarningCarriesItsPropertyAsSource() {
        SourceModelEntity inconsistent = model3.getEntity("test.model3.InconsistentEntity");
        SourceModelProperty label = inconsistent.getDeclaredProperties().get("label");

        Issue warning = model3.getIssues().stream()
                .filter(i -> i.getMessage().contains("mixes constant and literal identifiers"))
                .findFirst()
                .orElse(null);
        assertNotNull("expected the mixed-identifier-style warning to be present", warning);
        assertSame(label, warning.getSource());
    }

    @Test
    public void testSeverityCountsPartitionIssuesCount() {
        int errors = model3.getErrorsCount();
        int warnings = model3.getWarningsCount();
        int infos = model3.getInformationCount();
        assertEquals(model3.getIssuesCount(), errors + warnings + infos);
        assertTrue("expected at least one warning in model3", warnings >= 1);
    }

    // -------------------------------------------------------------------------
    // hasErrors / hasWarnings — per-element severity lookup (icon-decoration support,
    // context-menu-design.md / PamelaEditorIconLibrary#decorateWithSeverity)
    // -------------------------------------------------------------------------

    @Test
    public void testHasWarningsTrueForElementWithAttachedWarning() {
        SourceModelEntity inconsistent = model3.getEntity("test.model3.InconsistentEntity");
        SourceModelProperty label = inconsistent.getDeclaredProperties().get("label");

        assertTrue(model3.hasWarnings(label));
        assertFalse(model3.hasErrors(label));
    }

    @Test
    public void testHasErrorsAndWarningsFalseForCleanElement() {
        // Every entity in model3 carries its own "no @Initializer" Warning, so a property is
        // used here instead — LiteralEntity's own "name" property carries no issue.
        SourceModelEntity literalEntity = model3.getEntity("test.model3.LiteralEntity");
        SourceModelProperty clean = literalEntity.getDeclaredProperties().get("name");
        assertFalse(model3.hasErrors(clean));
        assertFalse(model3.hasWarnings(clean));
    }

    @Test
    public void testHasErrorsAndWarningsAreNullSafe() {
        assertFalse(model3.hasErrors(null));
        assertFalse(model3.hasWarnings(null));
    }

    @Test
    public void testSourceElementResolvesItsOwningMetaModel() {
        SourceModelEntity inconsistent = model3.getEntity("test.model3.InconsistentEntity");
        SourceModelProperty label = inconsistent.getDeclaredProperties().get("label");

        assertSame(model3, inconsistent.getMetaModel());
        assertSame(model3, label.getMetaModel());
        assertSame(model3, model3.getMetaModel());
    }

    // -------------------------------------------------------------------------
    // Roll-up through the containment hierarchy — a container (package, source folder,
    // metamodel) reports an issue raised against a nested element, even though it carries
    // no issue of its own.
    // -------------------------------------------------------------------------

    @Test
    public void testPackageAggregatesWarningFromNestedProperty() {
        // SourcePackage is never itself a direct Issue source in this codebase, so this
        // isolates pure roll-up from InconsistentEntity.label's consistency warning.
        SourceModelEntity inconsistent = model3.getEntity("test.model3.InconsistentEntity");
        SourcePackage pkg = inconsistent.getSourcePackage();

        assertFalse("the package itself carries no direct issue",
                model3.getIssues().stream().anyMatch(i -> i.getSource() == pkg));
        assertTrue(model3.hasWarnings(pkg));
        assertFalse(model3.hasErrors(pkg));
    }

    @Test
    public void testSourceFolderAggregatesWarningFromNestedEntity() {
        // SourceFolder is never itself a direct Issue source either — same isolation as above,
        // one level further up (folder -> package -> entity -> property).
        assertFalse("no source folder is itself a direct issue source",
                model3.getIssues().stream().anyMatch(i -> i.getSource() instanceof SourceFolder));

        boolean anyFolderAggregatesWarning = model3.getSourceFolders().stream()
                .anyMatch(model3::hasWarnings);
        assertTrue("expected at least one source folder to roll up a nested warning",
                anyFolderAggregatesWarning);
    }

    @Test
    public void testMetaModelAggregatesWarningsWithNoDirectIssueOfItsOwn() {
        assertFalse("the metamodel itself carries no direct issue in model3",
                model3.getIssues().stream().anyMatch(i -> i.getSource() == model3));
        assertTrue(model3.hasWarnings(model3));
        assertFalse("model3 has no Error anywhere", model3.hasErrors(model3));
    }

    // -------------------------------------------------------------------------
    // issuesRegarding(element) — the actual Issue list behind the browser hover tooltips
    // (same roll-up semantics as hasErrors/hasWarnings, ordered by severity).
    // -------------------------------------------------------------------------

    @Test
    public void testIssuesRegardingLeafReturnsOnlyItsOwnIssue() {
        SourceModelEntity inconsistent = model3.getEntity("test.model3.InconsistentEntity");
        SourceModelProperty label = inconsistent.getDeclaredProperties().get("label");

        java.util.List<Issue> issues = model3.issuesRegarding(label);
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).getMessage().contains("mixes constant and literal identifiers"));
        assertSame(label, issues.get(0).getSource());
    }

    @Test
    public void testIssuesRegardingEntityRollsUpNestedPropertyIssue() {
        SourceModelEntity inconsistent = model3.getEntity("test.model3.InconsistentEntity");
        SourceModelProperty label = inconsistent.getDeclaredProperties().get("label");

        java.util.List<Issue> issues = model3.issuesRegarding(inconsistent);
        // The entity's own "no @Initializer" warning AND the nested property's warning.
        assertTrue("expected the entity's own issue to be listed",
                issues.stream().anyMatch(i -> i.getSource() == inconsistent));
        assertTrue("expected the nested property's issue to roll up",
                issues.stream().anyMatch(i -> i.getSource() == label));
    }

    @Test
    public void testIssuesRegardingIsOrderedBySeverity() {
        // Errors before warnings before information — verify the rank sequence never decreases.
        java.util.List<Issue> issues = model3.issuesRegarding(model3);
        assertFalse("model3 has issues to order", issues.isEmpty());
        int previousRank = -1;
        for (Issue issue : issues) {
            int rank = issue instanceof Error ? 0 : issue instanceof Warning ? 1 : 2;
            assertTrue("issues must be ordered by decreasing severity", rank >= previousRank);
            previousRank = rank;
        }
    }

    @Test
    public void testIssuesRegardingCleanLeafIsEmpty() {
        SourceModelEntity literalEntity = model3.getEntity("test.model3.LiteralEntity");
        SourceModelProperty clean = literalEntity.getDeclaredProperties().get("name");
        assertTrue(model3.issuesRegarding(clean).isEmpty());
    }

    @Test
    public void testIssuesRegardingNullIsEmpty() {
        assertTrue(model3.issuesRegarding(null).isEmpty());
    }

    @Test
    public void testRollUpIsOneDirectionalChildToParentOnly() {
        // MixedEntity itself carries its own "no @Initializer" warning, but its two
        // properties ("name", "age") carry no issue at all — the roll-up must not leak
        // the other way (parent's own issue must not make its clean children look flagged).
        SourceModelEntity mixed = model3.getEntity("test.model3.MixedEntity");
        SourceModelProperty name = mixed.getDeclaredProperties().get("name");
        SourceModelProperty age = mixed.getDeclaredProperties().get("age");

        assertTrue("the entity itself has its own direct warning", model3.hasWarnings(mixed));
        assertFalse(model3.hasWarnings(name));
        assertFalse(model3.hasWarnings(age));
        assertFalse(model3.hasErrors(mixed));
    }
}
