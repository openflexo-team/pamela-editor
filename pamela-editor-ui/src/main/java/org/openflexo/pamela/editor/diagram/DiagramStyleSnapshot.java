package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreference;
import org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreferences;
import org.openflexo.pamela.editor.ui.preferences.EntityStylePreferences;
import org.openflexo.pamela.editor.ui.preferences.PreferencesManager;
import org.openflexo.pamela.factory.PamelaModelFactory;

/**
 * Bridges the global style <em>defaults</em> (preferences) into a {@link PamelaClassDiagram}'s
 * <em>embedded</em> styles (see {@code preferences-design.md §6bis}). Lives in the diagram package
 * (which may depend on the preferences package; the reverse dependency would be a cycle).
 *
 * <p>Used at diagram creation (snapshot the defaults) and to capture a catalogue style into a
 * diagram the first time it is applied to one of its connectors.</p>
 */
public final class DiagramStyleSnapshot {

    private DiagramStyleSnapshot() {
    }

    /**
     * Initializes a freshly created diagram from the current preference defaults: the entity-look
     * block, the two default connector styles, and the default style ids. Called once at creation.
     */
    public static void initializeFromDefaults(PamelaClassDiagram diagram, PamelaModelFactory factory) {
        PreferencesManager prefs = PreferencesManager.getInstance();
        EntityStylePreferences entityDefaults = prefs.entityStyle();
        ConnectorStylePreferences connectorDefaults = prefs.connectors();

        // Entity look block (snapshot).
        DiagramEntityStyle es = factory.newInstance(DiagramEntityStyle.class);
        if (entityDefaults != null) {
            es.setHeaderBackgroundColor(entityDefaults.getHeaderBackgroundColor());
            es.setBodyBackgroundColor(entityDefaults.getBodyBackgroundColor());
            es.setBorderColor(entityDefaults.getBorderColor());
            es.setTitleFont(entityDefaults.getTitleFont());
            es.setMemberFont(entityDefaults.getMemberFont());
        }
        diagram.setEntityStyle(es);

        // Default style ids + the two default connector styles (captured as used).
        if (connectorDefaults != null) {
            String inh = connectorDefaults.getDefaultInheritanceStyleId();
            String assoc = connectorDefaults.getDefaultAssociationStyleId();
            diagram.setDefaultInheritanceStyleId(inh);
            diagram.setDefaultAssociationStyleId(assoc);
            captureConnectorStyle(diagram, inh, factory);
            captureConnectorStyle(diagram, assoc, factory);
        }
    }

    /**
     * Ensures the diagram embeds a copy of the catalogue style {@code styleId} (no-op if already
     * present or unknown). Returns the diagram's embedded style for that id, or {@code null}.
     */
    public static ConnectorStylePreference captureConnectorStyle(PamelaClassDiagram diagram,
            String styleId, PamelaModelFactory factory) {
        if (styleId == null || styleId.isEmpty()) {
            return null;
        }
        ConnectorStylePreference existing = diagram.getConnectorStyleById(styleId);
        if (existing != null) {
            return existing;
        }
        ConnectorStylePreference source = PreferencesManager.getInstance().connectors().getStyleById(styleId);
        if (source == null) {
            return null;
        }
        ConnectorStylePreference copy = copyStyle(source, factory);
        diagram.addToConnectorStyles(copy);
        return copy;
    }

    /** Creates a diagram-factory copy of a connector style (same field values). */
    public static ConnectorStylePreference copyStyle(ConnectorStylePreference src,
            PamelaModelFactory factory) {
        ConnectorStylePreference dst = factory.newInstance(ConnectorStylePreference.class);
        copyStyleValues(src, dst);
        return dst;
    }

    /** Copies all visual field values from {@code src} into {@code dst} (keeps {@code dst}'s identity). */
    public static void copyStyleValues(ConnectorStylePreference src, ConnectorStylePreference dst) {
        dst.setId(src.getId());
        dst.setName(src.getName());
        dst.setConnectorType(src.getConnectorType());
        dst.setColor(src.getColor());
        dst.setLineWidth(src.getLineWidth());
        dst.setStraightLineWhenPossible(src.getStraightLineWhenPossible());
        dst.setRounded(src.getRounded());
        dst.setArcSize(src.getArcSize());
        dst.setAdjustability(src.getAdjustability());
        dst.setConstraints(src.getConstraints());
    }

    /** Copies the entity-look field values from preferences into a diagram's embedded block. */
    public static void copyEntityStyle(EntityStylePreferences src, DiagramEntityStyle dst) {
        if (src == null || dst == null) {
            return;
        }
        dst.setHeaderBackgroundColor(src.getHeaderBackgroundColor());
        dst.setBodyBackgroundColor(src.getBodyBackgroundColor());
        dst.setBorderColor(src.getBorderColor());
        dst.setTitleFont(src.getTitleFont());
        dst.setMemberFont(src.getMemberFont());
    }
}
