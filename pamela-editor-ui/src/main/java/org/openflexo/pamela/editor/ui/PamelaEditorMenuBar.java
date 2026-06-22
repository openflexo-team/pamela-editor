package org.openflexo.pamela.editor.ui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.openflexo.diana.control.DianaInteractiveEditor;
import org.openflexo.diana.swing.control.tools.JDianaDialogInspectors;
import org.openflexo.pamela.editor.ui.diagram.DianaDrawingEditor;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;
import org.openflexo.pamela.editor.ui.preferences.PreferenceChangeListener;
import org.openflexo.pamela.editor.ui.preferences.PreferencesNode;
import org.openflexo.pamela.editor.ui.preferences.RecentFilesPreferences;
import org.openflexo.pamela.undo.UndoManager;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * Application menu bar for the PAMELA editor.
 *
 * <p>File menu: open / save project, new diagram, quit.<br>
 * Edit menu: undo / redo, copy / cut / paste (wired to the active diagram editor).<br>
 * Tools menu: logs, localised string editor.</p>
 */
public class PamelaEditorMenuBar extends JMenuBar implements PreferenceChangeListener {

    private final JMenu fileMenu;
    private final JMenu editMenu;
    private final JMenu viewMenu;
    private final JMenu toolsMenu;
    private final JMenu helpMenu;

    // File menu items
    private final JMenuItem newProjectItem;
    private final JMenuItem openItem;
    private final JMenuItem newDiagramItem;
    private final JMenuItem saveItem;
    private final JMenuItem refreshItem;
    private final JMenuItem closeItem;
    private final JMenuItem quitItem;
    private final JMenu openRecent;

    // View menu items
    private final JMenuItem backItem;
    private final JMenuItem forwardItem;

    // Edit menu items (synchronised with the active diagram editor)
    final SynchronizedMenuItem copyItem;
    final SynchronizedMenuItem cutItem;
    final SynchronizedMenuItem pasteItem;
    final SynchronizedMenuItem undoItem;
    final SynchronizedMenuItem redoItem;

    // Tools menu items
    private final JMenuItem logsItem;
    private final JMenuItem localizedItem;

    private final PamelaEditorApplication application;

