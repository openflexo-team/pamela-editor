package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.editor.model.CustomMethodFilter;

/** Source-analysis preferences theme ({@code /analysis}). */
@ModelEntity
@ImplementationClass(AnalysisPreferences.AnalysisPreferencesImpl.class)
public interface AnalysisPreferences extends PreferencesNode {

    String BUILD_CACHE_ENABLED = "buildCacheEnabled";
    String CUSTOM_METHOD_FILTER = "customMethodFilter";

    @Getter(value = BUILD_CACHE_ENABLED, defaultValue = "true")
    boolean getBuildCacheEnabled();

    @Setter(BUILD_CACHE_ENABLED)
    void setBuildCacheEnabled(boolean enabled);

    /**
     * Which interface methods are surfaced as entity operations (custom methods) in the
     * DetailedBrowser and the diagram method compartments. See {@code custom-method-design.md}.
     */
    @Getter(value = CUSTOM_METHOD_FILTER, defaultValue = "PAMELA_ANNOTATED")
    CustomMethodFilter getCustomMethodFilter();

    @Setter(CUSTOM_METHOD_FILTER)
    void setCustomMethodFilter(CustomMethodFilter filter);

    abstract class AnalysisPreferencesImpl extends PreferencesNodeImpl implements AnalysisPreferences {
    }
}
