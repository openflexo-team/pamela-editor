package org.openflexo.pamela.editor.ui.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.io.File;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

/**
 * Round-trip tests for the preferences core (model + factory + registry + serializer).
 * Pure model layer — no Swing / Gina.
 */
public class TestPreferences {

    private PreferencesFactory factory;

    @Before
    public void setUp() throws Exception {
        PreferencesRegistry.resetForTests();
        PreferencesRegistry.registerBuiltinThemes();
        factory = new PreferencesFactory();
    }

    private PamelaEditorPreferencesModel buildTree() {
        return PreferencesRegistry.buildTree(factory, Function.identity());
    }

    @Test
    public void testTreeStructure() {
        PamelaEditorPreferencesModel root = buildTree();
        assertTrue(root.isRoot());

        PreferencesNode general = root.getChild("general");
        assertNotNull(general);
        assertTrue(general instanceof GeneralPreferences);
        assertEquals("/general", general.getPath());

        assertNotNull(general.getChild("window"));
        assertTrue(general.getChild("window") instanceof WindowPreferences);
        assertEquals("/general/window", general.getChild("window").getPath());
        assertNotNull(general.getChild("recent"));
        assertTrue(general.getChild("recent") instanceof RecentFilesPreferences);

        PreferencesNode design = root.getChild("classDiagramDesign");
        assertTrue(design instanceof ClassDiagramDesignPreferences);
        assertTrue(design.getChild("entities") instanceof EntityStylePreferences);
        assertEquals("/classDiagramDesign/entities", design.getChild("entities").getPath());
        assertTrue(root.getChild("analysis") instanceof AnalysisPreferences);

        PreferencesNode generation = root.getChild("generation");
        assertTrue(generation instanceof GenerationPreferences);
        assertTrue(generation.getChild("style") instanceof SourceStylePreferences);
        assertEquals("/generation/style", generation.getChild("style").getPath());
    }

    @Test
    public void testDefaults() {
        PamelaEditorPreferencesModel root = buildTree();
        GeneralPreferences general = (GeneralPreferences) root.getChild("general");
        assertEquals("English", general.getLanguage());
        assertTrue(general.getConfirmOnDelete());

        RecentFilesPreferences recent = (RecentFilesPreferences) general.getChild("recent");
        assertEquals(10, recent.getMaxCount());
        assertTrue(recent.getFiles().isEmpty());

        AnalysisPreferences analysis = (AnalysisPreferences) root.getChild("analysis");
        assertTrue(analysis.getBuildCacheEnabled());

        SourceStylePreferences style = (SourceStylePreferences) root.getChild("generation").getChild("style");
        assertTrue(style.getUseTabulations());
        assertEquals(4, style.getTabulationSize());
        assertTrue(style.getBlankLineAfterPackage());
        assertTrue(style.getBlankLineAfterImports());
        assertTrue(style.getExpandEmptyBody());
    }

