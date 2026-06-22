package org.openflexo.pamela.editor.ui.preferences;

import java.awt.Color;

import org.openflexo.diana.connectors.ConnectorSpecification.ConnectorType;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;

/**
 * Controller for the connector-styles master-detail editor ({@link ConnectorStyleEditorPanel}).
 * Creates / removes {@link ConnectorStylePreference}s through the preferences factory.
 */
public class ConnectorStyleEditorFIBController extends PamelaEditorFIBController<ConnectorStylePreferences> {

    public ConnectorStyleEditorFIBController(FIBComponent rootComponent) {
        super(rootComponent);
    }

    /** Creates a new style with default appearance and appends it to the list. */
    public ConnectorStylePreference addStyle() {
        ConnectorStylePreferences prefs = getDataObject();
        if (prefs == null) {
            return null;
        }
        PreferencesFactory factory = PreferencesManager.getInstance().getFactory();
        ConnectorStylePreference style = factory.newInstance(ConnectorStylePreference.class);
        style.setId(uniqueId(prefs));
        style.setName(uniqueName(prefs));
        style.setConnectorType(ConnectorType.LINE);
        style.setColor(Color.DARK_GRAY);
        style.setLineWidth(1.5);
        prefs.addToStyles(style);
        return style;
    }

    /** Removes the given style (no-op if {@code null}). */
    public void removeStyle(ConnectorStylePreference style) {
        ConnectorStylePreferences prefs = getDataObject();
        if (prefs != null && style != null) {
            prefs.removeFromStyles(style);
        }
    }

    private static String uniqueId(ConnectorStylePreferences prefs) {
        int i = 1;
        String id;
        do {
            id = "style-" + i++;
        } while (prefs.getStyleById(id) != null);
        return id;
    }

    private static String uniqueName(ConnectorStylePreferences prefs) {
        int i = 1;
        String name;
        boolean clash;
        do {
            name = "New style " + i++;
            clash = false;
            for (ConnectorStylePreference s : prefs.getStyles()) {
                if (name.equals(s.getName())) {
                    clash = true;
                    break;
                }
            }
        } while (clash);
        return name;
    }
}
