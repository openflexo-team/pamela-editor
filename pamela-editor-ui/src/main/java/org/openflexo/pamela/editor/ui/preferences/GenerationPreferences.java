package org.openflexo.pamela.editor.ui.preferences;

import java.util.Arrays;
import java.util.List;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.editor.model.PropertyIdentifierStyle;

/**
 * Code-generation preferences theme ({@code /generation}). Groups the settings that govern how the
 * editor writes new source. For now it carries a single option — the default property-identifier
 * style. See {@code property-identifier-constant-design.md §5}.
 */
@ModelEntity
@ImplementationClass(GenerationPreferences.GenerationPreferencesImpl.class)
public interface GenerationPreferences extends PreferencesNode {

    String DEFAULT_PROPERTY_IDENTIFIER_STYLE = "defaultPropertyIdentifierStyle";

    /**
     * The identifier style used when <em>generating</em> a property on an entity with no decisive
     * style of its own (no property yet).
     */
    @Getter(value = DEFAULT_PROPERTY_IDENTIFIER_STYLE, defaultValue = "CONSTANT")
    PropertyIdentifierStyle getDefaultPropertyIdentifierStyle();

    @Setter(DEFAULT_PROPERTY_IDENTIFIER_STYLE)
    void setDefaultPropertyIdentifierStyle(PropertyIdentifierStyle style);

    /** The choices offered for {@link #getDefaultPropertyIdentifierStyle()} — CONSTANT or LITERAL only. */
    List<PropertyIdentifierStyle> getIdentifierStyleChoices();

    abstract class GenerationPreferencesImpl extends PreferencesNodeImpl implements GenerationPreferences {
        @Override
        public List<PropertyIdentifierStyle> getIdentifierStyleChoices() {
            return Arrays.asList(PropertyIdentifierStyle.CONSTANT, PropertyIdentifierStyle.LITERAL);
        }
    }
}
