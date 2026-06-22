package org.openflexo.pamela.editor.ui.preferences;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Two-pane preferences window (see {@code preferences-design.md §3}): a tree browser on the
 * left, an editable property editor on the right, and a button bar at the bottom.
 *
 * <p>Editing is done on a detached <b>working copy</b> ({@link PreferencesManager#createWorkingCopy()}):
 * nothing is applied or saved while typing. The buttons commit explicitly —
 * <b>Apply</b> (push to the running editor, no file write), <b>Save</b> (apply + write the file),
 * <b>Cancel</b> (revert to the last saved version), <b>Reset to defaults</b> (reset the currently
 * selected node only; disabled for nodes without resettable properties).</p>
 */
public class PreferencesDialog extends JDialog {

    private static PreferencesDialog instance;

    private static final String CARD_INSPECTOR = "inspector";
    private static final String CARD_CONNECTORS = "connectors";

    private final Function<String, String> localizer;
    private final DiagramRestyleHandler restyleHandler;

    private PreferencesBrowser browser;
    private final PreferencesInspectorController inspector;

    private final JPanel rightPane;
    private final CardLayout rightCards;
    private ConnectorStyleEditorPanel connectorEditor;

    private final JButton resetButton;
    private final JButton applyButton;
    private final JButton saveButton;
    private final JButton cancelButton;

    /** The model currently edited by the dialog (a detached working copy of the live model). */
    private PamelaEditorPreferencesModel workingModel;
    /** The working-copy node currently shown in the right pane (for Reset-to-defaults). */
    private PreferencesNode currentNode;

    /** Working-copy objects already observed for live button-state refresh (identity). */
    private final java.util.Set<Object> observed =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final java.beans.PropertyChangeListener workingListener = e -> {
        attachWorkingListeners();
        updateButtons();
    };

    private PreferencesDialog(Frame owner, String title, Function<String, String> localizer,
                              DiagramRestyleHandler restyleHandler) {
        super(owner, title, false);
        this.localizer = localizer;
        this.restyleHandler = restyleHandler;

        workingModel = PreferencesManager.getInstance().createWorkingCopy();
        browser = new PreferencesBrowser(workingModel);
        inspector = new PreferencesInspectorController();

        rightCards = new CardLayout();
        rightPane = new JPanel(rightCards);
        rightPane.add(inspector.getRootPane(), CARD_INSPECTOR);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, browser, rightPane);
        split.setDividerLocation(220);
        split.setResizeWeight(0.3);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(split, BorderLayout.CENTER);

        // ---- Button bar -------------------------------------------------------
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        resetButton = new JButton(loc("reset_to_default"));
        resetButton.addActionListener(e -> onReset());
        applyButton = new JButton(loc("apply"));
        applyButton.addActionListener(e -> {
            PreferencesManager.getInstance().applyWorkingCopy(workingModel);
            maybePromptRestyle();
            updateButtons();
        });
        saveButton = new JButton(loc("save"));
        saveButton.addActionListener(e -> {
            PreferencesManager.getInstance().save(workingModel);
            maybePromptRestyle();
            updateButtons();
        });
        cancelButton = new JButton(loc("cancel"));
        cancelButton.addActionListener(e -> onCancel());
        buttons.add(resetButton);
        buttons.add(applyButton);
        buttons.add(cancelButton);
        buttons.add(saveButton);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(760, 560));

        browser.getController().addSelectionListener(selection -> {
            if (selection != null && !selection.isEmpty() && selection.get(0) instanceof PreferencesNode) {
                showEditorFor((PreferencesNode) selection.get(0));
            }
        });

        attachWorkingListeners();
        updateButtons();

        pack();
        setLocationRelativeTo(owner);
    }

    /** Observes the working copy (nodes + connector styles) so edits refresh the button states. */
    private void attachWorkingListeners() {
        if (workingModel != null) {
            attachWorkingDeep(workingModel);
        }
    }

    private void attachWorkingDeep(PreferencesNode node) {
        if (observed.add(node)) {
            node.getPropertyChangeSupport().addPropertyChangeListener(workingListener);
        }
        for (PreferencesNode child : node.getChildren()) {
            attachWorkingDeep(child);
        }
        if (node instanceof ConnectorStylePreferences) {
            for (ConnectorStylePreference style : ((ConnectorStylePreferences) node).getStyles()) {
                if (observed.add(style)) {
                    style.getPropertyChangeSupport().addPropertyChangeListener(workingListener);
                }
            }
        }
    }

    /** Grays each button when its action has no effect in the current state. */
    private void updateButtons() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        applyButton.setEnabled(mgr.isModified(workingModel));
        saveButton.setEnabled(mgr.isSavable(workingModel));
        cancelButton.setEnabled(mgr.isRevertable(workingModel));
        resetButton.setEnabled(currentNode != null
                && mgr.canResetNode(currentNode) && !mgr.isNodeAtDefaults(currentNode));
    }

    /** Shows the dedicated editor for connector styles, otherwise the generic inspector. */
    private void showEditorFor(PreferencesNode node) {
        currentNode = node;
        if (node instanceof ConnectorStylePreferences) {
            if (connectorEditor == null) {
                connectorEditor = new ConnectorStyleEditorPanel((ConnectorStylePreferences) node);
                rightPane.add(connectorEditor, CARD_CONNECTORS);
            }
            else {
                connectorEditor.setEditedObject((ConnectorStylePreferences) node);
            }
            rightCards.show(rightPane, CARD_CONNECTORS);
        }
        else {
            inspector.inspectObject(node);
            rightCards.show(rightPane, CARD_INSPECTOR);
        }
        updateButtons();
    }

    private void onReset() {
        if (currentNode == null) {
            return;
        }
        PreferencesManager.getInstance().resetNodeToDefaults(currentNode);
        attachWorkingListeners(); // a re-seeded connector list has new style objects to observe
        showEditorFor(currentNode); // rebind the right pane to reflect the reset values
    }

    private void onCancel() {
        PreferencesManager mgr = PreferencesManager.getInstance();
        mgr.cancelToSaved();
        // Rebuild the working copy from the (reverted) live model and rebind the UI.
        workingModel = mgr.createWorkingCopy();
        observed.clear();
        browser.setEditedObject(workingModel);
        connectorEditor = null;
        attachWorkingListeners();
        SwingUtilities.invokeLater(this::selectFirstTheme);
    }

    /** Opens (or brings to front) the singleton preferences window. */
    public static void showPreferences(Frame owner, Function<String, String> localizer,
                                       DiagramRestyleHandler restyleHandler) {
        SwingUtilities.invokeLater(() -> {
            if (instance == null) {
                String title = localizer != null ? localizer.apply("preferences") : "Preferences";
                instance = new PreferencesDialog(owner, title, localizer, restyleHandler);
            }
            instance.selectFirstTheme();
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
        });
    }

    /**
     * After applying/saving new style defaults, offers to restyle the currently open diagrams.
     * Open diagrams keep their own embedded styles unless the user accepts.
     */
    private void maybePromptRestyle() {
        if (restyleHandler == null || !restyleHandler.hasOpenDiagrams()) {
            return;
        }
        int answer = javax.swing.JOptionPane.showConfirmDialog(this,
                loc("restyle_open_diagrams_question"), loc("preferences"),
                javax.swing.JOptionPane.YES_NO_OPTION);
        if (answer == javax.swing.JOptionPane.YES_OPTION) {
            restyleHandler.restyleOpenDiagrams();
        }
    }

    private void selectFirstTheme() {
        if (workingModel.getChildren() == null || workingModel.getChildren().isEmpty()) {
            return;
        }
        PreferencesNode first = workingModel.getChildren().get(0);
        browser.getController().setSelectedNode(first);
        showEditorFor(first);
    }

    private String loc(String key) {
        return localizer != null ? localizer.apply(key) : key;
    }
}
