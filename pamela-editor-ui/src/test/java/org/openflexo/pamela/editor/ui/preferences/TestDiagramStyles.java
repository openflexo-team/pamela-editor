package org.openflexo.pamela.editor.ui.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramSerializer;
import org.openflexo.pamela.editor.ui.PamelaProject;

/**
 * Step A of the per-diagram styles feature ({@code preferences-design.md §6bis}): a diagram
 * snapshots the preference defaults at creation, and embeds them in its {@code .diagram} file.
 */
public class TestDiagramStyles {

    private File prefsFile;

    @Before
    public void setUp() throws Exception {
        prefsFile = File.createTempFile("prefs-diag", ".json");
        prefsFile.delete();
        prefsFile.deleteOnExit();
        System.setProperty(PreferencesManager.PREFERENCES_FILE_PROPERTY, prefsFile.getAbsolutePath());
        PreferencesRegistry.resetForTests();
        PreferencesManager.resetForTests();
        PreferencesManager.initialize(Function.identity());
    }

    @After
    public void tearDown() {
        PreferencesManager.resetForTests();
        System.clearProperty(PreferencesManager.PREFERENCES_FILE_PROPERTY);
    }

    private PamelaProject newProject() throws Exception {
        File pamelaFile = File.createTempFile("proj", ".pamela");
        pamelaFile.deleteOnExit();
        return new PamelaProject(pamelaFile, null);
    }

    @Test
    public void testSnapshotAtCreation() throws Exception {
        PamelaProject project = newProject();
        PamelaClassDiagram diagram = project.getDiagramFactory().newDiagram("d");

        assertNotNull("entity style block snapshotted", diagram.getEntityStyle());
        assertNotNull(diagram.getEntityStyle().getHeaderBackgroundColor());
        // The two default connector styles are captured as used.
        assertEquals("inheritance-line", diagram.getDefaultInheritanceStyleId());
        assertEquals("relationship-rect-polylin", diagram.getDefaultAssociationStyleId());
        assertNotNull(diagram.getConnectorStyleById("inheritance-line"));
        assertNotNull(diagram.getConnectorStyleById("relationship-rect-polylin"));
    }

    @Test
    public void testDiagramStylesRoundTrip() throws Exception {
        PamelaProject project = newProject();
        PamelaClassDiagram diagram = project.getDiagramFactory().newDiagram("d");
        diagram.setId("d");

        diagram.getEntityStyle().setHeaderBackgroundColor(new Color(7, 8, 9));
        ConnectorStylePreference inh = diagram.getConnectorStyleById("inheritance-line");
        inh.setColor(new Color(1, 2, 3));
        inh.setLineWidth(3.0);

        File diagramFile = File.createTempFile("diag", ".diagram");
        diagramFile.deleteOnExit();
        PamelaClassDiagramSerializer.save(diagram, diagramFile);

        PamelaClassDiagram reloaded = PamelaClassDiagramSerializer.load(diagramFile, project);
        assertEquals(new Color(7, 8, 9), reloaded.getEntityStyle().getHeaderBackgroundColor());
        ConnectorStylePreference inh2 = reloaded.getConnectorStyleById("inheritance-line");
        assertNotNull(inh2);
        assertEquals(new Color(1, 2, 3), inh2.getColor());
        assertTrue(Math.abs(inh2.getLineWidth() - 3.0) < 1e-9);
        assertEquals("inheritance-line", reloaded.getDefaultInheritanceStyleId());
    }
}
