package org.openflexo.pamela.editor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.diana.swing.control.tools.JDianaDialogInspectors;
import org.openflexo.diana.swing.control.tools.JDianaLayoutWidget;
import org.openflexo.diana.swing.control.tools.JDianaScaleSelector;
import org.openflexo.diana.swing.control.tools.JDianaStyles;
import org.openflexo.diana.swing.control.tools.JDianaToolSelector;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.localization.LocalizedEditor;
import org.openflexo.gina.swing.utils.logging.FlexoLoggingViewer;
import org.openflexo.localization.FlexoLocalization;
import org.openflexo.localization.LocalizedDelegate;
import org.openflexo.localization.LocalizedDelegateImpl;
import org.openflexo.logging.FlexoLogger;
import org.openflexo.logging.FlexoLoggingManager;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramSerializer;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.action.AddAsRootTypeAction;
import org.openflexo.pamela.editor.ui.action.AddSourceFolderAction;
import org.openflexo.pamela.editor.ui.action.DeclareAsPamelaEntityAction;
import org.openflexo.pamela.editor.ui.action.NewClassDiagramAction;
import org.openflexo.pamela.editor.ui.action.RenameClassDiagramAction;
import org.openflexo.pamela.editor.ui.action.DeleteClassDiagramAction;
import org.openflexo.pamela.editor.ui.action.ContextualAction;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;
import org.openflexo.pamela.editor.ui.widget.DetailedBrowser;
import org.openflexo.pamela.editor.ui.widget.MetaModelBrowser;
import org.openflexo.pamela.editor.ui.widget.MetaModelSummaryView;
import org.openflexo.pamela.editor.ui.widget.PackageSummaryView;
import org.openflexo.pamela.editor.ui.widget.PamelaEditorInspectorController;
import org.openflexo.pamela.editor.ui.widget.JavaFileView;
import org.openflexo.pamela.editor.ui.widget.SourceCodeView;
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

    private final JDianaToolSelector toolSelector;
    private final JDianaScaleSelector scaleSelector;
    private final JDianaLayoutWidget layoutWidget;
    private final JDianaStyles stylesWidget;
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

    /** Placeholder for the right-bottom validation area. */
    private final JPanel validationArea;

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
    LocalizedEditor localizedEditor;

    /** Used by {@link PamelaEditorMenuBar} to manage PropertyChange listener registrations. */
    final PropertyChangeListenerRegistrationManager manager =
            new PropertyChangeListenerRegistrationManager();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public PamelaEditorApplication() {
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
        toolSelector  = toolFactory.makeDianaToolSelector(null);
        stylesWidget  = toolFactory.makeDianaStyles();
        scaleSelector = toolFactory.makeDianaScaleSelector(null);
        layoutWidget  = toolFactory.makeDianaLayoutWidget();
        inspectors    = toolFactory.makeDianaDialogInspectors();

        toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbarPanel.add(toolSelector.getComponent());
        toolbarPanel.add(stylesWidget.getComponent());
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

        JPanel centerColumn = new JPanel(new BorderLayout());
        centerColumn.add(topBarPanel,      BorderLayout.NORTH);
        centerColumn.add(centralViewPanel, BorderLayout.CENTER);

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

        // --- Right column: validation area (bottom) ---
        validationArea = new JPanel(new BorderLayout());
        validationArea.add(new JLabel("Validation panel", JLabel.CENTER),
                BorderLayout.CENTER);

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
        splitPane.add(validationArea,    LayoutPosition.BOTTOM_RIGHT.name());

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(buildStatusBar(), BorderLayout.SOUTH);

        // --- Menu bar ---
        menuBar = new PamelaEditorMenuBar(this);
        frame.setJMenuBar(menuBar);

        // --- Contextual actions ---
        registerAction(new AddSourceFolderAction());
        registerAction(new AddAsRootTypeAction());
        registerAction(new DeclareAsPamelaEntityAction());
        registerAction(new NewClassDiagramAction());
        registerAction(new RenameClassDiagramAction());
        registerAction(new DeleteClassDiagramAction());

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
                SourceMetaModel metaModel = SourceMetaModelSerializer.load(pamelaFile, pl);
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

                    // Notify Gina bindings that the projects list has changed
                    pcSupport.firePropertyChange("projects",
                            Collections.unmodifiableList(oldProjects),
                            Collections.unmodifiableList(projects));

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

                    pcSupport.firePropertyChange("projects",
                            Collections.unmodifiableList(oldProjects),
                            Collections.unmodifiableList(projects));

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
                // A rebuild recreates all Source* objects and may have changed files
                // on disk. Drop cached views backed by those (everything except
                // diagram editors) so they are recreated fresh from disk on next show.
                invalidateSourceViews();

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
                pcs.firePropertyChange("entitiesCount", -1, metaModel.getEntitiesCount());
                pcs.firePropertyChange("packagesCount", -1, metaModel.getPackagesCount());
                pcs.firePropertyChange("totalPropertiesCount", -1, metaModel.getTotalPropertiesCount());
                pcs.firePropertyChange("abstractEntitiesCount", -1, metaModel.getAbstractEntitiesCount());
                pcs.firePropertyChange("totalInitializersCount", -1, metaModel.getTotalInitializersCount());
                pcs.firePropertyChange("issuesCount", -1, metaModel.getIssuesCount());

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
    private void invalidateSourceViews() {
        viewCache.keySet().removeIf(key -> !(key instanceof PamelaClassDiagram));
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
        List<PamelaProject> oldProjects = new ArrayList<>(projects);
        projects.remove(project);
        pcSupport.firePropertyChange("projects",
                Collections.unmodifiableList(oldProjects),
                Collections.unmodifiableList(projects));
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
        if (settingSelectedElement) {
            return; // discard re-entrant calls from Gina's internal rebind machinery
        }
        settingSelectedElement = true;
        try {
            this.currentSelectedElement = element;
            try {
                // 1. Rebind the detailed browser to the "described" element.
                //    For child elements (property, initializer, impl class) this is the
                //    parent entity, so the browser stays populated.
                detailedBrowser.setEditedObject(getDetailedBrowserElement(element));
            } catch (Exception e) {
                logger.warning("DetailedBrowser rebind failed: " + e.getMessage());
                e.printStackTrace();
            }
            try {
                // 2. Refresh the inspector (always for the actual selected element)
                inspectorController.inspectObject(element);
            } catch (Exception e) {
                logger.warning("Inspector refresh failed: " + e.getMessage());
                e.printStackTrace();
            }
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
    private Object getDetailedBrowserElement(Object element) {
        if (element instanceof SourceModelProperty) {
            return ((SourceModelProperty) element).getModelEntity();
        }
        if (element instanceof org.openflexo.pamela.editor.model.SourceModelInitializer) {
            return ((org.openflexo.pamela.editor.model.SourceModelInitializer) element).getEntity();
        }
        if (element instanceof org.openflexo.pamela.editor.model.SourceImplementationClass) {
            return ((org.openflexo.pamela.editor.model.SourceImplementationClass) element).getEntity();
        }
        return element;
    }

    /** Called from MetaModelBrowserFIBController on single-click. */
    private void onBrowserSelectionChanged(Object element) {
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
            return;
        }

        // Already showing this element — do nothing
        if (element == currentHistoryElement) {
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
            return new MetaModelSummaryView((SourceMetaModel) element);
        }
        if (element instanceof PamelaProject) {
            PamelaProject project = (PamelaProject) element;
            MetaModelSummaryView view = new MetaModelSummaryView(project.getMetaModel());
            File pamelaFile = project.getPamelaFile();
            if (pamelaFile != null && pamelaFile.getParentFile() != null) {
                view.setProjectDirectory(pamelaFile.getParentFile());
            }
            return view;
        }
        if (element instanceof SourcePackage) {
            return new PackageSummaryView((SourcePackage) element);
        }
        if (element instanceof SourceModelEntity) {
            return new SourceCodeView((SourceModelEntity) element);
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
            return editor.getView();
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
        toolSelector.attachToEditor(editor.getDianaEditor());
        stylesWidget.attachToEditor(editor.getDianaEditor());
        scaleSelector.attachToEditor(editor.getDianaEditor());
        layoutWidget.attachToEditor(editor.getDianaEditor());
        inspectors.attachToEditor(editor.getDianaEditor());
        toolbarPanel.setVisible(true);
        toolbarPanel.revalidate();
    }

    private void detachDianaTools() {
        // Diana bug: several tool widgets throw NPE in attachToEditor(null) when their
        // internal sub-selectors still reference the previous editor instance.
        // Known affected widgets: JDianaStyles (backgroundSelector / shapeSelector),
        //                         JDianaScaleSelector (handleScaleChanged).
        // Wrap each call individually so that one failing widget does not prevent
        // the others from being detached or the toolbar from being hidden.
        // The toolbar is hidden regardless, so leaving a widget "attached" to the
        // last editor is harmless (it is not visible and cannot be interacted with).
        try { toolSelector.attachToEditor(null); }  catch (Exception ignored) {}
        // stylesWidget: known NPE — leave attached to last editor (hidden)
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
        // Force the MetaModelBrowser to rebuild so the new diagram node appears under
        // the project. The projects list content is unchanged (only a project's internal
        // diagrams list grew), so we pass null as old value to bypass the equals() guard
        // in PropertyChangeSupport and guarantee the event fires.
        pcSupport.firePropertyChange("projects", null,
                Collections.unmodifiableList(projects));
        // Navigate to the diagram editor immediately
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
        // Refresh the browser node label and the central title if this diagram is shown.
        pcSupport.firePropertyChange("projects", null,
                Collections.unmodifiableList(projects));
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
        pcSupport.firePropertyChange("projects", null,
                Collections.unmodifiableList(projects));
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
    private PamelaProject getProjectForElement(Object element) {
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
