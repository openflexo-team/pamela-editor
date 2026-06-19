package org.openflexo.pamela.editor.ui.preferences;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.function.Function;

import javax.swing.JDialog;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Two-pane preferences window (see {@code preferences-design.md §3}): a tree browser on the
 * left, an editable property editor on the right. Non-modal, created lazily and reused.
 * Edits are applied immediately (decision D4) — there is no OK/Cancel.
 */
public class PreferencesDialog extends JDialog {

    private static PreferencesDialog instance;

    private final PreferencesBrowser browser;
    private final PreferencesInspectorController inspector;

    private PreferencesDialog(Frame owner, String title, Function<String, String> localizer) {
        super(owner, title, false);

        browser = new PreferencesBrowser(PreferencesManager.getInstance().getModel());
        inspector = new PreferencesInspectorController();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                browser, inspector.getRootPane());
        split.setDividerLocation(220);
        split.setResizeWeight(0.3);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(split, BorderLayout.CENTER);
        setPreferredSize(new Dimension(720, 480));

        // Browser selection -> right-pane inspector (no apply button: immediate).
        // Use Gina's FIBSelectionListener (fired by the browser on every click, gina §9),
        // which is more reliable here than the two-way `selected` binding's PropertyChange.
        browser.getController().addSelectionListener(selection -> {
            if (selection != null && !selection.isEmpty()) {
                inspector.inspectObject(selection.get(0));
            }
        });

        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens (or brings to front) the singleton preferences window. */
    public static void showPreferences(Frame owner, Function<String, String> localizer) {
        SwingUtilities.invokeLater(() -> {
            if (instance == null) {
                String title = localizer != null ? localizer.apply("preferences") : "Preferences";
                instance = new PreferencesDialog(owner, title, localizer);
            }
            instance.selectFirstTheme();
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
        });
    }

    private void selectFirstTheme() {
        PamelaEditorPreferencesModel model = PreferencesManager.getInstance().getModel();
        if (browser.getController().getSelectedNode() == null
                && model.getChildren() != null && !model.getChildren().isEmpty()) {
            PreferencesNode first = model.getChildren().get(0);
            browser.getController().setSelectedNode(first);
            inspector.inspectObject(first);
        }
    }
}
