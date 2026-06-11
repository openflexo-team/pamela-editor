package org.openflexo.pamela.editor.ui.widget;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;

/**
 * ContextPanel sub-view for the Diagram inspector mode (ui-design.md §19.4).
 *
 * <p>Shows either:</p>
 * <ul>
 *   <li>The diagram name and entity count (when no EntityView is selected), or</li>
 *   <li>Editable x/y/width/height geometry fields for the selected EntityView.</li>
 * </ul>
 *
 * <p>Uses plain Swing (not FIB) to avoid Connie double↔String coercion issues
 * (decision D5). Changes in the text fields write back to the PAMELA model via
 * setters; incoming PAMELA {@link PropertyChangeEvent}s update the fields.</p>
 */
@SuppressWarnings("serial")
public class DiagramElementInspectorView extends JPanel {

    private static final String CARD_EMPTY       = "empty";
    private static final String CARD_DIAGRAM      = "diagram";
    private static final String CARD_ENTITY_VIEW  = "entityView";

    private final CardLayout cards = new CardLayout();

    // Diagram card
    private final JTextField diagramNameField = new JTextField();
    private final JTextField entityCountField = new JTextField();

    // EntityView card
    private final JFormattedTextField xField      = makeDoubleField();
    private final JFormattedTextField yField      = makeDoubleField();
    private final JFormattedTextField widthField  = makeDoubleField();
    private final JFormattedTextField heightField = makeDoubleField();

    private EntityView currentEntityView;
    private final PropertyChangeListener entityViewListener = this::onEntityViewPropertyChange;

    public DiagramElementInspectorView() {
        setLayout(cards);

        add(new JPanel(), CARD_EMPTY);
        add(buildDiagramCard(), CARD_DIAGRAM);
        add(buildEntityViewCard(), CARD_ENTITY_VIEW);

        cards.show(this, CARD_EMPTY);
    }

    // -------------------------------------------------------------------------
    // Public API — called by ContextPanel
    // -------------------------------------------------------------------------

    /** Shows diagram-level info; no EntityView selected. */
    public void showDiagram(PamelaClassDiagram diagram) {
        detachEntityView();
        if (diagram != null) {
            diagramNameField.setText(diagram.getName() != null ? diagram.getName() : "");
            int count = diagram.getEntityViews() != null ? diagram.getEntityViews().size() : 0;
            entityCountField.setText(String.valueOf(count));
        }
        cards.show(this, CARD_DIAGRAM);
    }

    /** Shows geometry fields for the selected EntityView. */
    public void showEntityView(EntityView ev) {
        detachEntityView();
        currentEntityView = ev;
        if (ev != null) {
            // Register as listener so PAMELA model changes (e.g. drag) refresh fields
            ev.getPropertyChangeSupport().addPropertyChangeListener(entityViewListener);
            refreshEntityViewFields(ev);
        }
        cards.show(this, CARD_ENTITY_VIEW);
    }

    /** Clears the panel. */
    public void clear() {
        detachEntityView();
        cards.show(this, CARD_EMPTY);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private JPanel buildDiagramCard() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Diagram"));
        GridBagConstraints c = baseConstraints();

        c.gridy = 0;
        c.gridx = 0; p.add(label("Name"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        diagramNameField.setEditable(false);
        diagramNameField.setOpaque(false);
        diagramNameField.setBorder(null);
        p.add(diagramNameField, c);

        c.gridy = 1; c.weightx = 0;
        c.gridx = 0; c.fill = GridBagConstraints.NONE; p.add(label("Entities"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        entityCountField.setEditable(false);
        entityCountField.setOpaque(false);
        entityCountField.setBorder(null);
        p.add(entityCountField, c);

        return p;
    }

    private JPanel buildEntityViewCard() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Entity view"));
        GridBagConstraints c = baseConstraints();

        String[] labels  = { "X",     "Y",     "Width",  "Height" };
        JFormattedTextField[] fields = { xField, yField, widthField, heightField };

        for (int i = 0; i < labels.length; i++) {
            c.gridy = i;
            c.gridx = 0; c.fill = GridBagConstraints.NONE; c.weightx = 0;
            p.add(label(labels[i]), c);
            c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
            p.add(fields[i], c);
        }

        // Write-back listeners: commit on focus-lost or Enter
        addWriteBack(xField,      v -> { if (currentEntityView != null) currentEntityView.setX(v); });
        addWriteBack(yField,      v -> { if (currentEntityView != null) currentEntityView.setY(v); });
        addWriteBack(widthField,  v -> { if (currentEntityView != null) currentEntityView.setWidth(v); });
        addWriteBack(heightField, v -> { if (currentEntityView != null) currentEntityView.setHeight(v); });

        return p;
    }

    private void refreshEntityViewFields(EntityView ev) {
        // Prevent write-back loops: suppress listeners briefly by working on EDT
        SwingUtilities.invokeLater(() -> {
            silentSet(xField,      ev.getX());
            silentSet(yField,      ev.getY());
            silentSet(widthField,  ev.getWidth());
            silentSet(heightField, ev.getHeight());
        });
    }

    private void onEntityViewPropertyChange(PropertyChangeEvent evt) {
        if (currentEntityView == null) return;
        String name = evt.getPropertyName();
        if (EntityView.X.equals(name))      silentSet(xField,      currentEntityView.getX());
        if (EntityView.Y.equals(name))      silentSet(yField,      currentEntityView.getY());
        if (EntityView.WIDTH.equals(name))  silentSet(widthField,  currentEntityView.getWidth());
        if (EntityView.HEIGHT.equals(name)) silentSet(heightField, currentEntityView.getHeight());
    }

    private void detachEntityView() {
        if (currentEntityView != null) {
            currentEntityView.getPropertyChangeSupport()
                    .removePropertyChangeListener(entityViewListener);
            currentEntityView = null;
        }
    }

    // -------------------------------------------------------------------------
    // Field factories and utilities
    // -------------------------------------------------------------------------

    private static JFormattedTextField makeDoubleField() {
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
        fmt.setMaximumFractionDigits(1);
        fmt.setGroupingUsed(false);
        JFormattedTextField f = new JFormattedTextField(fmt);
        f.setColumns(6);
        f.setHorizontalAlignment(SwingConstants.RIGHT);
        return f;
    }

    @FunctionalInterface
    private interface DoubleConsumer { void accept(double value); }

    private static void addWriteBack(JFormattedTextField field, DoubleConsumer consumer) {
        field.addPropertyChangeListener("value", evt -> {
            Object v = field.getValue();
            if (v instanceof Number) {
                consumer.accept(((Number) v).doubleValue());
            }
        });
    }

    private static void silentSet(JFormattedTextField field, double value) {
        try {
            // Parse current display to avoid spurious write-back
            String current = field.getText();
            Number parsed = (Number) field.getFormatter().stringToValue(current);
            if (parsed != null && Math.abs(parsed.doubleValue() - value) < 0.001) return;
        } catch (ParseException ignored) {}
        field.setValue(value);
    }

    private static GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text + ":");
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }
}
