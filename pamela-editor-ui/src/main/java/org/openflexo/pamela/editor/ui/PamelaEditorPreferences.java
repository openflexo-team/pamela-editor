package org.openflexo.pamela.editor.ui;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openflexo.pamela.editor.ui.preferences.PreferenceChangeListener;
import org.openflexo.pamela.editor.ui.preferences.PreferencesManager;
import org.openflexo.pamela.editor.ui.preferences.RecentFilesPreferences;
import org.openflexo.pamela.editor.ui.preferences.WindowPreferences;
import org.openflexo.rm.ResourceLocator;

/**
 * Thin static facade over the thematic, JSON-backed preferences model (decision D3 —
 * the former {@code java.util.prefs} storage was absorbed; see {@code preferences-design.md §5.1}).
 *
 * <p>Existing callers keep this API; it now reads/writes the {@link WindowPreferences} and
 * {@link RecentFilesPreferences} nodes of {@link PreferencesManager}. All mutations are
 * persisted automatically (immediate-apply, debounced save).</p>
 */
public class PamelaEditorPreferences {

    private static final Rectangle DEFAULT_FRAME = new Rectangle(0, 0, 1000, 800);
    private static final Rectangle DEFAULT_INSPECTOR = new Rectangle(1000, 400, 400, 400);
    private static final Rectangle DEFAULT_PALETTE = new Rectangle(1000, 0, 400, 400);

    private static WindowPreferences window() {
        return PreferencesManager.getInstance().window();
    }

    private static RecentFilesPreferences recent() {
        return PreferencesManager.getInstance().recent();
    }

    // -------------------------------------------------------------- change notification

    public static void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        PreferencesManager.getInstance().addPreferenceChangeListener(pcl);
    }

    public static void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        PreferencesManager.getInstance().removePreferenceChangeListener(pcl);
    }

    // -------------------------------------------------------------- window bounds

    public static Rectangle getFrameBounds() {
        Rectangle r = window().getFrameBounds();
        return r != null ? r : DEFAULT_FRAME;
    }

    public static void setFrameBounds(Rectangle bounds) {
        window().setFrameBounds(bounds);
    }

    public static Rectangle getInspectorBounds() {
        Rectangle r = window().getInspectorBounds();
        return r != null ? r : DEFAULT_INSPECTOR;
    }

    public static void setInspectorBounds(Rectangle bounds) {
        window().setInspectorBounds(bounds);
    }

    public static Rectangle getPaletteBounds() {
        Rectangle r = window().getPaletteBounds();
        return r != null ? r : DEFAULT_PALETTE;
    }

    public static void setPaletteBounds(Rectangle bounds) {
        window().setPaletteBounds(bounds);
    }

    // -------------------------------------------------------------- recent files

    public static File getLastDirectory() {
        File dir = recent().getLastDirectory();
        if (dir != null) {
            return dir;
        }
        return ResourceLocator.retrieveResourceAsFile(ResourceLocator.locateResource("Fib"));
    }

    public static void setLastDirectory(File file) {
        recent().setLastDirectory(file);
    }

    public static int getLastFileCount() {
        return recent().getMaxCount();
    }

    public static void setLastFileCount(int count) {
        recent().setMaxCount(count);
    }

    public static List<File> getLastFiles() {
        return new ArrayList<>(recent().getFiles());
    }

    public static void setLastFiles(List<File> files) {
        RecentFilesPreferences recent = recent();
        List<File> current = new ArrayList<>(recent.getFiles());
        for (File f : current) {
            recent.removeFromFiles(f);
        }
        for (File f : files) {
            recent.addToFiles(f);
        }
    }

    public static void setLastFile(File file) {
        RecentFilesPreferences recent = recent();
        List<File> files = new ArrayList<>(recent.getFiles());
        files.remove(file);
        files.add(0, file);
        while (files.size() > recent.getMaxCount()) {
            files.remove(files.size() - 1);
        }
        setLastFiles(files);
        setLastDirectory(file.getParentFile());
    }
}
