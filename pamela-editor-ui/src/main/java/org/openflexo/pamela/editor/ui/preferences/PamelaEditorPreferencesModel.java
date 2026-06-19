package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;

/**
 * Serialization root of the preferences tree. It <em>is</em> the (invisible) root
 * {@link PreferencesNode}; its {@link #getChildren()} are the top-level themes.
 * Its {@code name} is the empty string and it is not shown in the browser
 * ({@code rootVisible="false"}).
 */
@ModelEntity
@ImplementationClass(PamelaEditorPreferencesModelImpl.class)
public interface PamelaEditorPreferencesModel extends PreferencesNode {
}
