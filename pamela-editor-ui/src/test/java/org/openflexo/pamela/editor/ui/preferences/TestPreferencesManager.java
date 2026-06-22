package org.openflexo.pamela.editor.ui.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link PreferencesManager} lifecycle: immediate-apply change dispatch and
 * (debounced) persistence. Uses a temporary preferences file (never the user's real one).
 */
public class TestPreferencesManager {

    private File prefsFile;

    @Before
    public void setUp() throws Exception {
        prefsFile = File.createTempFile("prefs-mgr", ".json");
        prefsFile.delete(); // start with no file so the manager creates it
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

    @Test
    public void testAccessorsAndFileCreatedOnFirstRun() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        assertNotNull(mgr.general());
        assertNotNull(mgr.window());
        assertNotNull(mgr.recent());
        assertNotNull(mgr.classDiagramDesign());
        assertNotNull(mgr.entityStyle());
        assertNotNull(mgr.analysis());
        // First run with no file → manager imported (nothing) and saved a fresh file.
        assertTrue(prefsFile.exists());
    }

    @Test
    public void testImmediateApplyDispatchesEvent() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        AtomicReference<String> seenKey = new AtomicReference<>();
        AtomicReference<Object> seenValue = new AtomicReference<>();
        mgr.addPreferenceChangeListener((node, key, oldV, newV) -> {
            if (node instanceof GeneralPreferences) {
                seenKey.set(key);
                seenValue.set(newV);
            }
        });

        mgr.general().setLanguage("French");

        assertEquals(GeneralPreferences.LANGUAGE, seenKey.get());
        assertEquals("French", seenValue.get());
    }

    @Test
    public void testPersistAndReload() throws Exception {
        PreferencesManager mgr = PreferencesManager.getInstance();
        mgr.general().setLanguage("Dutch");
        mgr.analysis().setBuildCacheEnabled(false);
        mgr.saveNow(); // bypass the debounce for a deterministic assertion

        // Reload through a fresh factory + serializer (simulates a new session).
        PreferencesRegistry.resetForTests();
        PreferencesRegistry.registerBuiltinThemes();
        PreferencesFactory factory = new PreferencesFactory();
        PamelaEditorPreferencesModel reloaded =
                PreferencesRegistry.buildTree(factory, Function.identity());
        new PreferencesSerializer(factory).applyJson(reloaded, prefsFile);

        assertEquals("Dutch", ((GeneralPreferences) reloaded.getChild("general")).getLanguage());
        assertEquals(false, ((AnalysisPreferences) reloaded.getChild("analysis")).getBuildCacheEnabled());
    }

    // ---- Working-copy lifecycle (Apply / Save / Cancel / Reset) -----------------------------

    private static GeneralPreferences general(PamelaEditorPreferencesModel m) {
        return (GeneralPreferences) m.getChild("general");
    }

    /** Reads the language value currently persisted in the file (fresh session). */
    private String fileLanguage() throws Exception {
        PreferencesFactory f = new PreferencesFactory();
        PamelaEditorPreferencesModel m = PreferencesRegistry.buildTree(f, Function.identity());
        new PreferencesSerializer(f).applyJson(m, prefsFile);
        return general(m).getLanguage();
    }

    @Test
    public void testWorkingCopyIsolation_apply_save() throws Exception {
        PreferencesManager mgr = PreferencesManager.getInstance();
        PamelaEditorPreferencesModel wc = mgr.createWorkingCopy();

        general(wc).setLanguage("French");
        // Editing the working copy must not touch the live model nor the file.
        assertEquals("English", mgr.general().getLanguage());
        assertEquals("English", fileLanguage());

        // Apply: live changes, file unchanged.
        mgr.applyWorkingCopy(wc);
        assertEquals("French", mgr.general().getLanguage());
        assertEquals("English", fileLanguage());

        // Save: file now matches.
        mgr.save(wc);
        assertEquals("French", fileLanguage());
    }

    @Test
    public void testCancelRevertsToSaved() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        PamelaEditorPreferencesModel wc = mgr.createWorkingCopy();
        general(wc).setLanguage("French");
        mgr.applyWorkingCopy(wc); // live = French, file = English (default)

        mgr.cancelToSaved();
        assertEquals("English", mgr.general().getLanguage());
    }

    @Test
    public void testResetNodeToDefaults() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        PamelaEditorPreferencesModel wc = mgr.createWorkingCopy();
        EntityStylePreferences entities = (EntityStylePreferences)
                wc.getChild("classDiagramDesign").getChild("entities");
        entities.setHeaderBackgroundColor(new Color(1, 2, 3));

        mgr.resetNodeToDefaults(entities);
        assertEquals(new Color(210, 225, 245), entities.getHeaderBackgroundColor());

        // Reset is scoped to the working copy — live/file untouched until Apply/Save.
        assertEquals("English", mgr.general().getLanguage());
    }

    @Test
    public void testResetConnectorsReseeds() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        PamelaEditorPreferencesModel wc = mgr.createWorkingCopy();
        ConnectorStylePreferences connectors = (ConnectorStylePreferences)
                wc.getChild("classDiagramDesign").getChild("connectors");
        connectors.removeFromStyles(connectors.getStyles().get(0));
        assertEquals(3, connectors.getStyles().size());

        mgr.resetNodeToDefaults(connectors);
        assertEquals(4, connectors.getStyles().size());
    }

    @Test
    public void testCanResetNode() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        PamelaEditorPreferencesModel wc = mgr.createWorkingCopy();
        assertTrue(mgr.canResetNode((PreferencesNode) wc.getChild("general")));
        assertTrue(mgr.canResetNode((PreferencesNode)
                wc.getChild("classDiagramDesign").getChild("entities")));
        // The invisible root has no own resettable properties.
        assertFalse(mgr.canResetNode(wc));
    }

    @Test
    public void testButtonStateQueries() throws Exception {
        PreferencesManager mgr = PreferencesManager.getInstance();
        PamelaEditorPreferencesModel wc = mgr.createWorkingCopy();

        // Pristine working copy: nothing to apply, nothing to save, nothing to revert.
        assertFalse(mgr.isModified(wc));
        assertFalse(mgr.isSavable(wc));
        assertFalse(mgr.isRevertable(wc));

        // Edit the working copy → Apply and Save become meaningful, but not yet applied/saved.
        ((GeneralPreferences) wc.getChild("general")).setLanguage("French");
        assertTrue(mgr.isModified(wc));
        assertTrue(mgr.isSavable(wc));
        assertTrue(mgr.isRevertable(wc));

        // Apply → live matches working (nothing more to apply) but disk still differs (savable).
        mgr.applyWorkingCopy(wc);
        assertFalse(mgr.isModified(wc));
        assertTrue(mgr.isSavable(wc));
        assertTrue(mgr.isRevertable(wc));

        // Save → disk matches; everything is clean again.
        mgr.save(wc);
        assertFalse(mgr.isModified(wc));
        assertFalse(mgr.isSavable(wc));
        assertFalse(mgr.isRevertable(wc));
    }

    @Test
    public void testIsNodeAtDefaults() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        PamelaEditorPreferencesModel wc = mgr.createWorkingCopy();
        EntityStylePreferences entities = (EntityStylePreferences)
                wc.getChild("classDiagramDesign").getChild("entities");

        assertTrue(mgr.isNodeAtDefaults(entities));
        entities.setHeaderBackgroundColor(new java.awt.Color(1, 2, 3));
        assertFalse(mgr.isNodeAtDefaults(entities));
        mgr.resetNodeToDefaults(entities);
        assertTrue(mgr.isNodeAtDefaults(entities));
    }
}
