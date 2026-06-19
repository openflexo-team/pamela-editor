package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/** Source-analysis preferences theme ({@code /analysis}). */
@ModelEntity
@ImplementationClass(AnalysisPreferences.AnalysisPreferencesImpl.class)
public interface AnalysisPreferences extends PreferencesNode {

    String BUILD_CACHE_ENABLED = "buildCacheEnabled";

    @Getter(value = BUILD_CACHE_ENABLED, defaultValue = "true")
    boolean getBuildCacheEnabled();

    @Setter(BUILD_CACHE_ENABLED)
    void setBuildCacheEnabled(boolean enabled);

    abstract class AnalysisPreferencesImpl extends PreferencesNodeImpl implements AnalysisPreferences {
    }
}
