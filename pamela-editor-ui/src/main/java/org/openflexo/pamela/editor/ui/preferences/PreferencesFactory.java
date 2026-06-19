package org.openflexo.pamela.editor.ui.preferences;

import java.awt.Rectangle;
import java.util.Arrays;

import org.openflexo.pamela.PamelaMetaModel;
import org.openflexo.pamela.converter.AWTRectangleConverter;
import org.openflexo.pamela.exceptions.ModelDefinitionException;
import org.openflexo.pamela.factory.PamelaModelFactory;

/**
 * PAMELA factory for the preferences model. Built from the union of the model root and every
 * registered theme class ({@link PreferencesRegistry#allEntityClasses()}).
 *
 * <p>{@link Rectangle} preference properties (window bounds) are declared
 * {@code @Getter(ignoreType = true)} so PAMELA does not treat {@link Rectangle} as a model-entity
 * reference at metamodel-build time. The {@code AWTRectangleConverter} is registered on this
 * factory's string encoder so {@link PreferencesSerializer} can convert those values to/from JSON.</p>
 */
public class PreferencesFactory extends PamelaModelFactory {

    public PreferencesFactory() throws ModelDefinitionException {
        super(new PamelaMetaModel(Arrays.asList(PreferencesRegistry.allEntityClasses())));
        addConverter(new AWTRectangleConverter());
    }
}