    public PamelaEditorMenuBar(PamelaEditorApplication application) {
        this.application = application;

        // ------------------------------------------------------------------ menus
        fileMenu  = new JMenu(loc("file"));
        editMenu  = new JMenu(loc("edit"));
        viewMenu  = new JMenu(loc("view"));
        toolsMenu = new JMenu(loc("tools"));
        helpMenu  = new JMenu(loc("help"));

        // ------------------------------------------------------------------ File menu
        newProjectItem = new JMenuItem(loc("new_project"));
        newProjectItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_N, PamelaEditorApplication.META_MASK));
        newProjectItem.addActionListener(e -> application.newProject());

        openItem = new JMenuItem(loc("open_project"));
        openItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_O, PamelaEditorApplication.META_MASK));
        openItem.addActionListener(e -> application.openProjectFromChooser());

        newDiagramItem = new JMenuItem(loc("new_diagram"));
        newDiagramItem.addActionListener(e -> application.newDiagram());

        openRecent = new JMenu(loc("open_recent"));
        PamelaEditorPreferences.addPreferenceChangeListener(this);
        updateOpenRecent();

        saveItem = new JMenuItem(loc("save_project"));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S, PamelaEditorApplication.META_MASK));
        saveItem.addActionListener(e -> application.saveActiveProject());

        // Refresh: re-run the source analysis so changes made on disk outside the
        // editor (e.g. a class added in the IDE) are picked up. F5 is the de-facto
        // refresh key and avoids the ⌘R / redo accelerator clash.
        refreshItem = new JMenuItem(loc("refresh_project"));
        refreshItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refreshItem.addActionListener(e -> application.refreshActiveProject());

        closeItem = new JMenuItem(loc("close"));
        closeItem.addActionListener(e -> {
            // Close the session that contains the currently selected element
        });

        quitItem = new JMenuItem(loc("quit"));
        quitItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Q, PamelaEditorApplication.META_MASK));
        quitItem.addActionListener(e -> application.quit());

        fileMenu.add(newProjectItem);
        fileMenu.addSeparator();
        fileMenu.add(openItem);
        fileMenu.add(openRecent);
        fileMenu.addSeparator();
        fileMenu.add(newDiagramItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(refreshItem);
        fileMenu.add(closeItem);
        fileMenu.addSeparator();
        fileMenu.add(quitItem);

        // ------------------------------------------------------------------ View menu
        backItem = new JMenuItem(loc("navigate_back"));
        backItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_LEFT, java.awt.event.InputEvent.ALT_DOWN_MASK));
        backItem.addActionListener(e -> application.navigateBack());

        forwardItem = new JMenuItem(loc("navigate_forward"));
        forwardItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_RIGHT, java.awt.event.InputEvent.ALT_DOWN_MASK));
        forwardItem.addActionListener(e -> application.navigateForward());

        viewMenu.add(backItem);
        viewMenu.add(forwardItem);
        viewMenu.addSeparator();
        addInspectorItems(viewMenu);

        // ------------------------------------------------------------------ Edit menu
        copyItem = makeSynchronizedMenuItem(
                "copy",
                PamelaEditorIconLibrary.COPY_ICON,
                KeyStroke.getKeyStroke(KeyEvent.VK_C,
                        PamelaEditorApplication.META_MASK),
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PamelaClassDiagramEditor ed = application.getActiveDiagramEditor();
                        if (ed != null) {
                            try { ed.getDianaEditor().copy(); }
                            catch (Exception ex) { ex.printStackTrace(); }
                        }
                    }
                },
                (observable, menuItem) -> {
                    if (observable instanceof DianaDrawingEditor) {
                        menuItem.setEnabled(
                                ((DianaDrawingEditor) observable).isCopiable());
                    }
                });

        cutItem = makeSynchronizedMenuItem(
                "cut",
                PamelaEditorIconLibrary.CUT_ICON,
                KeyStroke.getKeyStroke(KeyEvent.VK_X,
                        PamelaEditorApplication.META_MASK),
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PamelaClassDiagramEditor ed = application.getActiveDiagramEditor();
                        if (ed != null) {
                            try { ed.getDianaEditor().cut(); }
                            catch (Exception ex) { ex.printStackTrace(); }
                        }
                    }
                },
                (observable, menuItem) -> {
                    if (observable instanceof DianaDrawingEditor) {
                        menuItem.setEnabled(
                                ((DianaDrawingEditor) observable).isCutable());
                    }
                });

        pasteItem = makeSynchronizedMenuItem(
                "paste",
                PamelaEditorIconLibrary.PASTE_ICON,
                KeyStroke.getKeyStroke(KeyEvent.VK_V,
                        PamelaEditorApplication.META_MASK),
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PamelaClassDiagramEditor ed = application.getActiveDiagramEditor();
                        if (ed != null) {
                            try { ed.getDianaEditor().paste(); }
                            catch (Exception ex) { ex.printStackTrace(); }
                        }
                    }
                },
                (observable, menuItem) -> {
                    if (observable instanceof DianaInteractiveEditor) {
                        menuItem.setEnabled(
                                ((DianaInteractiveEditor<?, ?, ?>) observable).isPastable());
                    }
                });

        undoItem = makeSynchronizedMenuItem(
                "undo",
                PamelaEditorIconLibrary.UNDO_ICON,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                        PamelaEditorApplication.META_MASK),
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PamelaClassDiagramEditor ed = application.getActiveDiagramEditor();
                        if (ed != null) {
                            ed.getDianaEditor().undo();
                        }
                    }
                },
                (observable, menuItem) -> {
                    if (observable instanceof UndoManager) {
                        UndoManager um = (UndoManager) observable;
                        menuItem.setEnabled(um.canUndo());
                        menuItem.setText(um.canUndo()
                                ? um.getUndoPresentationName()
                                : loc("undo"));
                    }
                });

        redoItem = makeSynchronizedMenuItem(
                "redo",
                PamelaEditorIconLibrary.REDO_ICON,
                KeyStroke.getKeyStroke(KeyEvent.VK_R,
                        PamelaEditorApplication.META_MASK),
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PamelaClassDiagramEditor ed = application.getActiveDiagramEditor();
                        if (ed != null) {
                            ed.getDianaEditor().redo();
                        }
                    }
                },
                (observable, menuItem) -> {
                    if (observable instanceof UndoManager) {
                        UndoManager um = (UndoManager) observable;
                        menuItem.setEnabled(um.canRedo());
                        menuItem.setText(um.canRedo()
                                ? um.getRedoPresentationName()
                                : loc("redo"));
                    }
                });

        editMenu.add(copyItem);
        editMenu.add(cutItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(undoItem);
        editMenu.add(redoItem);

        // Preferences: on macOS this lives in the application menu (⌘,) via the Desktop
        // preferences handler; elsewhere add an explicit Edit > Preferences… item (Ctrl+,).
        if (!application.macPreferencesHandlerInstalled) {
            JMenuItem preferencesItem = new JMenuItem(loc("preferences"));
            preferencesItem.setAccelerator(KeyStroke.getKeyStroke(
                    KeyEvent.VK_COMMA, PamelaEditorApplication.META_MASK));
            preferencesItem.addActionListener(e -> application.showPreferences());
            editMenu.addSeparator();
            editMenu.add(preferencesItem);
        }

        // ------------------------------------------------------------------ Tools menu
        logsItem = new JMenuItem(loc("logs"));
        logsItem.addActionListener(e -> application.showLogs());

        localizedItem = new JMenuItem(loc("localized_editor"));
        localizedItem.addActionListener(e -> application.showLocalizedEditor());

        toolsMenu.add(logsItem);
        toolsMenu.add(localizedItem);

        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(toolsMenu);
        add(helpMenu);
    }

    // =========================================================================
    // Diana inspectors (View menu)
    // =========================================================================

    /**
     * Adds the Diana floating-inspector toggles directly at the root of the
     * given menu (the <i>View</i> menu): one item per inspector dialog
     * (location/size, shape, connector, foreground, background, text, shadow,
     * control, layout manager).
     *
     * <p>Each item is a {@link WindowMenuItem} whose checked state mirrors the
     * dialog visibility. The dialogs are created lazily here (first getter call)
     * and staggered near the top-right of the main frame so they do not stack on
     * top of one another. They follow the selection of the active diagram editor
     * because {@code PamelaEditorApplication.attachDianaTools(...)} calls
     * {@code inspectors.attachToEditor(...)} whenever a diagram view becomes
     * active.</p>
     */
    private void addInspectorItems(JMenu menu) {
        JDianaDialogInspectors insp = application.inspectors;

        int i = 0;
        i = addInspectorItem(menu, "location_size_inspector",
                insp.getLocationSizeInspector(), i);
        i = addInspectorItem(menu, "shape_inspector",
                insp.getShapeInspector(), i);
        i = addInspectorItem(menu, "connector_inspector",
                insp.getConnectorInspector(), i);
        menu.addSeparator();
        i = addInspectorItem(menu, "foreground_inspector",
                insp.getForegroundStyleInspector(), i);
        i = addInspectorItem(menu, "background_inspector",
                insp.getBackgroundStyleInspector(), i);
        i = addInspectorItem(menu, "text_inspector",
                insp.getTextPropertiesInspector(), i);
        i = addInspectorItem(menu, "shadow_inspector",
                insp.getShadowStyleInspector(), i);
        menu.addSeparator();
        i = addInspectorItem(menu, "control_inspector",
                insp.getControlInspector(), i);
        i = addInspectorItem(menu, "layout_manager_inspector",
                insp.getLayoutManagersInspector(), i);
    }

    /**
     * Adds one inspector toggle to {@code menu}, positioning the dialog at a
     * staggered offset {@code index} from the top-right of the frame, and
     * returns {@code index + 1}.
     */
    private int addInspectorItem(JMenu menu, String labelKey, Window dialog, int index) {
        // Stagger dialogs vertically along the right edge of the frame.
        int x = application.frame.getX() + application.frame.getWidth() - 360;
        int y = application.frame.getY() + 40 + index * 30;
        dialog.setLocation(Math.max(0, x), Math.max(0, y));
        menu.add(new WindowMenuItem(loc(labelKey), dialog));
        return index + 1;
    }

    // =========================================================================
    // Open recent
    // =========================================================================

    private boolean willUpdate = false;

    @Override
    public void preferenceChanged(PreferencesNode node, String key, Object oldValue, Object newValue) {
        if (node instanceof RecentFilesPreferences) {
            if (willUpdate) {
                return;
            }
            willUpdate = true;
            SwingUtilities.invokeLater(() -> {
                willUpdate = false;
                updateOpenRecent();
            });
        }
    }

    private void updateOpenRecent() {
        openRecent.removeAll();
        List<File> files = PamelaEditorPreferences.getLastFiles();
        openRecent.setEnabled(!files.isEmpty());
        for (File file : files) {
            JMenuItem item = new JMenuItem(file.getName());
            item.setToolTipText(file.getAbsolutePath());
            item.addActionListener(e -> application.openProject(file));
            openRecent.add(item);
        }
    }

    // =========================================================================
    // Synchronized menu item factory
    // =========================================================================

    SynchronizedMenuItem makeSynchronizedMenuItem(
            String actionName, Icon icon, KeyStroke accelerator,
            AbstractAction action, Synchronizer synchronizer) {
        String localizedName = loc(actionName);
        SynchronizedMenuItem returned = new SynchronizedMenuItem(localizedName, synchronizer);
        action.putValue(Action.NAME, localizedName);
        returned.setAction(action);
        returned.setIcon(icon);
        returned.setAccelerator(accelerator);
        application.frame.getRootPane()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(accelerator, actionName);
        application.frame.getRootPane()
                .getActionMap()
                .put(actionName, action);
        returned.setEnabled(false);
        return returned;
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private static String loc(String key) {
        return PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey(key);
    }

    // =========================================================================
    // Inner types
    // =========================================================================

    public interface Synchronizer {
        void synchronize(HasPropertyChangeSupport observable,
                         SynchronizedMenuItem menuItem);
    }

    public class SynchronizedMenuItem extends JMenuItem
            implements PropertyChangeListener {

        private HasPropertyChangeSupport observable;
        private final Synchronizer synchronizer;

        public SynchronizedMenuItem(String menuName, Synchronizer synchronizer) {
            super(menuName);
            this.synchronizer = synchronizer;
        }

        public void synchronizeWith(HasPropertyChangeSupport anObservable) {
            if (this.observable != null) {
                application.manager.removeListener(this, this.observable);
            }
            application.manager.addListener(this, anObservable);
            observable = anObservable;
            synchronizer.synchronize(observable, this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            synchronizer.synchronize(observable, this);
        }

        @Override
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            if (getAction() != null) {
                getAction().setEnabled(b);
            }
        }
    }

    public class WindowMenuItem extends JCheckBoxMenuItem
            implements WindowListener {

        private final Window window;

        public WindowMenuItem(String menuName, Window aWindow) {
            super(menuName);
            this.window = aWindow;
            addActionListener(e -> window.setVisible(!window.isVisible()));
            aWindow.addWindowListener(this);
        }

        @Override public void windowOpened(WindowEvent e)      { setState(window.isVisible()); }
        @Override public void windowIconified(WindowEvent e)   {}
        @Override public void windowDeiconified(WindowEvent e) {}
        @Override public void windowDeactivated(WindowEvent e) { setState(window.isVisible()); }
        @Override public void windowClosing(WindowEvent e)     { setState(window.isVisible()); }
        @Override public void windowClosed(WindowEvent e)      { setState(window.isVisible()); }
        @Override public void windowActivated(WindowEvent e)   { setState(window.isVisible()); }
    }
}
