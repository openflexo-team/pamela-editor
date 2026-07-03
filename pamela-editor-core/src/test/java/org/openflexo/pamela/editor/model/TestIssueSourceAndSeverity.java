package org.openflexo.pamela.editor.model;

import static org.junit.Assert.assertEquals;
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
}
