package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/** General application preferences theme ({@code /general}). */
@ModelEntity
@ImplementationClass(GeneralPreferences.GeneralPreferencesImpl.class)
public interface GeneralPreferences extends PreferencesNode {

    String LANGUAGE = "language";
    String CONFIRM_ON_DELETE = "confirmOnDelete";

    @Getter(value = LANGUAGE, defaultValue = "English")
    String getLanguage();

    @Setter(LANGUAGE)
    void setLanguage(String language);

    @Getter(value = CONFIRM_ON_DELETE, defaultValue = "true")
    boolean getConfirmOnDelete();

    @Setter(CONFIRM_ON_DELETE)
    void setConfirmOnDelete(boolean confirmOnDelete);

    abstract class GeneralPreferencesImpl extends PreferencesNodeImpl implements GeneralPreferences {
    }
}
