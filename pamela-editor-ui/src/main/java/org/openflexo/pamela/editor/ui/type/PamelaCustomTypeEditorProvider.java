package org.openflexo.pamela.editor.ui.type;

import org.openflexo.connie.type.CustomType;
import org.openflexo.gina.controller.CustomTypeEditor;
import org.openflexo.gina.controller.CustomTypeEditorProvider;

/**
 * {@link CustomTypeEditorProvider} that supplies the {@link PamelaEntityTypeEditor}
 * for the {@link PamelaEntityType} category. The editor shares the same
 * {@link PamelaEntityTypeFactory} as the manager so the user's choice propagates
 * to {@code TypeSelector}.
 */
public class PamelaCustomTypeEditorProvider implements CustomTypeEditorProvider {

    private final PamelaEntityTypeEditor editor;

    public PamelaCustomTypeEditorProvider(PamelaEntityTypeFactory factory) {
        this.editor = new PamelaEntityTypeEditor(factory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CustomType> CustomTypeEditor<T> getCustomTypeEditor(Class<T> typeClass) {
        if (typeClass == PamelaEntityType.class) {
            return (CustomTypeEditor<T>) editor;
        }
        return null;
    }
}
