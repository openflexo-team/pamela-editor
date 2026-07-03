package org.openflexo.pamela.editor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.diana.swing.control.tools.JDianaDialogInspectors;
import org.openflexo.diana.swing.control.tools.JDianaLayoutWidget;
import org.openflexo.diana.swing.control.tools.JDianaScaleSelector;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.localization.LocalizedEditor;
import org.openflexo.gina.swing.utils.logging.FlexoLoggingViewer;
import org.openflexo.localization.FlexoLocalization;
import org.openflexo.localization.LocalizedDelegate;
import org.openflexo.localization.LocalizedDelegateImpl;
import org.openflexo.logging.FlexoLogger;
import org.openflexo.logging.FlexoLoggingManager;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramSerializer;
import org.openflexo.pamela.editor.model.SourceCustomMethod;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceImplementationClass;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.action.ActionGroup;
import org.openflexo.pamela.editor.ui.action.AddAsRootTypeAction;
import org.openflexo.pamela.editor.ui.action.AddSourceFolderAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsAdderAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsFinderAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsInitializerAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsOperationAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsReindexerAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsRemoverAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsSetterAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsUpdaterAction;
import org.openflexo.pamela.editor.ui.action.PromotableMethod;
import org.openflexo.pamela.editor.ui.action.PromoteGetterAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsPamelaEntityAction;
import org.openflexo.pamela.editor.ui.action.NewEntityAction;
import org.openflexo.pamela.editor.ui.action.RenameEntityAction;
import org.openflexo.pamela.editor.ui.action.DeleteEntityAction;
import org.openflexo.pamela.editor.ui.action.AddSuperEntityAction;
import org.openflexo.pamela.editor.ui.action.RemoveSuperEntityAction;
import org.openflexo.pamela.editor.ui.action.NewPropertyAction;
import org.openflexo.pamela.editor.ui.action.PromoteMethodAction;
import org.openflexo.pamela.editor.ui.action.RenamePropertyAction;
import org.openflexo.pamela.editor.ui.action.ChangePropertyTypeAction;
import org.openflexo.pamela.editor.ui.action.AddSetterAction;
import org.openflexo.pamela.editor.ui.action.RemoveSetterAction;
import org.openflexo.pamela.editor.ui.action.AddAdderRemoverAction;
import org.openflexo.pamela.editor.ui.action.RemoveAdderRemoverAction;
import org.openflexo.pamela.editor.ui.action.DeletePropertyAction;
import org.openflexo.pamela.editor.ui.action.NewClassDiagramAction;
import org.openflexo.pamela.editor.ui.action.RenameClassDiagramAction;
import org.openflexo.pamela.editor.ui.action.DeleteClassDiagramAction;
import org.openflexo.pamela.editor.ui.action.RemoveFromDiagramAction;
import org.openflexo.pamela.editor.ui.action.HideMemberAction;
import org.openflexo.pamela.editor.ui.action.ShowMemberAction;
import org.openflexo.pamela.editor.ui.action.ShowAllMembersAction;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramDrawing;
import org.openflexo.pamela.editor.ui.action.ShowSourceCodeAction;
import org.openflexo.pamela.editor.ui.action.ContextualAction;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;
import org.openflexo.pamela.editor.ui.widget.ContextPanel;
import org.openflexo.pamela.editor.ui.widget.DetailedBrowser;
import org.openflexo.pamela.editor.ui.widget.MetaModelBrowser;
import org.openflexo.pamela.editor.ui.widget.MetaModelSummaryView;
import org.openflexo.pamela.editor.ui.widget.PackageSummaryView;
import org.openflexo.pamela.editor.ui.widget.PamelaEditorInspectorController;
import org.openflexo.pamela.editor.ui.widget.JavaFileView;
import org.openflexo.pamela.editor.ui.widget.SourceCodeView;
import org.openflexo.pamela.editor.ui.widget.SourceFolderSummaryView;
import org.openflexo.pamela.editor.ui.widget.ValidationPanel;
import org.openflexo.rm.FileSystemResourceLocatorImpl;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;
import org.openflexo.swing.ComponentBoundSaver;
import org.openflexo.swing.FlexoFileChooser;
import org.openflexo.swing.layout.JXMultiSplitPane;
import org.openflexo.swing.layout.JXMultiSplitPane.DividerPainter;
import org.openflexo.swing.layout.MultiSplitLayout;
import org.openflexo.swing.layout.MultiSplitLayout.Divider;
import org.openflexo.swing.layout.MultiSplitLayout.Leaf;
import org.openflexo.swing.layout.MultiSplitLayout.Node;
import org.openflexo.swing.layout.MultiSplitLayout.Split;
import org.openflexo.swing.layout.MultiSplitLayoutFactory;
import org.openflexo.toolbox.PropertyChangeListenerRegistrationManager;
import org.openflexo.toolbox.ToolBox;

/**
 * Main application class for the PAMELA editor.
 *
 * <p>Layout: three-column {@link JXMultiSplitPane}.
 * <ul>
 *   <li>LEFT top    — {@link MetaModelBrowser} (project tree)</li>
 *   <li>LEFT bottom — {@link DetailedBrowser} (content of selected element)</li>
 *   <li>CENTER      — single contextual view with Back / Forward navigation history</li>
 *   <li>RIGHT top   — inspector (dynamically-swapped FIB panel)</li>
 *   <li>RIGHT bottom — validation panel (placeholder)</li>
 * </ul>
 * </p>
 *
 * <p>Selection model: a single {@link #currentSelectedElement} ({@code Object}).
 * Changing it updates the detailed browser, the inspector, and the central view.</p>
 *
 * <p>Central view navigation: clicking any element replaces the central view and
 * pushes the previous element onto the back-history stack.  The Back (←) and
 * Forward (→) buttons navigate through that history.</p>
 */
public class PamelaEditorApplication implements org.openflexo.toolbox.HasPropertyChangeSupport {

    private static final Logger logger =
            FlexoLogger.getLogger(PamelaEditorApplication.class.getPackage().getName());

    private final java.beans.PropertyChangeSupport pcSupport =
            new java.beans.PropertyChangeSupport(this);

    @Override
    public java.beans.PropertyChangeSupport getPropertyChangeSupport() {
        return pcSupport;
    }

