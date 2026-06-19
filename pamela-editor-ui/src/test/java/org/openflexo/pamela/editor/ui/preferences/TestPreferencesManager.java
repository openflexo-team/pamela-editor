package org.openflexo.pamela.editor.ui.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        assertNotNull(mgr.diagram());
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
}