    @Test
    public void testRoundTrip() throws Exception {
        PamelaEditorPreferencesModel root = buildTree();
        GeneralPreferences general = (GeneralPreferences) root.getChild("general");
        WindowPreferences window = (WindowPreferences) general.getChild("window");
        RecentFilesPreferences recent = (RecentFilesPreferences) general.getChild("recent");
        AnalysisPreferences analysis = (AnalysisPreferences) root.getChild("analysis");

        general.setLanguage("French");
        general.setConfirmOnDelete(false);
        window.setFrameBounds(new Rectangle(10, 20, 800, 600));
        recent.setMaxCount(5);
        recent.addToFiles(new File("/tmp/A.pamela"));
        recent.addToFiles(new File("/tmp/B.pamela"));
        recent.setLastDirectory(new File("/tmp"));
        analysis.setBuildCacheEnabled(false);

        ClassDiagramDesignPreferences design = (ClassDiagramDesignPreferences) root.getChild("classDiagramDesign");
        EntityStylePreferences entities = (EntityStylePreferences) design.getChild("entities");
        entities.setHeaderBackgroundColor(new java.awt.Color(12, 34, 56));
        entities.setTitleFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 14));

        File tmp = File.createTempFile("prefs", ".json");
        tmp.deleteOnExit();
        PreferencesSerializer serializer = new PreferencesSerializer(factory);
        serializer.save(root, tmp);

        // Fresh tree (defaults), then overlay the saved JSON.
        PamelaEditorPreferencesModel reloaded = buildTree();
        serializer.applyJson(reloaded, tmp);

        GeneralPreferences general2 = (GeneralPreferences) reloaded.getChild("general");
        WindowPreferences window2 = (WindowPreferences) general2.getChild("window");
        RecentFilesPreferences recent2 = (RecentFilesPreferences) general2.getChild("recent");
        AnalysisPreferences analysis2 = (AnalysisPreferences) reloaded.getChild("analysis");

        assertEquals("French", general2.getLanguage());
        assertFalse(general2.getConfirmOnDelete());
        assertEquals(new Rectangle(10, 20, 800, 600), window2.getFrameBounds());
        assertEquals(5, recent2.getMaxCount());
        assertEquals(2, recent2.getFiles().size());
        assertEquals(new File("/tmp/A.pamela"), recent2.getFiles().get(0));
        assertEquals(new File("/tmp"), recent2.getLastDirectory());
        assertFalse(analysis2.getBuildCacheEnabled());

        EntityStylePreferences entities2 = (EntityStylePreferences)
                reloaded.getChild("classDiagramDesign").getChild("entities");
        assertEquals(new java.awt.Color(12, 34, 56), entities2.getHeaderBackgroundColor());
        assertEquals(new java.awt.Font("Serif", java.awt.Font.BOLD, 14), entities2.getTitleFont());
    }

    @Test
    public void testSourceStylePreferencesRoundTrip() throws Exception {
        PamelaEditorPreferencesModel root = buildTree();
        SourceStylePreferences style = (SourceStylePreferences) root.getChild("generation").getChild("style");

        style.setUseTabulations(false);
        style.setTabulationSize(2);
        style.setBlankLineAfterPackage(false);
        style.setBlankLineAfterImports(false);
        style.setExpandEmptyBody(false);

        File tmp = File.createTempFile("prefs-style", ".json");
        tmp.deleteOnExit();
        PreferencesSerializer serializer = new PreferencesSerializer(factory);
        serializer.save(root, tmp);

        PamelaEditorPreferencesModel reloaded = buildTree();
        serializer.applyJson(reloaded, tmp);
        SourceStylePreferences style2 =
                (SourceStylePreferences) reloaded.getChild("generation").getChild("style");

        assertFalse(style2.getUseTabulations());
        assertEquals(2, style2.getTabulationSize());
        assertFalse(style2.getBlankLineAfterPackage());
        assertFalse(style2.getBlankLineAfterImports());
        assertFalse(style2.getExpandEmptyBody());
    }

    @Test
    public void testConnectorStylesRoundTrip() throws Exception {
        PamelaEditorPreferencesModel root = buildTree();
        ConnectorStylePreferences connectors = (ConnectorStylePreferences)
                root.getChild("classDiagramDesign").getChild("connectors");
        assertTrue(connectors.getStyles().isEmpty());

        ConnectorStyleDefaults.seedIfEmpty(connectors, factory);
        assertEquals(4, connectors.getStyles().size());
        // Edit one seeded style + reassign a default.
        connectors.getStyleById("relationship-line").setColor(new java.awt.Color(1, 2, 3));
        connectors.setDefaultInheritanceStyleId("inheritance-rect-polylin");

        File tmp = File.createTempFile("prefs-connectors", ".json");
        tmp.deleteOnExit();
        PreferencesSerializer serializer = new PreferencesSerializer(factory);
        serializer.save(root, tmp);

        PamelaEditorPreferencesModel reloaded = buildTree();
        serializer.applyJson(reloaded, tmp);
        ConnectorStylePreferences connectors2 = (ConnectorStylePreferences)
                reloaded.getChild("classDiagramDesign").getChild("connectors");

        assertEquals(4, connectors2.getStyles().size());
        assertEquals(new java.awt.Color(1, 2, 3), connectors2.getStyleById("relationship-line").getColor());
        assertEquals("inheritance-rect-polylin", connectors2.getDefaultInheritanceStyleId());
        // Enum + double round-trip on a seeded style.
        ConnectorStylePreference orth = connectors2.getStyleById("relationship-rect-polylin");
        assertEquals(org.openflexo.diana.connectors.ConnectorSpecification.ConnectorType.RECT_POLYLIN,
                orth.getConnectorType());
        assertTrue(orth.getRounded());
        assertEquals(10, orth.getArcSize());
    }

    @Test
    public void testForwardCompatibility_missingKeysKeepDefaults() throws Exception {
        // A JSON that only sets language must leave everything else at its default.
        PamelaEditorPreferencesModel root = buildTree();
        ((GeneralPreferences) root.getChild("general")).setLanguage("Dutch");

        File tmp = File.createTempFile("prefs-partial", ".json");
        tmp.deleteOnExit();
        PreferencesSerializer serializer = new PreferencesSerializer(factory);
        serializer.save(root, tmp);

        PamelaEditorPreferencesModel reloaded = buildTree();
        serializer.applyJson(reloaded, tmp);

        GeneralPreferences general2 = (GeneralPreferences) reloaded.getChild("general");
        assertEquals("Dutch", general2.getLanguage());
        assertTrue(general2.getConfirmOnDelete()); // untouched default
        WindowPreferences window2 = (WindowPreferences) general2.getChild("window");
        assertNull(window2.getFrameBounds()); // never set, no default
    }
}
