package org.openflexo.pamela.editor.ui.preferences;

import java.io.File;
import java.util.List;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;

/**
 * Recently-opened-files preferences ({@code /general/recent}). Absorbs the recent-files
 * list and last-directory formerly stored through {@code java.util.prefs}.
 *
 * <p>{@code files} is a LIST of {@link File} (string-convertible by the global converter
 * library); {@code lastDirectory} is a SINGLE {@link File}.</p>
 */
@ModelEntity
@ImplementationClass(RecentFilesPreferences.RecentFilesPreferencesImpl.class)
public interface RecentFilesPreferences extends PreferencesNode {

    String MAX_COUNT = "maxCount";
    String FILES = "files";
    String LAST_DIRECTORY = "lastDirectory";

    @Getter(value = MAX_COUNT, defaultValue = "10")
    int getMaxCount();

    @Setter(MAX_COUNT)
    void setMaxCount(int maxCount);

    @Getter(value = FILES, cardinality = Cardinality.LIST)
    List<File> getFiles();

    @Adder(FILES)
    void addToFiles(File file);

    @Remover(FILES)
    void removeFromFiles(File file);

    @Getter(LAST_DIRECTORY)
    File getLastDirectory();

    @Setter(LAST_DIRECTORY)
    void setLastDirectory(File lastDirectory);

    abstract class RecentFilesPreferencesImpl extends PreferencesNodeImpl implements RecentFilesPreferences {
    }
}
