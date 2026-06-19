package org.openflexo.pamela.editor.ui.preferences;

import java.awt.Rectangle;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.openflexo.pamela.converter.AWTRectangleConverter;
import org.openflexo.pamela.model.StringConverterLibrary;
import org.openflexo.pamela.model.StringConverterLibrary.Converter;

/**
 * One-time import of the legacy {@code java.util.prefs} values (decision D3,
 * {@code preferences-design.md §5.1}) into the new model, run by {@link PreferencesManager}
 * on first launch (when {@code preferences.json} does not yet exist). The legacy backend is
 * never written again afterwards.
 */
final class LegacyPreferencesImporter {

    private static final Logger logger = Logger.getLogger(LegacyPreferencesImporter.class.getPackage().getName());

    // Legacy node + keys (formerly in PamelaEditorPreferences).
    private static final String LEGACY_NODE = "DiagramEditor";
    private static final String FRAME = "Frame";
    private static final String INSPECTOR = "Inspector";
    private static final String PALETTE = "Palette";
    private static final String LAST_DIR = "LastDirectory";
    private static final String LAST_FILES_COUNT = "LAST_FILES_COUNT";
    private static final String LAST_FILE = "LAST_FILE";

    private LegacyPreferencesImporter() {
    }

    static void importInto(PreferencesManager manager) {
        try {
            Preferences prefs = Preferences.userRoot().node(LEGACY_NODE);
            if (prefs.keys().length == 0) {
                return; // nothing to import
            }
            AWTRectangleConverter rect = new AWTRectangleConverter();
            Converter<File> fileConverter = StringConverterLibrary.getInstance().getConverter(File.class);

            WindowPreferences window = manager.window();
            if (window != null) {
                Rectangle frame = readRectangle(prefs, FRAME, rect);
                if (frame != null) window.setFrameBounds(frame);
                Rectangle insp = readRectangle(prefs, INSPECTOR, rect);
                if (insp != null) window.setInspectorBounds(insp);
                Rectangle pal = readRectangle(prefs, PALETTE, rect);
                if (pal != null) window.setPaletteBounds(pal);
            }

            RecentFilesPreferences recent = manager.recent();
            if (recent != null) {
                int count = prefs.getInt(LAST_FILES_COUNT, recent.getMaxCount());
                recent.setMaxCount(count);
                File lastDir = readFile(prefs, LAST_DIR, fileConverter);
                if (lastDir != null) recent.setLastDirectory(lastDir);
                for (int i = 0; i < count; i++) {
                    File f = readFile(prefs, LAST_FILE + i, fileConverter);
                    if (f != null) recent.addToFiles(f);
                }
            }
            logger.info("Imported legacy java.util.prefs preferences into the new model");
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Could not import legacy preferences", e);
        }
    }

    private static Rectangle readRectangle(Preferences prefs, String key, AWTRectangleConverter conv) {
        String s = prefs.get(key, null);
        if (s == null) {
            return null;
        }
        try {
            return conv.convertFromString(s, null);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static File readFile(Preferences prefs, String key, Converter<File> conv) {
        String s = prefs.get(key, null);
        if (s == null) {
            return null;
        }
        try {
            return conv.convertFromString(s, null);
        }
        catch (Exception e) {
            return null;
        }
    }
}
