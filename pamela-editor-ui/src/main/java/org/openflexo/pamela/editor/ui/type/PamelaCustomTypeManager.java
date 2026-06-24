package org.openflexo.pamela.editor.ui.type;

import java.util.HashMap;
import java.util.Map;

import org.openflexo.connie.type.CustomType;
import org.openflexo.connie.type.CustomTypeFactory;
import org.openflexo.connie.type.CustomTypeManager;

/**
 * {@link CustomTypeManager} exposing the single {@link PamelaEntityType} factory,
 * so the Gina {@code TypeSelector} offers PAMELA source entities as a custom type
 * category. The factory is shared with the editor (see
 * {@link PamelaEntityTypeFactory}).
 */
public class PamelaCustomTypeManager implements CustomTypeManager {

    private final Map<Class<? extends CustomType>, CustomTypeFactory<?>> factories = new HashMap<>();

    public PamelaCustomTypeManager(PamelaEntityTypeFactory factory) {
        factories.put(PamelaEntityType.class, factory);
    }

    @Override
    public Map<Class<? extends CustomType>, CustomTypeFactory<?>> getCustomTypeFactories() {
        return factories;
    }
}