    @Override
    public String getDeletedProperty() {
        return null;
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    public static final LocalizedDelegate PAMELA_EDITOR_LOCALIZATION =
            new LocalizedDelegateImpl(
                    ResourceLocator.locateResource("PamelaLocalization/PamelaEditor"),
                    FlexoLocalization.getMainLocalizer(), true, true);

    static final int META_MASK =
            ToolBox.isMacOS() ? InputEvent.META_MASK : InputEvent.CTRL_MASK;

    protected static final MultiSplitLayoutFactory MSL_FACTORY =
            new MultiSplitLayoutFactory.DefaultMultiSplitLayoutFactory();

    protected static final int KNOB_SIZE = 5;
    protected static final int KNOB_SPACE = 2;
    protected static final int DIVIDER_SIZE = KNOB_SIZE + 2 * KNOB_SPACE;
    protected static final int DIVIDER_KNOB_SIZE = 3 * KNOB_SIZE + 2 * KNOB_SPACE;

    protected static final Paint KNOB_PAINTER = new RadialGradientPaint(
            new Point((KNOB_SIZE - 1) / 2, (KNOB_SIZE - 1) / 2),
            (KNOB_SIZE - 1) / 2,
            new float[]{0.0f, 1.0f},
            new Color[]{Color.GRAY, Color.LIGHT_GRAY});

    // -------------------------------------------------------------------------
    // Layout position / column enums
    // -------------------------------------------------------------------------

    public enum LayoutPosition {
        TOP_LEFT, BOTTOM_LEFT, CENTER, TOP_RIGHT, BOTTOM_RIGHT
    }

    public enum LayoutColumns {
        LEFT, CENTER, RIGHT
    }

    // -------------------------------------------------------------------------
    // Swing frame
    // -------------------------------------------------------------------------

    final JFrame frame;
    private JXMultiSplitPane splitPane;

    // Status bar (bottom of the main window): progress bar + message, shown during
    // background project load / rebuild (Spoon analysis).
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusLabel;
    private int activeBuilds; // ref-count so concurrent loads don't hide the bar early

    private final SwingToolFactory toolFactory;
    private final FlexoFileChooser fileChooser;
    private final FileSystemResourceLocatorImpl resourceLocator;

    // -------------------------------------------------------------------------
    // Diana toolbar (top of CENTER column)
    // -------------------------------------------------------------------------

    private final JDianaScaleSelector scaleSelector;
    private final JDianaLayoutWidget layoutWidget;
    final JDianaDialogInspectors inspectors;
    private final JPanel toolbarPanel;

    // -------------------------------------------------------------------------
    // Left column
    // -------------------------------------------------------------------------

    private final MetaModelBrowser metaModelBrowser;
    private final DetailedBrowser detailedBrowser;

    // -------------------------------------------------------------------------
    // Central column — single view with Back/Forward navigation
    // -------------------------------------------------------------------------

    /** The swappable content area: holds the currently displayed view. */
    private final JPanel centralViewPanel;

    /**
     * The center column's own container. Its {@code CENTER} slot is swapped between
     * {@link #centralViewPanel} alone (no project open) and {@link #centerSplit} (validation
     * strip present) — see {@link #applyValidationPanelLayout()}.
     */
    private final JPanel centerColumn;

    /**
     * Bottom-of-center-column validation strip (validation-log-panel-design.md).
     * Shows the active project's issues.
     */
    private final ValidationPanel validationPanel;

    /**
     * Vertical split holding {@link #centralViewPanel} (top) and {@link #validationPanel}
     * (bottom), {@code oneTouchExpandable} so the divider carries the native Swing
     * collapse/expand arrows.
     */
    private final JSplitPane centerSplit;

    /** Default fraction of the center column's height given to the validation strip. */
    private static final double VALIDATION_PANEL_DIVIDER_FRACTION = 0.75;

    /** Below this bottom-pane height (px), the divider is considered "collapsed". */
    private static final int VALIDATION_PANEL_COLLAPSE_THRESHOLD = 4;

    /**
     * Whether the validation strip is currently expanded. Kept in sync with
     * {@link #centerSplit}'s divider position by a {@code DIVIDER_LOCATION_PROPERTY} listener
     * (installed in the constructor), so it reflects a one-touch-arrow click, a manual drag,
     * or {@link #setValidationPanelVisible(boolean)} uniformly.
     */
    private boolean validationPanelVisible = true;

    /** Remembers the divider position across a collapse/expand cycle; -1 = not yet set. */
    private int lastValidationDividerLocation = -1;

    /** Navigation bar: Back button, title label, Forward button. */
    private final JButton backButton;
    private final JButton forwardButton;
    private final JLabel centralTitleLabel;

    /**
     * Navigation history — back stack (most-recently visited element at top).
     * When the user navigates to a new element, the previous element is pushed here.
     */
    private final Deque<Object> backHistory = new ArrayDeque<>();

    /**
     * Navigation history — forward stack.
     * Populated when the user navigates backward; cleared when a new element is visited.
     */
    private final Deque<Object> forwardHistory = new ArrayDeque<>();

    /**
     * The model element whose view is currently shown in the central panel.
     * Null when nothing is shown.
     */
    private Object currentHistoryElement = null;

    /**
     * Cache of already-created views: element identity → Swing component.
     * Avoids recreating expensive views (Spoon source code, Diana diagram) on
     * every back/forward navigation.
     */
    private final Map<Object, JComponent> viewCache = new IdentityHashMap<>();

    /**
     * Map from {@link PamelaClassDiagram} → its editor (for Diana tool
     * attachment when the diagram view becomes active).
     */
    private final Map<PamelaClassDiagram, PamelaClassDiagramEditor> diagramEditors =
            new IdentityHashMap<>();

    // -------------------------------------------------------------------------
    // Right column
    // -------------------------------------------------------------------------

    /** Inspector controller — manages .inspector files and type-dispatch. */
    private final PamelaEditorInspectorController inspectorController;

    /** Context-sensitive complementary panel (right-bottom, ui-design.md §19). */
    private final ContextPanel contextPanel;

    // -------------------------------------------------------------------------
    // Application state
    // -------------------------------------------------------------------------

    /** Open projects (projects). Exposed to the MetaModelBrowser FIB as {@code data.projects}. */
    private final List<PamelaProject> projects = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Contextual action registry
    // -------------------------------------------------------------------------

    /**
     * All registered contextual actions, in registration order.
     * Filtered by {@link #getActionsFor(Object)} on each right-click.
     */
    private final List<ContextualAction> registeredActions = new ArrayList<>();

    /**
     * Registers a contextual action so it appears in right-click menus when
     * {@link ContextualAction#isApplicable(Object)} returns {@code true}.
     */
    public void registerAction(ContextualAction action) {
        registeredActions.add(action);
    }

    /**
     * Returns the subset of registered actions applicable to {@code target},
     * in registration order.
     */
    public List<ContextualAction> getActionsFor(Object target) {
        if (target == null) {
            return Collections.emptyList();
        }
        List<ContextualAction> result = new ArrayList<>();
        for (ContextualAction action : registeredActions) {
            if (action.isApplicable(target)) {
                result.add(action);
            }
        }
        return result;
    }

    /**
     * Builds and shows a {@link JPopupMenu} for an ordered list of target facets,
     * using the registered contextual actions. Shared by the browsers (single facet)
     * and the diagram (e.g. {@code [entity, entityView]} so an entity box offers the
     * browser's entity actions <em>and</em> the diagram-specific "Remove from diagram"
     * — see {@code ui-design.md §18.4}).
     *
     * <p>For each facet, in order, the applicable actions are appended; an action that
     * matches several facets is added once (bound to the first facet that matched). The
     * menu is not shown when no action applies. When {@code invoker} is {@code null}
     * (no source component), the menu is shown at screen coordinates {@code (x, y)}.</p>
     */
    public void showContextualMenuFor(List<Object> targetFacets, Component invoker, int x, int y) {
        if (targetFacets == null || targetFacets.isEmpty()) {
            return;
        }
        // 1. Collect applicable (action, boundFacet) pairs, deduped by action identity;
        //    the first facet that matches an action binds it (preserves §18.4 behaviour).
        Map<ContextualAction, Object> boundFacet = new IdentityHashMap<>();
        List<ContextualAction> ordered = new ArrayList<>();
        for (Object facet : targetFacets) {
            if (facet == null) {
                continue;
            }
            for (ContextualAction action : getActionsFor(facet)) {
                if (boundFacet.containsKey(action)) {
                    continue;
                }
                boundFacet.put(action, facet);
                ordered.add(action);
            }
        }
        if (ordered.isEmpty()) {
            return;
        }

        // 2. Sort by (group order, registration order) — a stable secondary key keeps the
        //    intra-group order as registered.
        ordered.sort(Comparator
                .comparingInt((ContextualAction a) -> a.getGroup().ordinal())
                .thenComparingInt(registeredActions::indexOf));

        // 3. Render: a divider between groups, submenu routing for submenu groups (a submenu
        //    group with a single applicable action collapses to an inline item).
        JPopupMenu menu = new JPopupMenu();
        boolean firstGroup = true;
        int i = 0;
        while (i < ordered.size()) {
            ActionGroup group = ordered.get(i).getGroup();
            int j = i;
            while (j < ordered.size() && ordered.get(j).getGroup() == group) {
                j++;
            }
            if (!firstGroup) {
                menu.addSeparator();
            }
            firstGroup = false;

            if (group.isSubmenu() && j - i >= 2) {
                JMenu submenu = new JMenu(loc(group.getSubmenuLabelKey()));
                for (int k = i; k < ordered.size() && k < j; k++) {
                    ContextualAction action = ordered.get(k);
                    submenu.add(makeMenuItem(action, boundFacet.get(action)));
                }
                menu.add(submenu);
            } else {
                for (int k = i; k < j; k++) {
                    ContextualAction action = ordered.get(k);
                    menu.add(makeMenuItem(action, boundFacet.get(action)));
                }
            }
            i = j;
        }

        if (invoker != null) {
            menu.show(invoker, x, y);
        } else {
            // No source component (rare browser fallback): show detached at screen pos.
            menu.setInvoker(menu);
            menu.setLocation(x, y);
            menu.setVisible(true);
        }
    }

    /** Builds a menu item for an action bound to the facet that matched it. */
    private JMenuItem makeMenuItem(ContextualAction action, Object facet) {
        JMenuItem item = new JMenuItem(action.getLabel());
        if (action.getIcon() != null) {
            item.setIcon(action.getIcon());
        }
        item.addActionListener(e -> action.perform(facet, this));
        return item;
    }

    /** The currently selected element (any Source* or PamelaClassDiagram). */
    private Object currentSelectedElement;

    /**
     * Re-entrance guard for {@link #setCurrentSelectedElement(Object)}.
     * Prevents Gina's internal rebind machinery from triggering a recursive selection
     * propagation (e.g. when Gina calls {@code setSelectedElement(null)} on the
     * DetailedBrowser controller because the previously-selected node is not found
     * in the newly-bound tree).
     */
    private boolean settingSelectedElement = false;

    // --- Browser drag-to-diagram coordination ---
    // While the user holds the mouse down on the MetaModelBrowser tree (a potential
    // drag), the central view switch is deferred so the active diagram stays visible
    // and droppable. The switch is committed on mouse release only if no drag occurred.
    /** True between mousePressed and mouseReleased/dragDropEnd on the browser tree. */
    private boolean browserPressActive = false;
    /** True if a drag gesture started during the current browser press. */
    private boolean browserDragOccurred = false;
    /** Element whose central-view switch was deferred during a browser press. */
    private Object deferredCentralViewElement;

    private PamelaEditorMenuBar menuBar;

    /** True when the macOS application-menu Preferences handler is installed (see §6). */
    boolean macPreferencesHandlerInstalled;
    LocalizedEditor localizedEditor;

    /** Used by {@link PamelaEditorMenuBar} to manage PropertyChange listener registrations. */
    final PropertyChangeListenerRegistrationManager manager =
            new PropertyChangeListenerRegistrationManager();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public PamelaEditorApplication() {
        // --- Preferences (must precede the first PamelaEditorPreferences access) ---
        org.openflexo.pamela.editor.ui.preferences.PreferencesManager.initialize(
                PAMELA_EDITOR_LOCALIZATION::localizedForKey);

        // Changing the custom-method visibility filter (Analysis preference) must refresh the
        // operations shown in the browsers and diagrams → rebuild every open project, which
        // re-reads the filter (currentCustomMethodFilter) during analysis.
        org.openflexo.pamela.editor.ui.preferences.PreferencesManager.getInstance()
                .addPreferenceChangeListener((node, key, oldValue, newValue) -> {
                    if (org.openflexo.pamela.editor.ui.preferences.AnalysisPreferences
                            .CUSTOM_METHOD_FILTER.equals(key)) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            for (PamelaProject p : new ArrayList<>(projects)) {
                                rebuildProject(p);
                            }
                        });
                    }
                });

        // --- Frame ---
        frame = new JFrame("PAMELA Editor");
        frame.setBounds(PamelaEditorPreferences.getFrameBounds());
        new ComponentBoundSaver(frame) {
            @Override
            public void saveBounds(Rectangle bounds) {
                PamelaEditorPreferences.setFrameBounds(bounds);
            }
        };
        frame.setPreferredSize(new Dimension(1300, 900));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        // --- Resource locator ---
        resourceLocator = new FileSystemResourceLocatorImpl();
        if (PamelaEditorPreferences.getLastDirectory() != null) {
            resourceLocator.appendToDirectories(
                    PamelaEditorPreferences.getLastDirectory().getAbsolutePath());
        }
        resourceLocator.appendToDirectories(System.getProperty("user.home"));
        ResourceLocator.appendDelegate(resourceLocator);

        // --- File chooser ---
        fileChooser = new FlexoFileChooser(frame);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "PAMELA project files (*.pamela)", "pamela"));
        if (PamelaEditorPreferences.getLastDirectory() != null) {
            fileChooser.setCurrentDirectory(PamelaEditorPreferences.getLastDirectory());
        }

        // --- Diana tool factory ---
        toolFactory = new SwingToolFactory(frame);

        // --- Diana toolbar widgets ---
        // Only the layout widget and the scale selector are kept in the diagram header
        // to keep the view lightweight. The tool selector and the styles editor are not shown.
        scaleSelector = toolFactory.makeDianaScaleSelector(null);
        layoutWidget  = toolFactory.makeDianaLayoutWidget();
        inspectors    = toolFactory.makeDianaDialogInspectors();

        toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbarPanel.add(layoutWidget.getComponent());
        toolbarPanel.add(scaleSelector.getComponent());
        toolbarPanel.setVisible(false); // hidden until a diagram view is active

        // --- Central column: navigation bar ---
        backButton = new JButton("←");
        backButton.setFont(backButton.getFont().deriveFont(Font.BOLD));
        backButton.setToolTipText("Navigate back");
        backButton.setEnabled(false);
        backButton.addActionListener(e -> navigateBack());

        forwardButton = new JButton("→");
        forwardButton.setFont(forwardButton.getFont().deriveFont(Font.BOLD));
        forwardButton.setToolTipText("Navigate forward");
        forwardButton.setEnabled(false);
        forwardButton.addActionListener(e -> navigateForward());

        centralTitleLabel = new JLabel("", JLabel.CENTER);
        centralTitleLabel.setFont(centralTitleLabel.getFont().deriveFont(Font.BOLD));

        JPanel navButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        navButtonsPanel.add(backButton);
        navButtonsPanel.add(forwardButton);

        JPanel navigationBar = new JPanel(new BorderLayout());
        navigationBar.add(navButtonsPanel, BorderLayout.WEST);
        navigationBar.add(centralTitleLabel, BorderLayout.CENTER);

        // --- Central column: top bar (navigation + optional Diana toolbar) ---
        JPanel topBarPanel = new JPanel(new BorderLayout());
        topBarPanel.add(navigationBar, BorderLayout.NORTH);
        topBarPanel.add(toolbarPanel,  BorderLayout.SOUTH);

        // --- Central column: swappable content area ---
        centralViewPanel = new JPanel(new BorderLayout());

        // --- Central column: bottom validation strip (validation-log-panel-design.md) ---
        validationPanel = new ValidationPanel(null);
        validationPanel.setApplication(this);
        // Allows the one-touch arrow to collapse the strip fully to 0 height (otherwise it
        // would stop at the FIB's own layout-computed minimum).
        validationPanel.setMinimumSize(new java.awt.Dimension(0, 0));

        centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centralViewPanel, validationPanel);
        centerSplit.setResizeWeight(1.0); // extra height on resize goes to the central view
        centerSplit.setContinuousLayout(true);
        centerSplit.setOneTouchExpandable(true);
        centerSplit.setBorder(null);
        // Tracks validationPanelVisible from ANY divider move — one-touch arrow, manual drag,
        // or setValidationPanelVisible(...) itself — as a single source of truth.
        centerSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            int newLocation = (Integer) evt.getNewValue();
            boolean newVisible = !isValidationDividerCollapsed(newLocation);
            if (newVisible != validationPanelVisible) {
                if (!newVisible && evt.getOldValue() instanceof Integer) {
                    // Collapsing: remember the position we're collapsing FROM, so a later
                    // expand (menu, or the other one-touch arrow) restores it.
                    int oldLocation = (Integer) evt.getOldValue();
                    if (!isValidationDividerCollapsed(oldLocation)) {
                        lastValidationDividerLocation = oldLocation;
                    }
                }
                boolean oldVisible = validationPanelVisible;
                validationPanelVisible = newVisible;
                pcSupport.firePropertyChange("validationPanelVisible", oldVisible, newVisible);
            }
        });

        centerColumn = new JPanel(new BorderLayout());
        centerColumn.add(topBarPanel, BorderLayout.NORTH);
        centerColumn.add(centralViewPanel, BorderLayout.CENTER);
        // No project is open yet — the strip starts absent (applyValidationPanelLayout below).

        // --- Left column: MetaModelBrowser (top) ---
        metaModelBrowser = new MetaModelBrowser(this);
        // Wire selection: MetaModelBrowser → application
        metaModelBrowser.getController().getPropertyChangeSupport()
                .addPropertyChangeListener("selectedElement", evt ->
                        onBrowserSelectionChanged(evt.getNewValue()));
        // Enable dragging entities from the browser onto class diagrams.
        // Deferred so the Gina browser JTree is realised before we look it up.
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (!metaModelBrowser.enableEntityDrag()) {
                logger.warning("Could not enable entity drag: browser JTree not found");
            }
        });

        // --- Left column: DetailedBrowser (bottom) ---
        detailedBrowser = new DetailedBrowser(this);

        // --- Right column: inspector controller (top) ---
        inspectorController = new PamelaEditorInspectorController();

        // --- Right column: context panel (bottom, ui-design.md §19) ---
        contextPanel = new ContextPanel(this);

        // --- Multi-split layout ---
        Split<?> defaultLayout = getDefaultLayout();
        MultiSplitLayout splitLayout =
                new MultiSplitLayout(true, MSL_FACTORY);
        splitLayout.setLayoutMode(MultiSplitLayout.NO_MIN_SIZE_LAYOUT);
        splitLayout.setModel(defaultLayout);

        splitPane = new JXMultiSplitPane(splitLayout);
        splitPane.setDividerSize(DIVIDER_SIZE);
        splitPane.setDividerPainter(new DividerPainter() {
            @Override
            protected void doPaint(Graphics2D g, Divider divider,
                                   int width, int height) {
                if (!divider.isVisible()) {
                    return;
                }
                if (divider.isVertical()) {
                    int x = (width - KNOB_SIZE) / 2;
                    int y = (height - DIVIDER_KNOB_SIZE) / 2;
                    for (int i = 0; i < 3; i++) {
                        Graphics2D gr = (Graphics2D) g.create(
                                x, y + i * (KNOB_SIZE + KNOB_SPACE),
                                KNOB_SIZE + 1, KNOB_SIZE + 1);
                        gr.setPaint(KNOB_PAINTER);
                        gr.fillOval(0, 0, KNOB_SIZE, KNOB_SIZE);
                    }
                } else {
                    int x = (width - DIVIDER_KNOB_SIZE) / 2;
                    int y = (height - KNOB_SIZE) / 2;
                    for (int i = 0; i < 3; i++) {
                        Graphics2D gr = (Graphics2D) g.create(
                                x + i * (KNOB_SIZE + KNOB_SPACE), y,
                                KNOB_SIZE + 1, KNOB_SIZE + 1);
                        gr.setPaint(KNOB_PAINTER);
                        gr.fillOval(0, 0, KNOB_SIZE, KNOB_SIZE);
                    }
                }
            }
        });

        splitPane.add(metaModelBrowser,  LayoutPosition.TOP_LEFT.name());
        splitPane.add(detailedBrowser,   LayoutPosition.BOTTOM_LEFT.name());
        splitPane.add(centerColumn,      LayoutPosition.CENTER.name());
        splitPane.add(inspectorController.getRootPane(), LayoutPosition.TOP_RIGHT.name());
        splitPane.add(contextPanel,      LayoutPosition.BOTTOM_RIGHT.name());

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(buildStatusBar(), BorderLayout.SOUTH);

        // --- Menu bar ---
        macPreferencesHandlerInstalled = installMacPreferencesHandler();
        menuBar = new PamelaEditorMenuBar(this);
        frame.setJMenuBar(menuBar);

        // --- Contextual actions ---
        registerAction(new AddSourceFolderAction());
        registerAction(new AddAsRootTypeAction());
        registerAction(new DeclareAsPamelaEntityAction());
        // --- Model-editing actions (entities) — model-editing-design.md Lot 1 ---
        registerAction(new NewEntityAction());
        registerAction(new RenameEntityAction());
        registerAction(new DeleteEntityAction());
        registerAction(new AddSuperEntityAction());
        registerAction(new RemoveSuperEntityAction());
        // --- Model-editing actions (properties) — model-editing-design.md Lot 2 ---
        registerAction(new NewPropertyAction());
        registerAction(new PromoteMethodAction());
        registerAction(new RenamePropertyAction());
        registerAction(new ChangePropertyTypeAction());
        // --- Method-level "promote" actions (signature-based) — model-editing-design.md §3.3 ---
        registerAction(new PromoteGetterAction());
        registerAction(new DeclareAsSetterAction());
        registerAction(new DeclareAsUpdaterAction());
        registerAction(new DeclareAsAdderAction());
        registerAction(new DeclareAsRemoverAction());
        registerAction(new DeclareAsReindexerAction());
        registerAction(new DeclareAsOperationAction());
        registerAction(new DeclareAsInitializerAction());
        registerAction(new DeclareAsFinderAction());
        // --- Accessor toggles — model-editing-design.md Lot 3 ---
        registerAction(new AddSetterAction());
        registerAction(new RemoveSetterAction());
        registerAction(new AddAdderRemoverAction());
        registerAction(new RemoveAdderRemoverAction());
        registerAction(new DeletePropertyAction());
        registerAction(new NewClassDiagramAction());
        registerAction(new RenameClassDiagramAction());
        registerAction(new DeleteClassDiagramAction());
        registerAction(new ShowSourceCodeAction());
        registerAction(new RemoveFromDiagramAction());
        registerAction(new HideMemberAction(this));
        registerAction(new ShowMemberAction(this));
        registerAction(new ShowAllMembersAction(this, PamelaClassDiagramDrawing.Compartment.PROPERTIES));
        registerAction(new ShowAllMembersAction(this, PamelaClassDiagramDrawing.Compartment.INITIALIZERS));
        registerAction(new ShowAllMembersAction(this, PamelaClassDiagramDrawing.Compartment.METHODS));

        frame.validate();
        frame.pack();
    }

    // =========================================================================
    // Session management (exposed to FIB as data.projects)
    // =========================================================================

    /**
     * Returns the list of open projects.
     * <p>This is the root of the MetaModelBrowser tree via
     * {@code data.projects} in the FIB file.</p>
     */
    public List<PamelaProject> getProjects() {
        return Collections.unmodifiableList(projects);
    }

    /**
     * Returns the {@link SourceMetaModel} of the currently active project
     * (the one owning {@link #currentHistoryElement}), or {@code null} if none.
     */
    public SourceMetaModel getActiveMetaModel() {
        for (PamelaProject project : projects) {
            if (isElementInProject(currentHistoryElement, project)) {
                return project.getMetaModel();
            }
        }
        // Fallback: return the first project's metamodel if available
        return projects.isEmpty() ? null : projects.get(0).getMetaModel();
    }

    private boolean isElementInProject(Object element, PamelaProject project) {
        if (element == null) return false;
        SourceMetaModel mm = project.getMetaModel();
        if (mm == null) return false;
        if (element == mm) return true;
        if (element instanceof SourceFolder) {
            return mm.getSourceFolders().contains(element);
        }
        if (element instanceof SourcePackage) {
            return mm.getAllPackages().contains(element);
        }
        if (element instanceof SourceModelEntity) {
            return mm.getEntities().containsValue((SourceModelEntity) element);
        }
        if (element instanceof SourceJavaFile) {
            SourceJavaFile f = (SourceJavaFile) element;
            for (SourcePackage pkg : mm.getAllPackages()) {
                if (pkg.getJavaFiles().contains(f)) return true;
            }
        }
        if (project.getDiagrams() != null && project.getDiagrams().contains(element)) return true;
        return false;
    }

    /**
     * Scrolls the currently active {@link SourceCodeView} to the given 1-based line.
     * Called by the Spoon Outline panel when the user clicks a member (ui-design.md §19.3).
     */
    public void scrollSourceViewToLine(int line) {
        if (currentHistoryElement == null) return;
        JComponent view = viewCache.get(currentHistoryElement);
        if (view instanceof SourceCodeView) {
            // Clear any property highlight that may have been set by the DetailedBrowser
            // so that a click in the SpoonOutlineView starts from a clean state.
            ((SourceCodeView) view).clearHighlights();
            ((SourceCodeView) view).scrollToLine(line);
        } else if (view instanceof JavaFileView) {
            ((JavaFileView) view).scrollToLine(line);
        }
    }

    /**
     * Opens a {@code .pamela} project file.
     *
     * <p>The Spoon analysis ({@link SourceMetaModelSerializer#load}) is run on a
     * background thread so the EDT stays responsive. The browser is refreshed
     * on the EDT once the analysis completes.</p>
     */
    public void openProject(File pamelaFile) {
        if (pamelaFile == null || !pamelaFile.exists()) {
            return;
        }

        // Run Spoon analysis on a background thread to avoid blocking the EDT.
        buildStarted("Opening " + pamelaFile.getName() + "…");
        new javax.swing.SwingWorker<PamelaProject, String>() {

            @Override
            protected PamelaProject doInBackground() throws Exception {
                // 1. Build meta-model (heavy — runs Spoon / javac). Report progress via
                //    SwingWorker (setProgress drives the bar, publish carries the message).
                org.openflexo.pamela.editor.model.BuildProgressListener pl =
                        (fraction, message) -> {
                            setProgress(Math.max(0, Math.min(100, (int) Math.round(fraction * 100))));
                            publish(message);
                        };
                SourceMetaModel metaModel =
                        SourceMetaModelSerializer.load(pamelaFile, pl, currentCustomMethodFilter());
                // The default-identifier-style preference only affects generation (not detection),
                // so it is set on the built model rather than passed to the serializer.
                metaModel.setDefaultPropertyIdentifierStyle(currentDefaultPropertyIdentifierStyle());
                logger.info(metaModel.prettyPrint());

                // 2. Create project (lightweight)
                PamelaProject project =
                        new PamelaProject(pamelaFile, metaModel);

                // 3. Load diagram sidecars (I/O only, no Spoon)
                File projectDir = pamelaFile.getParentFile();
                if (projectDir == null) {
                    projectDir = new java.io.File(".");
                }
                try {
                    java.util.List<String> diagramFileNames =
                            SourceMetaModelSerializer.loadDiagramFileNames(pamelaFile);
                    // Remember which sidecars this project had on disk, so a later
                    // save can purge the files of deleted diagrams.
                    project.setKnownSidecarFiles(diagramFileNames);
                    for (String fileName : diagramFileNames) {
                        File diagramFile = new File(projectDir, fileName);
                        if (diagramFile.exists()) {
                            try {
                                PamelaClassDiagram diagram =
                                        PamelaClassDiagramSerializer.load(
                                                diagramFile, project);
                                project.addDiagram(diagram);
                            } catch (Exception de) {
                                logger.warning("Failed to load diagram sidecar "
                                        + diagramFile + ": " + de.getMessage());
                            }
                        } else {
                            logger.warning("Diagram sidecar not found: "
                                    + diagramFile.getAbsolutePath());
                        }
                    }
                } catch (Exception de) {
                    logger.warning("Failed to read diagram list from "
                            + pamelaFile + ": " + de.getMessage());
                }
                return project;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    buildProgress(getProgress(), chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                // Back on the EDT — safe to update UI
                try {
                    PamelaProject project = get();
                    project.setToolFactory(toolFactory);

                    List<PamelaProject> oldProjects = new ArrayList<>(projects);
                    projects.add(project);
                    PamelaEditorPreferences.setLastFile(pamelaFile);
                    applyValidationPanelLayout();

                    // Notify Gina bindings that the projects list has changed
                    pcSupport.firePropertyChange("projects",
                            Collections.unmodifiableList(oldProjects),
                            Collections.unmodifiableList(projects));
                    // Open the newly-visible tree content up to a comfortable, bounded number
                    // of rows (see MetaModelBrowser.autoExpandToFillView). Deferred so Gina has
                    // finished (re)building the tree for the new project first.
                    javax.swing.SwingUtilities.invokeLater(metaModelBrowser::autoExpandToFillView);

                } catch (Exception e) {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    logger.severe("Failed to open project "
                            + pamelaFile + ": " + cause.getMessage());
                    cause.printStackTrace();
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to open project:\n" + cause.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    buildFinished();
                }
            }
        }.execute();
    }

    /** Opens a file chooser and loads the selected {@code .pamela} project. */
    public void openProjectFromChooser() {
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            openProject(fileChooser.getSelectedFile());
        }
    }

    /**
     * Shows the {@link NewProjectDialog} and, if confirmed, creates a new
     * {@link SourceMetaModel} from the supplied inputs, saves the {@code .pamela}
     * file, and opens the resulting project in the editor.
     *
     * <p>The Spoon analysis ({@code buildMetaModel()}) runs on a background thread
     * to avoid blocking the EDT.</p>
     */
    public void newProject() {
        NewProjectDialog dialog = new NewProjectDialog(frame);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        String projectName = dialog.getProjectName();
        File   pamelaFile  = dialog.getPamelaFile();

        new javax.swing.SwingWorker<PamelaProject, Void>() {

            @Override
            protected PamelaProject doInBackground() throws Exception {
                // 1. Create an empty meta-model (no source dirs, no root types yet)
                SourceMetaModel metaModel = new SourceMetaModel();
                metaModel.setName(projectName);

                // 2. Persist the .pamela file immediately so the project has a location
                SourceMetaModelSerializer.save(metaModel, pamelaFile,
                        java.util.Collections.emptyList());

                // 3. Create the project
                return new PamelaProject(pamelaFile, metaModel);
            }

            @Override
            protected void done() {
                try {
                    PamelaProject project = get();
                    project.setToolFactory(toolFactory);

                    List<PamelaProject> oldProjects = new ArrayList<>(projects);
                    projects.add(project);
                    PamelaEditorPreferences.setLastFile(pamelaFile);
                    applyValidationPanelLayout();

                    pcSupport.firePropertyChange("projects",
                            Collections.unmodifiableList(oldProjects),
                            Collections.unmodifiableList(projects));
                    javax.swing.SwingUtilities.invokeLater(metaModelBrowser::autoExpandToFillView);

                    // Select the new project in the browser
                    setCurrentSelectedElement(project);

                } catch (Exception e) {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    logger.severe("Failed to create project: " + cause.getMessage());
                    cause.printStackTrace();
                    JOptionPane.showMessageDialog(
                            frame,
                            loc("error_creating_project") + "\n" + cause.getMessage(),
                            loc("error"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static String loc(String key) {
        return PAMELA_EDITOR_LOCALIZATION.localizedForKey(key);
    }

    /** Saves the currently active project's {@code .pamela} file and all diagram sidecars. */
    public void saveActiveProject() {
        PamelaProject project = getProjectForElement(currentSelectedElement);
        if (project == null && !projects.isEmpty()) {
            project = projects.get(projects.size() - 1);
        }
        saveProject(project);
        // If the active source view had unsaved text edits, re-derive the model from the
        // just-saved source (deferred-save reconcile on save, D4). Done only on an explicit
        // save (not the close/quit prompt) so a closing project is never rebuilt.
        if (project != null && pendingSourceViewReconcile) {
            reconcileCurrentSelection(project);
        }
    }

    /**
     * Re-runs the source analysis of the active project, picking up any
     * change made on disk outside the editor (e.g. a class added or edited in
     * the IDE). This is the manual counterpart of the (not-yet-implemented)
     * file watcher of {@code model-editing-design.md §7}: a freshly added
     * {@code .java} file appears as a {@code SourceJavaFile} in the browser
     * after the rebuild (and a new {@code @ModelEntity} materialises if it is
     * reachable from a root type).
     *
     * <p>The build-cache fingerprint already detects on-disk changes and forces
     * a full reparse when needed, so this never serves a stale model.</p>
     */
    public void refreshActiveProject() {
        PamelaProject project = getProjectForElement(currentSelectedElement);
        if (project == null && !projects.isEmpty()) {
            project = projects.get(projects.size() - 1);
        }
        if (project == null) {
            return;
        }
        // Re-anchor the selection on the same entity (by qualified name) after the
        // rebuild, since the rebuild replaces every Source* instance.
        final SourceMetaModel model = project.getMetaModel();
        final String selectedEntityQN = currentSelectedElement instanceof SourceModelEntity
                ? ((SourceModelEntity) currentSelectedElement).getQualifiedName()
                : null;
        rebuildProject(project, () -> {
            if (selectedEntityQN != null && model != null) {
                SourceModelEntity again = model.getEntity(selectedEntityQN);
                if (again != null) {
                    selectInBrowser(again);
                }
            }
        });
    }

    /**
     * Persists a project: reconciles the project directory with the in-memory model.
     * Writes each diagram to {@code <id>.diagram}, updates the {@code .pamela}
     * {@code "diagrams"} list, purges sidecars of deleted diagrams, and clears the
     * project's dirty flag.
     *
     * @param project the project to save (no-op if {@code null})
     */
    public void saveProject(PamelaProject project) {
        if (project == null) {
            return;
        }
        File projectDir = project.getPamelaFile().getParentFile();
        if (projectDir == null) {
            projectDir = new java.io.File(".");
        }

        // 0. Flush unsaved source-model edits (deferred-save dirty buffers) to disk.
        //    This is the only place model mutations reach disk — see
        //    editable-source-dirty-buffer-design.md.
        try {
            project.getMetaModel().flushAll();
        } catch (Exception e) {
            logger.severe("Failed to flush source edits: " + e.getMessage());
        }

        // 1. Write each diagram to <id>.diagram; collect the current file-name set.
        java.util.List<String> diagramFileNames = new java.util.ArrayList<>();
        for (PamelaClassDiagram diagram : project.getDiagrams()) {
            String fileName = PamelaClassDiagramSerializer.sidecarFileName(diagram.getId());
            diagramFileNames.add(fileName);
            File diagramFile = new File(projectDir, fileName);
            try {
                PamelaClassDiagramSerializer.save(diagram, diagramFile);
            } catch (Exception de) {
                logger.warning("Failed to save diagram sidecar "
                        + diagramFile + ": " + de.getMessage());
            }
        }

        // 2. Purge sidecars the project previously had but no longer does (deletions
        //    and any stale file names from a manual rename). Only files this project
        //    tracked are touched, never unrelated .diagram files.
        for (String stale : project.getKnownSidecarFiles()) {
            if (!diagramFileNames.contains(stale)) {
                File staleFile = new File(projectDir, stale);
                if (staleFile.exists() && !staleFile.delete()) {
                    logger.warning("Could not delete obsolete diagram sidecar: " + staleFile);
                }
            }
        }
        project.setKnownSidecarFiles(diagramFileNames);

        // 3. Save the .pamela file (includes the diagrams list).
        try {
            SourceMetaModelSerializer.save(
                    project.getMetaModel(),
                    project.getPamelaFile(),
                    diagramFileNames);
        } catch (Exception e) {
            logger.severe("Failed to save project: " + e.getMessage());
        }

        // 4. Clear dirty state and refresh the window title.
        project.setDirty(false);
        updateFrameTitle();
    }

    /**
     * Marks a project as having unsaved changes and refreshes the window title.
     * Central entry point for all diagram mutations (create/delete/rename, entity
     * add/remove, shape move/resize).
     */
    public void markProjectDirty(PamelaProject project) {
        if (project == null || project.isDirty()) {
            return;
        }
        project.setDirty(true);
        updateFrameTitle();
    }

    /** Updates the window title, appending " *" when any open project has unsaved changes. */
    private void updateFrameTitle() {
        boolean anyDirty = false;
        for (PamelaProject p : projects) {
            if (p.isDirty()) {
                anyDirty = true;
                break;
            }
        }
        frame.setTitle("PAMELA Editor" + (anyDirty ? " *" : ""));
    }

    /** Closes a project and removes all related views and browser entries. */
    /**
     * Runs {@link org.openflexo.pamela.editor.model.SourceMetaModel#rebuildMetaModel()}
     * on a background thread for the given project, then refreshes the UI on the EDT.
     *
     * <p>The metamodel's inputs (source directories, root type names) must already be
     * updated before calling this method.</p>
     */
    public void rebuildProject(PamelaProject project) {
        rebuildProject(project, null);
    }

    /**
     * Re-runs Spoon analysis off the EDT, then refreshes the UI.
     *
     * @param project      the project to rebuild
     * @param afterRebuild optional callback run on the EDT once the rebuild and
     *                     UI refresh are complete (e.g. to select a newly
     *                     materialised entity). May be {@code null}.
     */
    public void rebuildProject(PamelaProject project, Runnable afterRebuild) {
        if (project == null) {
            return;
        }
        SourceMetaModel metaModel = project.getMetaModel();

        buildStarted("Rebuilding " + metaModel.getName() + "…");
        new javax.swing.SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                org.openflexo.pamela.editor.model.BuildProgressListener pl =
                        (fraction, message) -> {
                            setProgress(Math.max(0, Math.min(100, (int) Math.round(fraction * 100))));
                            publish(message);
                        };
                metaModel.setProgressListener(pl);
                metaModel.setCustomMethodFilter(currentCustomMethodFilter());
                metaModel.setDefaultPropertyIdentifierStyle(currentDefaultPropertyIdentifierStyle());
                try {
                    metaModel.rebuildMetaModel();
                } finally {
                    metaModel.setProgressListener(null);
                }
                logger.info("Rebuild complete: " + metaModel.prettyPrint());
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    buildProgress(getProgress(), chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // propagate exceptions if any
                } catch (Exception e) {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    logger.severe("Rebuild failed: " + cause.getMessage());
                    cause.printStackTrace();
                    JOptionPane.showMessageDialog(
                            frame,
                            "Rebuild failed:\n" + cause.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                // A rebuild recreates all Source* objects. Re-anchor editable source views
                // of this model to the fresh entities (preserving caret/scroll/focus) and
                // drop the other cached views so they are recreated fresh on next show.
                invalidateSourceViews(metaModel);

                // Diagram editors are kept across a rebuild, but their EntityView.entity
                // references now point at stale instances (and entities may have appeared
                // or disappeared). Re-resolve them and restyle unresolved placeholders.
                for (PamelaClassDiagram diagram : project.getDiagrams()) {
                    PamelaClassDiagramEditor editor = diagramEditors.get(diagram);
                    if (editor != null) {
                        editor.refreshEntityResolution();
                    }
                }

                // Force Gina to refresh the browser tree and all summary statistics
                java.beans.PropertyChangeSupport pcs = metaModel.getPropertyChangeSupport();
                pcs.firePropertyChange("allPackages", null,
                        new ArrayList<>(metaModel.getAllPackages()));
                pcs.firePropertyChange("sourceFolders", null,
                        new ArrayList<>(metaModel.getSourceFolders()));
                pcs.firePropertyChange("entitiesCount", -1, metaModel.getEntitiesCount());
                pcs.firePropertyChange("packagesCount", -1, metaModel.getPackagesCount());
                pcs.firePropertyChange("totalPropertiesCount", -1, metaModel.getTotalPropertiesCount());
                pcs.firePropertyChange("abstractEntitiesCount", -1, metaModel.getAbstractEntitiesCount());
                pcs.firePropertyChange("totalInitializersCount", -1, metaModel.getTotalInitializersCount());
                pcs.firePropertyChange("issuesCount", -1, metaModel.getIssuesCount());
                pcs.firePropertyChange("issues", null, new ArrayList<>(metaModel.getIssues()));

                // A model-editing mutation leaves the meta-model dirty (deferred save).
                // Refresh the window title so the unsaved-changes marker (*) appears.
                updateFrameTitle();

                try {
                    if (afterRebuild != null) {
                        afterRebuild.run();
                    }
                } finally {
                    buildFinished();
                }
            }
        }.execute();
    }

    /**
     * Removes every cached central view except diagram editors (which are not
     * affected by a meta-model rebuild). Called after a rebuild so that re-showing
     * a {@code Source*} element rebuilds its view from the current source on disk.
     */
    private void invalidateSourceViews(SourceMetaModel rebuiltModel) {
        java.util.Map<Object, JComponent> reanchored = new java.util.IdentityHashMap<>();
        java.util.Iterator<java.util.Map.Entry<Object, JComponent>> it =
                viewCache.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Object, JComponent> e = it.next();
            Object key = e.getKey();
            JComponent v = e.getValue();
            if (key instanceof PamelaClassDiagram) {
                continue; // diagram editors are unaffected by a metamodel rebuild
            }
            // Keep an editable source view of the rebuilt model: re-anchor it to the fresh
            // entity instance (preserving caret / scroll / focus) instead of discarding it.
            if (v instanceof SourceCodeView && key instanceof SourceModelEntity
                    && ((SourceModelEntity) key).getMetaModel() == rebuiltModel) {
                SourceModelEntity fresh =
                        rebuiltModel.getEntity(((SourceModelEntity) key).getQualifiedName());
                if (fresh != null) {
                    ((SourceCodeView) v).rebind(fresh);
                    it.remove();
                    reanchored.put(fresh, v);
                    continue;
                }
            }
            // Everything else (summaries, java-file views, source views whose entity vanished)
            // is discarded and recreated fresh on next show.
            if (v instanceof SourceCodeView) {
                ((SourceCodeView) v).dispose();
            }
            it.remove();
        }
        viewCache.putAll(reanchored);
    }

    // =========================================================================
    // Status bar / progress (bottom of the main window)
    // =========================================================================

    /** Builds the bottom status bar: a message label plus a determinate progress bar. */
    private javax.swing.JComponent buildStatusBar() {
        statusLabel = new javax.swing.JLabel(" ");
        statusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 6));

        progressBar = new javax.swing.JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new java.awt.Dimension(220,
                progressBar.getPreferredSize().height));

        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.LIGHT_GRAY),
                javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        bar.add(statusLabel, BorderLayout.CENTER);
        bar.add(progressBar, BorderLayout.EAST);

        // Idle by default — shown only while a build is running.
        statusLabel.setVisible(false);
        progressBar.setVisible(false);
        return bar;
    }

    /** EDT: a background build started — reveal the status bar. Ref-counted. */
    private void buildStarted(String message) {
        activeBuilds++;
        statusLabel.setText(message);
        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setVisible(true);
        progressBar.setVisible(true);
    }

    /** EDT: update the running build's progress (0–100) and message. */
    private void buildProgress(int percent, String message) {
        int clamped = Math.max(0, Math.min(100, percent));
        progressBar.setValue(clamped);
        progressBar.setString(clamped + "%");
        if (message != null) {
            statusLabel.setText(message);
        }
    }

    /** EDT: a background build finished — hide the status bar when none remain. */
    private void buildFinished() {
        activeBuilds = Math.max(0, activeBuilds - 1);
        if (activeBuilds == 0) {
            statusLabel.setVisible(false);
            progressBar.setVisible(false);
            statusLabel.setText(" ");
        }
    }

    /**
     * Closes the project owning the current selection (falling back to the most recently
     * opened one), mirroring {@link #saveActiveProject()}/{@link #refreshActiveProject()}.
     * Entry point for {@code File → Close}, which was previously unwired.
     */
    public void closeActiveProject() {
        PamelaProject project = getProjectForElement(currentSelectedElement);
        if (project == null && !projects.isEmpty()) {
            project = projects.get(projects.size() - 1);
        }
        closeProject(project);
    }

    public void closeProject(PamelaProject project) {
        if (project == null) {
            return;
        }
        if (!confirmSaveIfDirty(project)) {
            return; // user cancelled the close
        }
        // Invalidate all diagram views belonging to this project
        for (PamelaClassDiagram diagram : project.getDiagrams()) {
            invalidateCachedView(diagram);
            diagramEditors.remove(diagram);
        }
        boolean selectionBelongsToClosedProject =
                getProjectForElement(currentSelectedElement) == project;
        boolean centralViewBelongsToClosedProject =
                getProjectForElement(currentHistoryElement) == project;
        List<PamelaProject> oldProjects = new ArrayList<>(projects);
        projects.remove(project);
        if (centralViewBelongsToClosedProject) {
            // Reset the central view rather than leave it showing the closed project's stale
            // content (openOrSwitchCentralView(null) is otherwise a no-op) — also keeps
            // currentHistoryElement correctly null so a freshly (re)opened project's validation
            // strip stays hidden until something is actually selected in it.
            clearCentralView();
        }
        applyValidationPanelLayout();
        pcSupport.firePropertyChange("projects",
                Collections.unmodifiableList(oldProjects),
                Collections.unmodifiableList(projects));
        if (selectionBelongsToClosedProject) {
            // Clears the detailed browser and inspector (ui-design.md, see the "genuine
            // programmatic clear" note on onBrowserSelectionChanged).
            setCurrentSelectedElement(null);
        }
    }

    /**
     * Resets the central view to empty: clears {@link #centralViewPanel}, the navigation
     * history and title, and detaches any Diana toolbar. Called when the project owning the
     * currently displayed view is closed (validation-log-panel-design.md).
     */
    private void clearCentralView() {
        currentHistoryElement = null;
        backHistory.clear();
        forwardHistory.clear();
        centralViewPanel.removeAll();
        centralViewPanel.revalidate();
        centralViewPanel.repaint();
        centralTitleLabel.setText("");
        detachDianaTools();
        updateNavButtons();
    }

    // =========================================================================
    // Selection model
    // =========================================================================

    /**
     * Called when the user clicks a node in either the {@link MetaModelBrowser}
     * or the {@link DetailedBrowser}.
     *
     * <p>The {@link DetailedBrowser} is rebound to the <em>described element</em>
     * (see {@link #getDetailedBrowserElement(Object)}), which may differ from
     * {@code element} itself: for child elements such as {@link SourceModelProperty},
     * {@link org.openflexo.pamela.editor.model.SourceModelInitializer}, and
     * {@link org.openflexo.pamela.editor.model.SourceImplementationClass}, the
     * described element is the parent {@link SourceModelEntity}.  This keeps the
     * browser showing the entity's full content rather than going empty.
     *
     * <p>The inspector and central view always receive the actually selected
     * {@code element}, so they continue to show property-specific content.</p>
     *
     * <p>This method is guarded against re-entrance: Gina may call the
     * {@link org.openflexo.pamela.editor.ui.widget.DetailedBrowserFIBController}'s
     * setter with {@code null} while rebinding the browser, which in turn would call
     * this method recursively.  The guard silently discards any re-entrant call.</p>
     */
    public void setCurrentSelectedElement(Object element) {
        setCurrentSelectedElement(element, true);
    }

    /**
     * Sets the current selected element, optionally navigating the central view.
     *
     * <p>With {@code navigateCentralView == true} (the default, used by the browsers)
     * the central view is switched to the element's view. With {@code false} (used by
     * the diagram — <em>soft</em> selection, see {@code ui-design.md §18.2}) only the
     * inspector and the detailed browser are refreshed: the central view is left
     * untouched, so selecting an element <em>inside</em> the active diagram does not
     * make the diagram disappear from under the click. {@link #currentHistoryElement}
     * is not modified in that case, so {@link #getActiveDiagramEditor()} keeps
     * returning the diagram editor.</p>
     */
    public void setCurrentSelectedElement(Object element, boolean navigateCentralView) {
        if (settingSelectedElement) {
            return; // discard re-entrant calls from Gina's internal rebind machinery
        }
        settingSelectedElement = true;
        try {
            this.currentSelectedElement = element;
            try {
                if (navigateCentralView) {
                    // 1a. Normal path: rebind the detailed browser to the "described" element.
                    //     For child elements (property, initializer, impl class) this is the
                    //     parent entity, so the browser stays populated.
                    //     Exception: a PamelaProject / SourceMetaModel / SourceFolder / SourcePackage
                    //     selection does NOT rebind — its content would just repeat, one panel over,
                    //     the same nested list already visible under that very node in the
                    //     MetaModelBrowser tree (deepExploration mode). Leaving the DetailedBrowser
                    //     bound to whatever it last showed avoids that redundant, distracting
                    //     mirror (see ui-design.md §4.2).
                    Object described = getDetailedBrowserElement(element);
                    if (!isRedundantWithMainBrowserTree(described)) {
                        detailedBrowser.setEditedObject(described);
                    }
                } else {
                    // 1b. Soft-selection (diagram canvas click): keep the diagram as the
                    //     browser root but update which EntityView node is highlighted.
                    syncDetailedBrowserToDiagramSelection(element);
                }
            } catch (Exception e) {
                logger.warning("DetailedBrowser update failed: " + e.getMessage());
                e.printStackTrace();
            }
            try {
                // 2. Refresh the inspector (always for the actual selected element)
                inspectorController.inspectObject(element);
                // 2b. Observe the inspected element for in-inspector edits: an editable
                //     Source* setter mutates the source and fires a PropertyChange; the
                //     application reacts by rebuilding (the model never calls the rebuild
                //     itself — see model-editing-design.md §6).
                installEditListener(element);
            } catch (Exception e) {
                logger.warning("Inspector refresh failed: " + e.getMessage());
                e.printStackTrace();
            }
            if (navigateCentralView) {
                try {
                    // 3. Open or navigate the central view.
                    //    If the user is pressing in the browser (a potential drag onto a
                    //    diagram), defer the switch: keep the current diagram visible so it
                    //    remains a valid drop target. The switch is committed on mouse
                    //    release only if no drag happened (see onBrowserMouseReleased).
                    if (browserPressActive) {
                        deferredCentralViewElement = element;
                    } else {
                        openOrSwitchCentralView(element);
                    }
                } catch (Exception e) {
                    logger.warning("Central view switch failed: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Soft-selection (diagram canvas click): update context panel without
                // switching the central view (ui-design.md §18.2, §19.4).
                contextPanel.setDiagramSelection(element);
            }
        } finally {
            settingSelectedElement = false;
        }
    }

    /**
     * Entry point for selections originating from the {@link DetailedBrowser} itself.
     *
     * <p>When a class diagram is the active central view the user is browsing the
     * diagram's entity tree.  A selection inside that tree behaves like a
     * <em>soft-selection</em> (inspector and context panel update, the diagram
     * remains the central view and the browser root stays bound to the diagram) and
     * is <em>mirrored onto the diagram canvas</em> — the full {@code selection} list
     * highlights all matching shapes, the {@code lead} drives the single-object
     * inspector and context panel. Otherwise (no diagram active) the {@code lead}
     * drives the normal single-selection navigation
     * ({@link #setCurrentSelectedElement(Object)}).</p>
     */
    public void onDetailedBrowserSelectionChanged(Object lead, List<Object> selection) {
        if (settingSelectedElement) {
            return; // discard re-entrant calls during programmatic browser/diagram sync
        }
        PamelaClassDiagramEditor diagramEditor = getActiveDiagramEditor();
        if (diagramEditor != null) {
            settingSelectedElement = true;
            try {
                this.currentSelectedElement = lead;
                // Mirror the browser multi-selection onto the diagram canvas.
                diagramEditor.getDianaEditor().selectModelElements(selection);
                inspectorController.inspectObject(lead);
                contextPanel.setDiagramSelection(lead);
            } catch (Exception e) {
                logger.warning("Detailed browser selection sync failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                settingSelectedElement = false;
            }
        } else {
            // No diagram active: lead drives the normal single-selection navigation.
            setCurrentSelectedElement(lead, true);
        }
    }

    /**
     * Entry point for selections originating from the diagram canvas (multi-selection).
     *
     * <p><em>Soft-selection</em> (see {@code ui-design.md §18.2}): updates the inspector
     * and context panel for the {@code lead} element and highlights every selected
     * element in the {@link DetailedBrowser}, without switching the central view away
     * from the diagram. The diagram is the source of this selection, so it is
     * <em>not</em> mirrored back to the canvas.</p>
     */
    public void onDiagramSelectionChanged(Object lead, List<Object> selection) {
        if (settingSelectedElement) {
            return; // discard re-entrant calls (e.g. round-trip from a programmatic browser sync)
        }
        settingSelectedElement = true;
        try {
            this.currentSelectedElement = lead;
            // Highlight all selected elements' nodes in the detailed browser.
            setDetailedBrowserMultiSelection(selection);
            inspectorController.inspectObject(lead);
            contextPanel.setDiagramSelection(lead);
        } catch (Exception e) {
            logger.warning("Diagram selection sync failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            settingSelectedElement = false;
        }
    }

    /**
     * Returns the element that the {@link DetailedBrowser} should describe.
     *
     * <p>For child elements ({@link SourceModelProperty},
     * {@link org.openflexo.pamela.editor.model.SourceModelInitializer},
     * {@link org.openflexo.pamela.editor.model.SourceImplementationClass}),
     * the parent {@link SourceModelEntity} is returned so that the browser
     * keeps showing the entity's full property/initializer list rather than
     * going blank when a leaf element is selected.</p>
     *
     * <p>For all other element types, the element itself is returned.</p>
     */
    /**
     * The current custom-method visibility filter, read from the Analysis preferences
     * (falls back to {@link org.openflexo.pamela.editor.model.CustomMethodFilter#DEFAULT}
     * if preferences are unavailable).
     */
    private org.openflexo.pamela.editor.model.CustomMethodFilter currentCustomMethodFilter() {
        try {
            org.openflexo.pamela.editor.ui.preferences.AnalysisPreferences analysis =
                    org.openflexo.pamela.editor.ui.preferences.PreferencesManager.getInstance().analysis();
            if (analysis != null && analysis.getCustomMethodFilter() != null) {
                return analysis.getCustomMethodFilter();
            }
        } catch (Exception ignored) {
            // preferences not available — fall back to default
        }
        return org.openflexo.pamela.editor.model.CustomMethodFilter.DEFAULT;
    }

    /**
     * The default property-identifier style to use when generating on an entity with no decisive
     * style, read from the Analysis preference (falls back to
     * {@link org.openflexo.pamela.editor.model.PropertyIdentifierStyle#DEFAULT} if unavailable).
     */
    private org.openflexo.pamela.editor.model.PropertyIdentifierStyle currentDefaultPropertyIdentifierStyle() {
        try {
            org.openflexo.pamela.editor.ui.preferences.GenerationPreferences generation =
                    org.openflexo.pamela.editor.ui.preferences.PreferencesManager.getInstance().generation();
            if (generation != null && generation.getDefaultPropertyIdentifierStyle() != null) {
                return generation.getDefaultPropertyIdentifierStyle();
            }
        } catch (Exception ignored) {
            // preferences not available — fall back to default
        }
        return org.openflexo.pamela.editor.model.PropertyIdentifierStyle.DEFAULT;
    }

    private Object getDetailedBrowserElement(Object element) {
        if (element instanceof SourceModelProperty) {
            return ((SourceModelProperty) element).getModelEntity();
        }
        if (element instanceof SourceModelInitializer) {
            return ((SourceModelInitializer) element).getEntity();
        }
        if (element instanceof SourceImplementationClass) {
            return ((SourceImplementationClass) element).getEntity();
        }
        if (element instanceof SourceCustomMethod) {
            SourceModelEntity owner = ((SourceCustomMethod) element).getEntity();
            return owner != null ? owner : element;
        }
        return element;
    }

    /**
     * Types for which rebinding the {@link DetailedBrowser}'s root object is pure
     * redundancy: their content is exactly the same nested children already shown
     * directly under that node in the {@link MetaModelBrowser} tree (both browsers use
     * {@code deepExploration}, so the whole sub-tree is already visible there). Selecting
     * one of these leaves the DetailedBrowser bound to whatever it was showing before.
     */
    private boolean isRedundantWithMainBrowserTree(Object element) {
        return element instanceof SourceMetaModel
                || element instanceof SourceFolder
                || element instanceof SourcePackage
                || element instanceof PamelaProject;
    }

    /**
     * Updates the selection inside the {@link DetailedBrowser} without changing its
     * root object. Called during diagram soft-selection ({@code navigateCentralView=false}):
     * the browser root stays the active {@link PamelaClassDiagram}, but the
     * {@link EntityView} corresponding to the clicked diagram element is highlighted.
     *
     * <p>Mapping rules:
     * <ul>
     *   <li>{@link EntityView} → select directly</li>
     *   <li>{@link SourceModelEntity} → find the matching {@link EntityView} in the diagram</li>
     *   <li>{@link SourceModelProperty} / {@link SourceModelInitializer} /
     *       {@link SourceCustomMethod} → find the {@link EntityView} of the parent entity</li>
     *   <li>Background click ({@link PamelaClassDiagram} or null) → deselect</li>
     * </ul>
     * </p>
     */
    private void syncDetailedBrowserToDiagramSelection(Object element) {
        if (!(currentHistoryElement instanceof PamelaClassDiagram)) return;
        PamelaClassDiagram diagram = (PamelaClassDiagram) currentHistoryElement;

        Object targetSelection = null;
        if (element instanceof EntityView) {
            targetSelection = element;
        } else if (element instanceof SourceModelEntity) {
            targetSelection = findEntityViewInDiagram((SourceModelEntity) element, diagram);
        } else if (element instanceof SourceModelProperty) {
            // Properties are now children of EntityView nodes — select directly.
            targetSelection = element;
        } else if (element instanceof SourceModelInitializer) {
            // Initializers are now children of EntityView nodes — select directly.
            targetSelection = element;
        } else if (element instanceof SourceCustomMethod) {
            // Custom methods have no equivalent node; fall back to the parent EntityView.
            SourceModelEntity owner = ((SourceCustomMethod) element).getEntity();
            if (owner != null) {
                targetSelection = findEntityViewInDiagram(owner, diagram);
            }
        }
        // PamelaClassDiagram or null → targetSelection stays null (deselect)

        detailedBrowser.getController().setSelectedElement(targetSelection);

        // Mirror the selection onto the Diana diagram canvas.
        PamelaClassDiagramEditor diagramEditor = getActiveDiagramEditor();
        if (diagramEditor != null) {
            diagramEditor.getDianaEditor().selectModelElement(element);
        }
    }

    /**
     * Highlights the {@link DetailedBrowser} nodes for every element of {@code selection}
     * without changing the browser root (multi-selection counterpart of
     * {@link #syncDetailedBrowserToDiagramSelection}). Called during diagram
     * soft-selection: the root stays the active {@link PamelaClassDiagram}; each element
     * is mapped to its browser node via {@link #detailedBrowserTargetFor}. Does not
     * mirror back to the diagram canvas (the diagram is the source of the selection).
     */
    private void setDetailedBrowserMultiSelection(List<Object> selection) {
        if (!(currentHistoryElement instanceof PamelaClassDiagram)) return;
        PamelaClassDiagram diagram = (PamelaClassDiagram) currentHistoryElement;

        List<Object> targets = new ArrayList<>();
        if (selection != null) {
            for (Object element : selection) {
                Object target = detailedBrowserTargetFor(element, diagram);
                if (target != null && !targets.contains(target)) {
                    targets.add(target);
                }
            }
        }
        detailedBrowser.getController().setSelection(targets);
    }

    /**
     * Maps a diagram-selected model element to the node that represents it in the
     * {@link DetailedBrowser} (whose root is {@code diagram}):
     * {@link EntityView} → itself; {@link SourceModelEntity} → its {@link EntityView};
     * {@link SourceModelProperty} / {@link SourceModelInitializer} → themselves (they are
     * children of the {@link EntityView} node); {@link SourceCustomMethod} → the parent
     * entity's {@link EntityView}. Returns {@code null} when there is no matching node.
     */
    private Object detailedBrowserTargetFor(Object element, PamelaClassDiagram diagram) {
        if (element instanceof EntityView) {
            return element;
        }
        if (element instanceof SourceModelEntity) {
            return findEntityViewInDiagram((SourceModelEntity) element, diagram);
        }
        if (element instanceof SourceModelProperty) {
            return element;
        }
        if (element instanceof SourceModelInitializer) {
            return element;
        }
        if (element instanceof SourceCustomMethod) {
            SourceModelEntity owner = ((SourceCustomMethod) element).getEntity();
            if (owner != null) {
                return findEntityViewInDiagram(owner, diagram);
            }
        }
        return null;
    }

    /** Returns the {@link EntityView} in {@code diagram} whose resolved entity matches, or null. */
    private EntityView findEntityViewInDiagram(SourceModelEntity entity, PamelaClassDiagram diagram) {
        if (entity == null || diagram.getEntityViews() == null) return null;
        for (EntityView ev : diagram.getEntityViews()) {
            if (ev.getEntity() == entity) return ev;
        }
        return null;
    }

    /** Called from MetaModelBrowserFIBController on single-click. */
    private void onBrowserSelectionChanged(Object element) {
        // Ignore a null from the browser: it is almost always a spurious deselect emitted while
        // Gina rebuilds the tree after a metamodel rebuild — the selected node is briefly removed,
        // which clears the tree selection and writes null back through the two-way "selected"
        // binding. Honouring it would empty the detailed browser and inspector. A genuine
        // programmatic clear (e.g. closing a project) calls setCurrentSelectedElement(null) directly.
        if (element == null) {
            return;
        }
        setCurrentSelectedElement(element);
    }

    // =========================================================================
    // Browser drag-to-diagram coordination (see field declarations above)
    // =========================================================================

    /** The user pressed the mouse on the browser tree — a drag may be starting. */
    public void onBrowserMousePressed() {
        browserPressActive = true;
        browserDragOccurred = false;
        deferredCentralViewElement = null;
    }

    /** A drag gesture was recognized on the browser tree (so it is a drag, not a click). */
    public void onBrowserDragStarted() {
        browserDragOccurred = true;
    }

    /**
     * The mouse was released on the browser tree. If this was a plain click (no
     * drag), commit the central-view switch that was deferred during the press.
     */
    public void onBrowserMouseReleased() {
        browserPressActive = false;
        Object pending = deferredCentralViewElement;
        deferredCentralViewElement = null;
        if (!browserDragOccurred && pending != null) {
            openOrSwitchCentralView(pending);
        }
    }

    /**
     * A browser drag-and-drop operation finished (drop or cancel). Clears the
     * press state without committing the deferred view switch, so the diagram the
     * entity was dropped on stays the active central view.
     */
    public void onBrowserDragEnded() {
        browserPressActive = false;
        browserDragOccurred = false;
        deferredCentralViewElement = null;
    }

    /**
     * Selects {@code element} in the {@link MetaModelBrowser} tree, so the node is
     * highlighted and scrolled into view. Setting the browser controller's
     * selection cascades to {@link #setCurrentSelectedElement(Object)} via the
     * registered listener, keeping the tree highlight, central view, inspector and
     * detailed browser all in sync.
     *
     * <p>Used after a model mutation that recreates the selected element (e.g.
     * declaring a Java file as an entity): the old node is gone from the refreshed
     * tree, so we re-point the browser at the new object. Deferred via
     * {@code invokeLater} so it runs after Gina has rebuilt the tree model from the
     * just-fired refresh events.</p>
     */
    public void selectInBrowser(Object element) {
        javax.swing.SwingUtilities.invokeLater(() ->
                metaModelBrowser.getController().setSelectedElement(element));
    }

    // =========================================================================
    // Central view — single view + Back/Forward history (ui-design.md §5)
    // =========================================================================

    /**
     * Navigates the central view to show {@code element}.
     *
     * <p>If {@code element} is already the currently displayed element, nothing
     * happens. Otherwise, the current element is pushed onto the back-history
     * stack, the forward history is cleared, and the view is replaced.</p>
     *
     * <p>For elements without a dedicated central view (e.g. {@link SourceModelProperty}),
     * the nearest ancestor view is shown instead (the entity), and the relevant
     * methods are highlighted in the source-code view.</p>
     */
    public void openOrSwitchCentralView(Object element) {
        if (element == null) {
            return;
        }

        // SourceModelProperty: show the parent entity's source view, then highlight
        if (element instanceof SourceModelProperty) {
            SourceModelProperty prop = (SourceModelProperty) element;
            SourceModelEntity entity = prop.getModelEntity();
            openOrSwitchCentralView(entity);
            JComponent view = viewCache.get(entity);
            if (view instanceof SourceCodeView) {
                ((SourceCodeView) view).highlightProperty(prop);
            }
            // Reflect the selection in the SpoonOutlineView (context panel)
            contextPanel.selectPropertyMethods(prop);
            return;
        }

        // SourceModelInitializer / SourceCustomMethod: no dedicated view — show the parent
        // entity's source and highlight the method declaration (ui-design.md §5.3).
        if (element instanceof SourceModelInitializer) {
            SourceModelInitializer init = (SourceModelInitializer) element;
            highlightMethodInEntityView(init.getEntity(), init.getDeclarationLine());
            return;
        }
        if (element instanceof SourceCustomMethod) {
            SourceCustomMethod method = (SourceCustomMethod) element;
            highlightMethodInEntityView(method.getEntity(), method.getDeclarationLine());
            return;
        }

        // Already showing this element — do nothing
        if (element == currentHistoryElement) {
            return;
        }

        // Same entity, fresh instance (after a metamodel rebuild): the source view was
        // re-anchored in place (invalidateSourceViews), so it is already correct. Keep it —
        // no view swap, so caret / scroll / focus are preserved — and just update the
        // reference, the title and the context panel.
        if (element instanceof SourceModelEntity && currentHistoryElement instanceof SourceModelEntity
                && ((SourceModelEntity) element).getQualifiedName()
                        .equals(((SourceModelEntity) currentHistoryElement).getQualifiedName())) {
            currentHistoryElement = element;
            centralTitleLabel.setText(titleFor(element));
            contextPanel.showFor(element);
            return;
        }

        // Push the current element onto the back stack (if there is one)
        if (currentHistoryElement != null) {
            backHistory.push(currentHistoryElement);
        }
        // A new navigation always clears the forward history
        forwardHistory.clear();

        currentHistoryElement = element;
        showViewForElement(element);
        updateNavButtons();
    }

    /**
     * Shows the given entity's source-code view and highlights the method declaration at the
     * given 1-based line. Used for elements without a dedicated central view (initializers,
     * custom methods); the line disambiguates overloaded methods.
     */
    private void highlightMethodInEntityView(SourceModelEntity entity, int declarationLine) {
        if (entity == null) {
            return;
        }
        openOrSwitchCentralView(entity);
        JComponent view = viewCache.get(entity);
        if (view instanceof SourceCodeView) {
            ((SourceCodeView) view).highlightLine(declarationLine);
        }
        // Reflect the selection in the SpoonOutlineView (context panel).
        contextPanel.selectMethodAtLine(declarationLine);
    }

    /**
     * Navigates backward in history: the current element goes to the forward
     * stack, and the top of the back stack becomes the new current element.
     */
    public void navigateBack() {
        if (backHistory.isEmpty()) {
            return;
        }
        if (currentHistoryElement != null) {
            forwardHistory.push(currentHistoryElement);
        }
        currentHistoryElement = backHistory.pop();
        showViewForElement(currentHistoryElement);
        updateNavButtons();
    }

    /**
     * Navigates forward in history: the current element goes to the back stack,
     * and the top of the forward stack becomes the new current element.
     */
    public void navigateForward() {
        if (forwardHistory.isEmpty()) {
            return;
        }
        if (currentHistoryElement != null) {
            backHistory.push(currentHistoryElement);
        }
        currentHistoryElement = forwardHistory.pop();
        showViewForElement(currentHistoryElement);
        updateNavButtons();
    }

    /**
     * Replaces the content of {@link #centralViewPanel} with the view for
     * {@code element}.  Creates the view if it is not yet cached.
     * Does NOT modify the navigation history stacks.
     */
    private void showViewForElement(Object element) {
        // Look up or create the view
        JComponent view = viewCache.get(element);
        if (view == null) {
            view = createViewFor(element);
            if (view == null) {
                // No dedicated view for this type — keep the current view unchanged
                return;
            }
            viewCache.put(element, view);
        }

        // Swap the content area
        centralViewPanel.removeAll();
        centralViewPanel.add(view, BorderLayout.CENTER);
        centralViewPanel.revalidate();
        centralViewPanel.repaint();

        // Update the navigation title
        centralTitleLabel.setText(titleFor(element));

        // Attach or detach Diana tools
        if (element instanceof PamelaClassDiagram) {
            PamelaClassDiagramEditor editor = diagramEditors.get(element);
            if (editor != null) {
                attachDianaTools(editor);
            }
        } else {
            detachDianaTools();
        }

        // Update the context panel (ui-design.md §19)
        contextPanel.showFor(element);

        // Rebind the validation strip to the owning project, and (re)evaluate whether it
        // should be shown now that the central view actually displays something
        // (validation-log-panel-design.md).
        rebindValidationPanel(element);
        applyValidationPanelLayout();
    }

    /**
     * Rebinds {@link #validationPanel} to the {@link SourceMetaModel} of the project owning
     * {@code element}, if different from what it currently shows. A no-op when the element does
     * not resolve to a project (e.g. nothing selected yet) — the panel keeps showing whatever it
     * last had.
     */
    private void rebindValidationPanel(Object element) {
        PamelaProject project = getProjectForElement(element);
        if (project == null || project.getMetaModel() == null) {
            return;
        }
        if (validationPanel.getController().getDataObject() != project.getMetaModel()) {
            validationPanel.setEditedObject(project.getMetaModel());
        }
    }

    /**
     * Whether the bottom validation strip is currently expanded. Kept fresh by the
     * {@code DIVIDER_LOCATION_PROPERTY} listener installed on {@link #centerSplit} in the
     * constructor — stays meaningful even while the strip is actually absent because no
     * project is open (validation-log-panel-design.md).
     */
    public boolean isValidationPanelVisible() {
        return validationPanelVisible;
    }

    /**
     * Expands or collapses the bottom validation strip by moving {@link #centerSplit}'s
     * divider — equivalent to clicking its native one-touch-expandable arrow. Has no effect on
     * whether the strip is present at all — see {@link #applyValidationPanelLayout()} for that.
     * A no-op if already in the requested state.
     */
    public void setValidationPanelVisible(boolean visible) {
        if (visible == validationPanelVisible) {
            return;
        }
        if (visible) {
            centerSplit.setDividerLocation(lastValidationDividerLocation > 0
                    ? lastValidationDividerLocation
                    : (int) (centerColumn.getHeight() * VALIDATION_PANEL_DIVIDER_FRACTION));
        } else {
            centerSplit.setDividerLocation(1.0); // push the divider to the bottom = collapse
        }
        // validationPanelVisible + the "validationPanelVisible" firing are handled by the
        // DIVIDER_LOCATION_PROPERTY listener reacting to the location change above.
    }

    /**
     * Whether a {@code centerSplit} divider at {@code dividerLocation} leaves
     * {@link #validationPanel} (the bottom pane) at, or below,
     * {@link #VALIDATION_PANEL_COLLAPSE_THRESHOLD} pixels tall.
     */
    private boolean isValidationDividerCollapsed(int dividerLocation) {
        int totalHeight = centerSplit.getHeight();
        if (totalHeight <= 0) {
            return false; // not yet realized — treat as expanded (matches the default)
        }
        int bottomHeight = totalHeight - dividerLocation - centerSplit.getDividerSize();
        return bottomHeight <= VALIDATION_PANEL_COLLAPSE_THRESHOLD;
    }

    /**
     * Adds or removes {@link #centerSplit} (in place of {@link #centralViewPanel} alone) from
     * {@link #centerColumn}'s {@code CENTER} slot depending on whether any project is open
     * <em>and</em> the central column is actually showing a view — the strip must not show at
     * all otherwise (e.g. right after opening a project, before anything has been selected).
     * Called after every change to {@link #projects} and every time the central view changes
     * (see {@link #showViewForElement(Object)}). Its own expanded/collapsed state (divider
     * position) is untouched.
     */
    private void applyValidationPanelLayout() {
        boolean shouldBePresent = !projects.isEmpty() && currentHistoryElement != null;
        java.awt.Component current =
                ((BorderLayout) centerColumn.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        boolean isPresent = (current == centerSplit);
        if (shouldBePresent == isPresent) {
            return;
        }
        centerColumn.remove(current);
        if (shouldBePresent) {
            centerSplit.setTopComponent(centralViewPanel);
            centerSplit.setBottomComponent(validationPanel);
            centerColumn.add(centerSplit, BorderLayout.CENTER);
            centerSplit.setDividerLocation(lastValidationDividerLocation > 0
                    ? lastValidationDividerLocation
                    : (int) (centerColumn.getHeight() * VALIDATION_PANEL_DIVIDER_FRACTION));
        } else {
            centerColumn.add(centralViewPanel, BorderLayout.CENTER);
        }
        centerColumn.revalidate();
        centerColumn.repaint();
    }

    /**
     * Updates the enabled state of the Back/Forward navigation buttons.
     * Must be called after any change to {@link #backHistory} or {@link #forwardHistory}.
     */
    private void updateNavButtons() {
        backButton.setEnabled(!backHistory.isEmpty());
        forwardButton.setEnabled(!forwardHistory.isEmpty());
    }

    /**
     * Creates the Swing component for a model element.
     * Returns {@code null} if this element type has no dedicated central view.
     */
    private JComponent createViewFor(Object element) {
        if (element instanceof SourceMetaModel) {
            MetaModelSummaryView view = new MetaModelSummaryView((SourceMetaModel) element);
            view.setApplication(this);
            return view;
        }
        if (element instanceof PamelaProject) {
            PamelaProject project = (PamelaProject) element;
            MetaModelSummaryView view = new MetaModelSummaryView(project.getMetaModel());
            view.setApplication(this);
            File pamelaFile = project.getPamelaFile();
            if (pamelaFile != null && pamelaFile.getParentFile() != null) {
                view.setProjectDirectory(pamelaFile.getParentFile());
            }
            return view;
        }
        if (element instanceof SourceFolder) {
            SourceFolderSummaryView view = new SourceFolderSummaryView((SourceFolder) element);
            view.setApplication(this);
            return view;
        }
        if (element instanceof SourcePackage) {
            PackageSummaryView view = new PackageSummaryView((SourcePackage) element);
            view.setApplication(this);
            return view;
        }
        if (element instanceof SourceModelEntity) {
            return new SourceCodeView((SourceModelEntity) element, this);
        }
        if (element instanceof SourceJavaFile) {
            return new JavaFileView((SourceJavaFile) element);
        }
        if (element instanceof PamelaClassDiagram) {
            PamelaClassDiagram diagram = (PamelaClassDiagram) element;
            PamelaProject project = getProjectForDiagram(diagram);
            PamelaClassDiagramEditor editor =
                    new PamelaClassDiagramEditor(project, diagram);
            // Mark the project dirty whenever the diagram is mutated (entity added/
            // removed, shape moved/resized) so the change is offered for saving.
            editor.installDirtyTracking(() -> markProjectDirty(project));
            diagramEditors.put(diagram, editor);
            JComponent view = editor.getView();
            // Diagram selection drives the inspector + detailed browser, without
            // switching the central view away from the diagram (ui-design.md §18.2).
            // Multi-selection: the full list highlights all matching browser nodes,
            // the lead drives the single-object inspector / context panel.
            editor.setSelectionListener(this::onDiagramSelectionChanged);
            // Right-click on a diagram element opens the same shared contextual menu
            // as the browsers (ui-design.md §18.4).
            editor.setContextualMenuHandler(this::showContextualMenuFor);
            return view;
        }
        return null;
    }

    /** Returns a short display title for a model element. */
    private String titleFor(Object element) {
        if (element instanceof SourceMetaModel) {
            return ((SourceMetaModel) element).getName();
        }
        if (element instanceof PamelaProject) {
            SourceMetaModel mm = ((PamelaProject) element).getMetaModel();
            return mm != null ? mm.getName() : "Project";
        }
        if (element instanceof SourceFolder) {
            return ((SourceFolder) element).getName();
        }
        if (element instanceof SourcePackage) {
            return ((SourcePackage) element).getQualifiedName();
        }
        if (element instanceof SourceModelEntity) {
            return ((SourceModelEntity) element).getSimpleName();
        }
        if (element instanceof SourceJavaFile) {
            return ((SourceJavaFile) element).getSimpleName();
        }
        if (element instanceof PamelaClassDiagram) {
            String name = ((PamelaClassDiagram) element).getName();
            return (name != null && !name.isEmpty()) ? name : "Diagram";
        }
        return element.toString();
    }

    /**
     * Removes the cached view for {@code element} and purges it from the
     * navigation history.  Used when a project is closed or a diagram is deleted.
     */
    private void invalidateCachedView(Object element) {
        viewCache.remove(element);

        // If this element is currently shown, clear the view
        if (element == currentHistoryElement) {
            currentHistoryElement = null;
            centralViewPanel.removeAll();
            centralTitleLabel.setText("");
            centralViewPanel.revalidate();
            centralViewPanel.repaint();
            detachDianaTools();
        }

        // Remove this element from both history stacks
        backHistory.removeIf(e -> e == element);
        forwardHistory.removeIf(e -> e == element);
        updateNavButtons();
    }

    // =========================================================================
    // Diana toolbar attachment
    // =========================================================================

    private void attachDianaTools(PamelaClassDiagramEditor editor) {
        scaleSelector.attachToEditor(editor.getDianaEditor());
        layoutWidget.attachToEditor(editor.getDianaEditor());
        inspectors.attachToEditor(editor.getDianaEditor());
        toolbarPanel.setVisible(true);
        toolbarPanel.revalidate();
    }

    private void detachDianaTools() {
        // Diana bug: several tool widgets throw NPE in attachToEditor(null) when their
        // internal sub-selectors still reference the previous editor instance.
        // Known affected widget: JDianaScaleSelector (handleScaleChanged).
        // Wrap each call individually so that one failing widget does not prevent
        // the others from being detached or the toolbar from being hidden.
        // The toolbar is hidden regardless, so leaving a widget "attached" to the
        // last editor is harmless (it is not visible and cannot be interacted with).
        try { scaleSelector.attachToEditor(null); } catch (Exception ignored) {}
        try { layoutWidget.attachToEditor(null); }  catch (Exception ignored) {}
        toolbarPanel.setVisible(false);
    }

    // =========================================================================
    // Double-click actions (dispatched from both browsers)
    // =========================================================================

    /**
     * Called when the user double-clicks a node in either browser.
     * For most elements this is equivalent to opening their central view.
     */
    public void doubleClickInBrowser(Object element) {
        if (element == null) {
            return;
        }
        openOrSwitchCentralView(element);
    }

    // =========================================================================
    // New diagram action
    // =========================================================================

    /**
     * Menu entry point: creates a new {@link PamelaClassDiagram} for the active
     * project (the one owning the current selection, or the most recently opened
     * one as a fallback). Prompts the user for a diagram name.
     */
    public void newDiagram() {
        PamelaProject project = getProjectForElement(currentSelectedElement);
        if (project == null && !projects.isEmpty()) {
            project = projects.get(projects.size() - 1);
        }
        if (project == null) {
            return;
        }
        newDiagram(project);
    }

    /**
     * Creates a new {@link PamelaClassDiagram} at the root of the given project,
     * prompting the user for its name, then opens it in the central view.
     *
     * @param project the project to add the diagram to
     * @return the created diagram, or {@code null} if the user cancelled or no
     *         diagram factory is available
     */
    public PamelaClassDiagram newDiagram(PamelaProject project) {
        if (project == null || project.getDiagramFactory() == null) {
            return null;
        }
        String name = (String) JOptionPane.showInputDialog(
                frame, "Diagram name:", "New Class Diagram",
                JOptionPane.PLAIN_MESSAGE, null, null, "New diagram");
        if (name == null || name.trim().isEmpty()) {
            return null; // cancelled or empty
        }
        PamelaClassDiagram diagram = project.getDiagramFactory().newDiagram(name.trim());
        // Assign a stable, project-unique id (the sidecar file-name stem).
        diagram.setId(project.generateDiagramId(name.trim()));
        project.addDiagram(diagram);
        markProjectDirty(project);
        // project.addDiagram() fires "diagrams" on the project itself, which is what the
        // MetaModelBrowser's "project.diagrams" children binding actually observes
        // (see PamelaProject.addDiagram()) — so the new node appears without further help here.
        // Navigate to the diagram editor immediately.
        openOrSwitchCentralView(diagram);
        return diagram;
    }

    /**
     * Renames a diagram (display name only; its stable id and file are unchanged),
     * marks the project dirty, and refreshes the browser label and central title.
     */
    public void renameDiagram(PamelaClassDiagram diagram) {
        PamelaProject project = getProjectForDiagram(diagram);
        if (diagram == null || project == null) {
            return;
        }
        String newName = (String) JOptionPane.showInputDialog(
                frame, "Diagram name:", "Rename Class Diagram",
                JOptionPane.PLAIN_MESSAGE, null, null, diagram.getName());
        if (newName == null) {
            return; // cancelled
        }
        newName = newName.trim();
        if (newName.isEmpty() || newName.equals(diagram.getName())) {
            return;
        }
        diagram.setName(newName);
        markProjectDirty(project);
        // diagram.setName(...) is a PAMELA-managed setter: it already fires its own "name"
        // PropertyChangeEvent on the diagram object, which the browser's label binding
        // (diagram.name) observes directly — no extra event needed here.
        if (currentHistoryElement == diagram) {
            centralTitleLabel.setText(titleFor(diagram));
        }
    }

    /**
     * Deletes a diagram after confirmation: removes it from the project, discards its
     * cached view/editor, and marks the project dirty. The {@code .diagram} file is
     * removed from disk on the next save (purge in {@link #saveProject(PamelaProject)}).
     */
    public void deleteDiagram(PamelaClassDiagram diagram) {
        PamelaProject project = getProjectForDiagram(diagram);
        if (diagram == null || project == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(frame,
                "Delete diagram \"" + diagram.getName() + "\"?\n"
                        + "Its .diagram file will be removed when the project is saved.",
                "Delete Class Diagram",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        invalidateCachedView(diagram);
        diagramEditors.remove(diagram);
        project.removeDiagram(diagram);
        markProjectDirty(project);
        // project.removeDiagram() fires "diagrams" on the project itself (see
        // PamelaProject.removeDiagram()) — the browser drops the node without further help here.
    }

    // =========================================================================
    // Application lifecycle
    // =========================================================================

    /** Makes the main frame visible. */
    public void showMainPanel() {
        frame.setVisible(true);
    }

    /** Exits the application, prompting to save each project with unsaved changes. */
    public void quit() {
        for (PamelaProject p : new ArrayList<>(projects)) {
            if (!confirmSaveIfDirty(p)) {
                return; // user cancelled → abort quit
            }
        }
        frame.dispose();
        System.exit(0);
    }

    /**
     * If the project has unsaved changes, asks the user whether to save. Saves on
     * "Yes", proceeds on "No", and reports cancellation.
     *
     * @return {@code true} to proceed with the close/quit, {@code false} to abort
     */
    private boolean confirmSaveIfDirty(PamelaProject project) {
        if (project == null || !project.isDirty()) {
            return true;
        }
        String name = (project.getMetaModel() != null
                && project.getMetaModel().getName() != null)
                ? project.getMetaModel().getName()
                : project.getPamelaFile().getName();
        int choice = JOptionPane.showConfirmDialog(frame,
                "Save changes to \"" + name + "\" before closing?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.CANCEL_OPTION
                || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (choice == JOptionPane.YES_OPTION) {
            saveProject(project);
        }
        return true;
    }

    // =========================================================================
    // Utility / edit menu actions
    // =========================================================================

    public void showLogs() {
        FlexoLoggingViewer.showLoggingViewer(
                FlexoLoggingManager.instance(),
                ApplicationFIBLibraryImpl.instance(), frame);
    }

    public void showLocalizedEditor() {
        if (localizedEditor == null) {
            localizedEditor = new LocalizedEditor(
                    frame, "localized_editor",
                    PAMELA_EDITOR_LOCALIZATION, PAMELA_EDITOR_LOCALIZATION,
                    true, false);
        }
        localizedEditor.setVisible(true);
    }

    /** Opens the thematic preferences window (see {@code preferences-design.md §3}). */
    public void showPreferences() {
        org.openflexo.pamela.editor.ui.preferences.PreferencesDialog.showPreferences(
                frame, PAMELA_EDITOR_LOCALIZATION::localizedForKey, restyleHandler);
    }

    /**
     * Restyle hook used by the preferences dialog (§6bis, step C): copies the current style
     * defaults into every open diagram's embedded styles and refreshes their drawings.
     */
    private final org.openflexo.pamela.editor.ui.preferences.DiagramRestyleHandler restyleHandler =
            new org.openflexo.pamela.editor.ui.preferences.DiagramRestyleHandler() {
                @Override
                public boolean hasOpenDiagrams() {
                    return !diagramEditors.isEmpty();
                }

                @Override
                public void restyleOpenDiagrams() {
                    org.openflexo.pamela.editor.ui.preferences.PreferencesManager prefs =
                            org.openflexo.pamela.editor.ui.preferences.PreferencesManager.getInstance();
                    org.openflexo.pamela.editor.ui.preferences.EntityStylePreferences entityDefaults =
                            prefs.entityStyle();
                    org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreferences connectorDefaults =
                            prefs.connectors();
                    for (java.util.Map.Entry<org.openflexo.pamela.editor.diagram.PamelaClassDiagram,
                            PamelaClassDiagramEditor> e : diagramEditors.entrySet()) {
                        org.openflexo.pamela.editor.diagram.PamelaClassDiagram diagram = e.getKey();
                        // Entity look block ← new default.
                        org.openflexo.pamela.editor.diagram.DiagramStyleSnapshot.copyEntityStyle(
                                entityDefaults, diagram.getEntityStyle());
                        // Each embedded connector style ← its matching catalogue style (by id).
                        if (diagram.getConnectorStyles() != null && connectorDefaults != null) {
                            for (org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreference s
                                    : diagram.getConnectorStyles()) {
                                org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreference src =
                                        connectorDefaults.getStyleById(s.getId());
                                if (src != null) {
                                    org.openflexo.pamela.editor.diagram.DiagramStyleSnapshot
                                            .copyStyleValues(src, s);
                                }
                            }
                            diagram.setDefaultInheritanceStyleId(
                                    connectorDefaults.getDefaultInheritanceStyleId());
                            diagram.setDefaultAssociationStyleId(
                                    connectorDefaults.getDefaultAssociationStyleId());
                        }
                        e.getValue().getDrawing().refreshStyles();
                        markProjectDirty(e.getValue().getProject());
                    }
                }
            };

    /**
     * Installs the macOS application-menu <i>Preferences…</i> handler ({@code ⌘,}) when running
     * on macOS. Returns {@code true} when installed, in which case the menu bar must <em>not</em>
     * add a redundant explicit item.
     *
     * <p>The project targets Java 8, where {@code java.awt.Desktop.setPreferencesHandler} (Java 9+)
     * is unavailable; on macOS Java 8 the handler is set via {@code com.apple.eawt.Application},
     * reached through reflection so the code still compiles on any platform/JDK.</p>
     */
    boolean installMacPreferencesHandler() {
        if (!ToolBox.isMacOS()) {
            return false;
        }
        try {
            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Object application = applicationClass.getMethod("getApplication").invoke(null);
            Class<?> handlerInterface = Class.forName("com.apple.eawt.PreferencesHandler");
            Object handler = java.lang.reflect.Proxy.newProxyInstance(
                    handlerInterface.getClassLoader(),
                    new Class<?>[] { handlerInterface },
                    (proxy, method, args) -> {
                        if ("handlePreferences".equals(method.getName())) {
                            showPreferences();
                        }
                        return null;
                    });
            applicationClass.getMethod("setPreferencesHandler", handlerInterface)
                    .invoke(application, handler);
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public JFrame getFrame() {
        return frame;
    }

    public SwingToolFactory getToolFactory() {
        return toolFactory;
    }

    /**
     * Returns the {@link PamelaClassDiagramEditor} currently shown in the
     * central view, or {@code null} if the current view is not a diagram view.
     */
    public PamelaClassDiagramEditor getActiveDiagramEditor() {
        if (currentHistoryElement instanceof PamelaClassDiagram) {
            return diagramEditors.get((PamelaClassDiagram) currentHistoryElement);
        }
        return null;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Returns the project that owns the given diagram (or null). */
    private PamelaProject getProjectForDiagram(PamelaClassDiagram diagram) {
        for (PamelaProject project : projects) {
            if (project.getDiagrams().contains(diagram)) {
                return project;
            }
        }
        // Fallback: most recent project
        return projects.isEmpty() ? null : projects.get(projects.size() - 1);
    }

    /**
     * Returns the project that the given element belongs to, or {@code null}.
     * Used to decide which project's meta-model to show in the validation panel.
     */
    // =========================================================================
    // Editable inspector — observe edits on the inspected element (Lot 4)
    // =========================================================================

    private Object editListenedElement;
    private final java.beans.PropertyChangeListener editListener = this::onInspectedElementEdited;

    /** Set when the editable source view has unsaved text edits awaiting a model reconcile. */
    private boolean pendingSourceViewReconcile;

    /**
     * Observes the currently inspected element for in-inspector edits. An editable
     * {@code Source*} setter mutates the source and fires a {@code PropertyChange};
     * the application reacts (rebuild) — the model never calls the rebuild itself.
     */
    private void installEditListener(Object element) {
        if (editListenedElement instanceof org.openflexo.toolbox.HasPropertyChangeSupport) {
            ((org.openflexo.toolbox.HasPropertyChangeSupport) editListenedElement)
                    .getPropertyChangeSupport().removePropertyChangeListener(editListener);
        }
        editListenedElement = null;
        if (element instanceof org.openflexo.toolbox.HasPropertyChangeSupport
                && getProjectForElement(element) != null) {
            ((org.openflexo.toolbox.HasPropertyChangeSupport) element)
                    .getPropertyChangeSupport().addPropertyChangeListener(editListener);
            editListenedElement = element;
        }
    }

    private void onInspectedElementEdited(java.beans.PropertyChangeEvent evt) {
        Object element = evt.getSource();
        PamelaProject project = getProjectForElement(element);
        if (project == null) {
            return;
        }
        // UI-intent signal (not a mutation): the inspector's "Change type…" button asked to edit
        // the property type — run the full ChangePropertyType dialog (TypeSelector + custom types)
        // rather than rebuild here. The action performs its own mutation + rebuild.
        if ("changeTypeRequested".equals(evt.getPropertyName())
                && element instanceof SourceModelProperty) {
            new org.openflexo.pamela.editor.ui.action.ChangePropertyTypeAction().perform(element, this);
            return;
        }
        if ("renameRequested".equals(evt.getPropertyName())
                && element instanceof SourceModelProperty) {
            new org.openflexo.pamela.editor.ui.action.RenamePropertyAction().perform(element, this);
            return;
        }
        // UI-intent: the entity inspector's super-entities table "+" footer asked to add a
        // super-entity — run the existing AddSuperEntityAction (its own selection dialog +
        // mutation + rebuild) rather than rebuilding here.
        if ("addSuperEntityRequested".equals(evt.getPropertyName())
                && element instanceof SourceModelEntity) {
            new org.openflexo.pamela.editor.ui.action.AddSuperEntityAction().perform(element, this);
            return;
        }
       // The Source* instance is replaced by the rebuild; capture how to re-resolve
        // the fresh element (by qualified name / property identifier) afterwards.
        final SourceMetaModel model = project.getMetaModel();
        final java.util.function.Supplier<Object> resolveFresh;
        if (element instanceof SourceModelEntity) {
            final String qn = ((SourceModelEntity) element).getQualifiedName();
            resolveFresh = () -> model.getEntity(qn);
        } else if (element instanceof SourceModelProperty) {
            SourceModelProperty p = (SourceModelProperty) element;
            final String entityQN = p.getModelEntity().getQualifiedName();
            final String propertyId = p.getPropertyIdentifier();
            resolveFresh = () -> {
                SourceModelEntity e = model.getEntity(entityQN);
                return e != null ? e.getDeclaredProperties().get(propertyId) : null;
            };
        } else {
            resolveFresh = () -> null;
        }
        rebuildProject(project, () -> {
            Object fresh = resolveFresh.get();
            if (fresh instanceof SourceModelEntity) {
                selectInBrowser(fresh); // re-anchor the browser too
            } else if (fresh != null) {
                // Property (and others): re-inspect with central-view navigation so the
                // detailed browser is rebound to the parent entity (getDetailedBrowserElement)
                // and the central source view refreshes to the edited file. (navigateCentralView
                // = false routes to the diagram soft-selection branch, which is wrong here.)
                setCurrentSelectedElement(fresh, true);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Editable source view (Phase 3) — buffer edits & reconciliation
    // (editable-source-dirty-buffer-design.md §7)
    // -------------------------------------------------------------------------

    /**
     * Called by an editable {@link SourceCodeView} on each user keystroke: the edit is
     * already in the compilation-unit buffer (so the meta-model is dirty); this only
     * flags a pending reconcile and refreshes the unsaved-changes marker.
     */
    public void onSourceBufferEdited() {
        pendingSourceViewReconcile = true;
        updateFrameTitle();
    }

    /**
     * Re-derives the model from the buffers after a source-text edit (focus-loss path, D4).
     * Resolves the owning project from the edited entity (robust to a stale instance), then
     * rebuilds and re-anchors the current selection.
     */
    public void reconcileSourceEdit(SourceModelEntity entity) {
        if (entity == null) {
            return;
        }
        PamelaProject project = null;
        for (PamelaProject p : projects) {
            if (p.getMetaModel() == entity.getMetaModel()) {
                project = p;
                break;
            }
        }
        if (project != null) {
            reconcileCurrentSelection(project);
        }
    }

    /**
     * Rebuilds {@code project} (parse-from-buffer) and, on completion, re-anchors the
     * <em>then-current</em> selection to its fresh instance — read in the callback, not
     * captured, so a selection the user made while the rebuild ran is honoured (no yank
     * back to the edited element).
     */
    private void reconcileCurrentSelection(PamelaProject project) {
        pendingSourceViewReconcile = false;
        final SourceMetaModel model = project.getMetaModel();
        rebuildProject(project, () -> {
            Object sel = currentSelectedElement;
            Object fresh = freshInstanceOf(sel, model);
            if (fresh == null || fresh == sel) {
                return;
            }
            // Re-anchor the selection. For a browser tree node (entity / package) go through
            // selectInBrowser, which re-selects via invokeLater AFTER Gina has rebuilt the
            // tree. A synchronous setCurrentSelectedElement here would be undone by the async
            // tree rebuild: removing the old node clears the tree selection, which writes null
            // back through the two-way "selected" binding and empties the detailed browser.
            // Mirrors onInspectedElementEdited / the action re-anchor path.
            if (fresh instanceof SourceModelEntity || fresh instanceof SourcePackage) {
                selectInBrowser(fresh);
            } else {
                setCurrentSelectedElement(fresh, true);
            }
        });
    }

    /** Re-resolves a (possibly stale) selected element to its fresh instance after a rebuild. */
    private Object freshInstanceOf(Object sel, SourceMetaModel model) {
        if (sel instanceof SourceModelEntity) {
            return model.getEntity(((SourceModelEntity) sel).getQualifiedName());
        }
        if (sel instanceof SourceModelProperty) {
            SourceModelProperty p = (SourceModelProperty) sel;
            SourceModelEntity e = model.getEntity(p.getModelEntity().getQualifiedName());
            return e != null ? e.getDeclaredProperties().get(p.getPropertyIdentifier()) : null;
        }
        if (sel instanceof SourcePackage) {
            return model.getPackage(((SourcePackage) sel).getQualifiedName());
        }
        return sel; // diagrams / project: survive the rebuild unchanged
    }

    public PamelaProject getProjectForElement(Object element) {
        if (element instanceof PamelaProject) {
            return (PamelaProject) element;
        }
        if (element instanceof SourceMetaModel) {
            for (PamelaProject s : projects) {
                if (s.getMetaModel() == element) {
                    return s;
                }
            }
        }
        if (element instanceof SourcePackage) {
            SourcePackage pkg = (SourcePackage) element;
            for (PamelaProject s : projects) {
                if (s.getMetaModel() != null
                        && s.getMetaModel().getAllPackages().contains(pkg)) {
                    return s;
                }
            }
        }
        if (element instanceof SourceModelEntity) {
            SourceModelEntity entity = (SourceModelEntity) element;
            for (PamelaProject s : projects) {
                if (s.getMetaModel() != null
                        && s.getMetaModel().getEntity(entity.getQualifiedName()) == entity) {
                    return s;
                }
            }
        }
        if (element instanceof SourceModelProperty) {
            return getProjectForElement(
                    ((SourceModelProperty) element).getModelEntity());
        }
        if (element instanceof SourceJavaFile) {
            return getProjectForElement(((SourceJavaFile) element).getMetaModel());
        }
        if (element instanceof PromotableMethod) {
            return getProjectForElement(((PromotableMethod) element).getEntity());
        }
        return null;
    }

    // =========================================================================
    // Multi-split layout
    // =========================================================================

    protected static Split<?> getDefaultLayout() {
        Split root = MSL_FACTORY.makeSplit();
        root.setName("ROOT");

        Split<?> left = getVerticalSplit(
                LayoutPosition.TOP_LEFT,    0.5,
                LayoutPosition.BOTTOM_LEFT, 0.5);
        left.setWeight(0.2);
        left.setName(LayoutColumns.LEFT.name());

        Node<?> center = MSL_FACTORY.makeLeaf(LayoutPosition.CENTER.name());
        center.setWeight(0.55);
        center.setName(LayoutColumns.CENTER.name());

        Split<?> right = getVerticalSplit(
                LayoutPosition.TOP_RIGHT,    0.4,
                LayoutPosition.BOTTOM_RIGHT, 0.6);
        right.setWeight(0.25);
        right.setName(LayoutColumns.RIGHT.name());

        root.setChildren(left, MSL_FACTORY.makeDivider(),
                         center, MSL_FACTORY.makeDivider(), right);
        return root;
    }

    protected static Split<?> getVerticalSplit(
            LayoutPosition pos1, double w1,
            LayoutPosition pos2, double w2) {
        Split split = MSL_FACTORY.makeSplit();
        split.setRowLayout(false);
        Leaf<?> l1 = MSL_FACTORY.makeLeaf(pos1.name());
        l1.setWeight(w1);
        Leaf<?> l2 = MSL_FACTORY.makeLeaf(pos2.name());
        l2.setWeight(w2);
        split.setChildren(l1, MSL_FACTORY.makeDivider(), l2);
        return split;
    }
}
