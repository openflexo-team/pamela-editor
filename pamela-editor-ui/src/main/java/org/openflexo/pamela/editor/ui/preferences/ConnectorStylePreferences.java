package org.openflexo.pamela.editor.ui.preferences;

import java.util.List;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Embedded;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;

/**
 * Connector design preferences ({@code /classDiagramDesign/connectors}): a user-editable list
 * of {@link ConnectorStylePreference}, plus the default style assigned to inheritance and to
 * association/composition connectors (referenced by style id).
 */
@ModelEntity
@ImplementationClass(ConnectorStylePreferences.ConnectorStylePreferencesImpl.class)
public interface ConnectorStylePreferences extends PreferencesNode {

    String STYLES = "styles";
    String DEFAULT_INHERITANCE_STYLE_ID = "defaultInheritanceStyleId";
    String DEFAULT_ASSOCIATION_STYLE_ID = "defaultAssociationStyleId";

    @Getter(value = STYLES, cardinality = Cardinality.LIST)
    @Embedded
    List<ConnectorStylePreference> getStyles();

    @Adder(STYLES)
    void addToStyles(ConnectorStylePreference style);

    @Remover(STYLES)
    void removeFromStyles(ConnectorStylePreference style);

    @Getter(value = DEFAULT_INHERITANCE_STYLE_ID, defaultValue = "inheritance-line")
    String getDefaultInheritanceStyleId();

    @Setter(DEFAULT_INHERITANCE_STYLE_ID)
    void setDefaultInheritanceStyleId(String id);

    @Getter(value = DEFAULT_ASSOCIATION_STYLE_ID, defaultValue = "relationship-rect-polylin")
    String getDefaultAssociationStyleId();

    @Setter(DEFAULT_ASSOCIATION_STYLE_ID)
    void setDefaultAssociationStyleId(String id);

    /** The style with the given id, or {@code null}. */
    ConnectorStylePreference getStyleById(String id);

    // Object-valued views of the default-style ids, for binding to a style drop-down.
    ConnectorStylePreference getDefaultInheritanceStyle();

    void setDefaultInheritanceStyle(ConnectorStylePreference style);

    ConnectorStylePreference getDefaultAssociationStyle();

    void setDefaultAssociationStyle(ConnectorStylePreference style);

    abstract class ConnectorStylePreferencesImpl extends PreferencesNodeImpl
            implements ConnectorStylePreferences {
        @Override
        public ConnectorStylePreference getStyleById(String id) {
            if (id == null || getStyles() == null) {
                return null;
            }
            for (ConnectorStylePreference s : getStyles()) {
                if (id.equals(s.getId())) {
                    return s;
                }
            }
            return null;
        }

        @Override
        public ConnectorStylePreference getDefaultInheritanceStyle() {
            return getStyleById(getDefaultInheritanceStyleId());
        }

        @Override
        public void setDefaultInheritanceStyle(ConnectorStylePreference style) {
            setDefaultInheritanceStyleId(style != null ? style.getId() : null);
        }

        @Override
        public ConnectorStylePreference getDefaultAssociationStyle() {
            return getStyleById(getDefaultAssociationStyleId());
        }

        @Override
        public void setDefaultAssociationStyle(ConnectorStylePreference style) {
            setDefaultAssociationStyleId(style != null ? style.getId() : null);
        }
    }
}
