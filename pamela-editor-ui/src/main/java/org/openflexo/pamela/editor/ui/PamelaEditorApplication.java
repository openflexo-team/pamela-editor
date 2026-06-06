package org.openflexo.pamela.editor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.diana.swing.control.tools.JDianaDialogInspectors;
import org.openflexo.diana.swing.control.tools.JDianaLayoutWidget;
import org.openflexo.diana.swing.control.tools.JDianaScaleSelector;
import org.openflexo.diana.swing.control.tools.JDianaStyles;
import org.openflexo.diana.swing.control.tools.JDianaToolSelector;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
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
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramEditor;
import org.openflexo.pamela.editor.ui.widget.DetailedBrowser;
import org.openflexo.pamela.editor.ui.widget.MetaModelBrowser;
import org.openflexo.pamela.editor.ui.widget.MetaModelSummaryView;
import org.openflexo.pamela.editor.ui.widget.PackageSummaryView;
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
 *   <li>LEFT top  — {@link MetaModelBrowser} (project tree)</li>
 *   <li>LEFT bottom — {@link DetailedBrowser} (content of selected element)</li>
 *   <li>CENTER — {@link JTabbedPane} (source code view, diagram view, etc.)</li>
 *   <li>RIGHT top — inspector (dynamically-swapped FIB panel)</li>
 *   <li>RIGHT bottom — validation panel (placeholder)</li>
 * </ul>
 * </p>
 *
 * <p>Selection model: a single {@link #currentSelectedElement} ({@code Object}).
 * Changing it updates the detailed browser, the inspector, and the central tab.</p>
 */
public class PamelaEditorApplication {

    private static final Logger logger =
            FlexoLogger.getLogger(PamelaEditorApplication.class.getPackage().getName());

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
    // Central column
    // -------------------------------------------------------------------------

    private final JTabbedPane centralTabbedPane;

    /**
     * Map from model element (identity) → the Swing component displayed for it.
     * Used to avoid opening duplicate tabs and to switch to existing tabs.
     */
    private final Map<Object, JComponent> openTabs =
            new IdentityHashMap<>();

    /**
     * Map from {@link PamelaClassDiagram} → its editor (for refresh and
     * tool-attachment on tab switch).
     */
    private final Map<PamelaClassDiagram, PamelaClassDiagramEditor> diagramEditors =
            new IdentityHashMap<>();

    // -------------------------------------------------------------------------
    // Right column
    // -------------------------------------------------------------------------

    /** Container for the dynamically-swapped inspector FIB panel. */
    private final JPanel inspectorArea;

    /** The currently displayed inspector panel (may be null). */
    private FIBJPanel<?> currentInspector;

    /** Placeholder for the right-bottom validation area. */
    private final JPanel validationArea;

    // -------------------------------------------------------------------------
    // Application state
    // -------------------------------------------------------------------------

    /** Open sessions (projects). Exposed to the MetaModelBrowser FIB as {@code data.sessions}. */
    private final List<PamelaEditorSession> sessions = new ArrayList<>();

    /** The currently selected element (any Source* or PamelaClassDiagram). */
    private Object currentSelectedElement;

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

        // --- Left column: MetaModelBrowser (top) ---
        metaModelBrowser = new MetaModelBrowser(this);
        // Wire selection: MetaModelBrowser → application
        metaModelBrowser.getController().getPropertyChangeSupport()
                .addPropertyChangeListener("selectedElement", evt ->
                        onBrowserSelectionChanged(evt.getNewValue()));

        // --- Left column: DetailedBrowser (bottom) ---
        detailedBrowser = new DetailedBrowser(this);

        // --- Central column: tabbed pane ---
        centralTabbedPane = new JTabbedPane();
        centralTabbedPane.addChangeListener(e -> onCentralTabChanged());

        JPanel centerColumn = new JPanel(new BorderLayout());
        centerColumn.add(toolbarPanel, BorderLayout.NORTH);
        centerColumn.add(centralTabbedPane, BorderLayout.CENTER);

        // --- Right column: inspector area (top) ---
        inspectorArea = new JPanel(new BorderLayout());
        inspectorArea.add(new JLabel("No selection", JLabel.CENTER),
                BorderLayout.CENTER);

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
        splitPane.add(inspectorArea,     LayoutPosition.TOP_RIGHT.name());
        splitPane.add(validationArea,    LayoutPosition.BOTTOM_RIGHT.name());

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        // --- Menu bar ---
        menuBar = new PamelaEditorMenuBar(this);
        frame.setJMenuBar(menuBar);

        frame.validate();
        frame.pack();
    }

    // =========================================================================
    // Session management (exposed to FIB as data.sessions)
    // =========================================================================

    /**
     * Returns the list of open sessions.
     * <p>This is the root of the MetaModelBrowser tree via
     * {@code data.sessions} in the FIB file.</p>
     */
    public List<PamelaEditorSession> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    /**
     * Opens a {@code .pamela} project file.
     * Builds the {@link SourceMetaModel} via Spoon, creates a
     * {@link PamelaEditorSession}, and refreshes the browser.
     */
    public void openProject(File pamelaFile) {
        if (pamelaFile == null || !pamelaFile.exists()) {
            return;
        }
        try {
            // 1. Build meta-model from source analysis
            SourceMetaModel metaModel = SourceMetaModelSerializer.load(pamelaFile);

            // 2. Create session
            PamelaEditorSession session = new PamelaEditorSession(pamelaFile, metaModel);
            session.setToolFactory(toolFactory);

            // 3. Load diagram sidecars listed in the .pamela file
            File projectDir = pamelaFile.getParentFile();
            if (projectDir == null) {
                projectDir = new java.io.File(".");
            }
            try {
                java.util.List<String> diagramFileNames =
                        SourceMetaModelSerializer.loadDiagramFileNames(pamelaFile);
                for (String fileName : diagramFileNames) {
                    File diagramFile = new File(projectDir, fileName);
                    if (diagramFile.exists()) {
                        try {
                            PamelaClassDiagram diagram =
                                    PamelaClassDiagramSerializer.load(diagramFile, session);
                            session.addDiagram(diagram);
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
                logger.warning("Failed to read diagram list from " + pamelaFile + ": "
                        + de.getMessage());
            }

            sessions.add(session);
            PamelaEditorPreferences.setLastFile(pamelaFile);
            // Rebind the browser to the application (refreshes the tree)
            metaModelBrowser.setEditedObject(this);
        } catch (Exception e) {
            logger.severe("Failed to open project " + pamelaFile + ": " + e.getMessage());
        }
    }

    /** Opens a file chooser and loads the selected {@code .pamela} project. */
    public void openProjectFromChooser() {
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            openProject(fileChooser.getSelectedFile());
        }
    }

    /** Saves the currently active session's {@code .pamela} file and all diagram sidecars. */
    public void saveActiveProject() {
        PamelaEditorSession session = getSessionForElement(currentSelectedElement);
        if (session == null && !sessions.isEmpty()) {
            session = sessions.get(sessions.size() - 1);
        }
        if (session == null) {
            return;
        }
        File projectDir = session.getPamelaFile().getParentFile();
        if (projectDir == null) {
            projectDir = new java.io.File(".");
        }

        // 1. Save diagram sidecars and collect their file names
        java.util.List<String> diagramFileNames = new java.util.ArrayList<>();
        for (PamelaClassDiagram diagram : session.getDiagrams()) {
            String fileName = PamelaClassDiagramSerializer.sidecarFileName(diagram.getName());
            diagramFileNames.add(fileName);
            File diagramFile = new File(projectDir, fileName);
            try {
                PamelaClassDiagramSerializer.save(diagram, diagramFile);
            } catch (Exception de) {
                logger.warning("Failed to save diagram sidecar "
                        + diagramFile + ": " + de.getMessage());
            }
        }

        // 2. Save the .pamela file (includes the diagrams list)
        try {
            SourceMetaModelSerializer.save(
                    session.getMetaModel(),
                    session.getPamelaFile(),
                    diagramFileNames);
        } catch (Exception e) {
            logger.severe("Failed to save project: " + e.getMessage());
        }
    }

    /** Closes a session and removes all related tabs and browser entries. */
    public void closeSession(PamelaEditorSession session) {
        if (session == null) {
            return;
        }
        // Close all tabs that belong to this session
        for (PamelaClassDiagram diagram : session.getDiagrams()) {
            closeDiagramTab(diagram);
        }
        sessions.remove(session);
        metaModelBrowser.setEditedObject(this);
    }

    // =========================================================================
    // Selection model
    // =========================================================================

    /**
     * Called when the user clicks a node in either the {@link MetaModelBrowser}
     * or the {@link DetailedBrowser}.
     */
    public void setCurrentSelectedElement(Object element) {
        this.currentSelectedElement = element;
        // 1. Rebind the detailed browser
        detailedBrowser.setEditedObject(element);
        // 2. Refresh the inspector
        refreshInspector(element);
        // 3. Open or switch the central tab
        openOrSwitchCentralView(element);
    }

    /** Called from MetaModelBrowserFIBController on single-click. */
    private void onBrowserSelectionChanged(Object element) {
        setCurrentSelectedElement(element);
    }

    // =========================================================================
    // Central tab management (ui-design.md §5)
    // =========================================================================

    /**
     * Opens a new central tab for {@code element}, or switches to the existing
     * one if it is already open.  For elements without their own view
     * (e.g. {@link SourceModelProperty}), the nearest ancestor view is shown.
     */
    public void openOrSwitchCentralView(Object element) {
        if (element == null) {
            return;
        }

        // Elements without their own tab: delegate to parent
        if (element instanceof SourceModelProperty) {
            SourceModelProperty prop = (SourceModelProperty) element;
            SourceModelEntity entity = prop.getModelEntity();
            openOrSwitchCentralView(entity);
            // Highlight the property methods in the source code view
            JComponent tab = openTabs.get(entity);
            if (tab instanceof SourceCodeView) {
                ((SourceCodeView) tab).highlightProperty(prop);
            }
            return;
        }

        // Find or create the Swing component for this element
        JComponent view = openTabs.get(element);
        if (view == null) {
            view = createViewFor(element);
            if (view == null) {
                return; // no view for this element type
            }
            String title = titleFor(element);
            openTabs.put(element, view);
            centralTabbedPane.addTab(title, view);
        }

        // Switch to the tab
        int idx = centralTabbedPane.indexOfComponent(view);
        if (idx >= 0) {
            centralTabbedPane.setSelectedIndex(idx);
        }
    }

    /**
     * Creates the Swing component for a model element.
     * Returns {@code null} if this element type has no dedicated central view.
     */
    private JComponent createViewFor(Object element) {
        if (element instanceof SourceMetaModel) {
            return new MetaModelSummaryView((SourceMetaModel) element);
        }
        if (element instanceof PamelaEditorSession) {
            return new MetaModelSummaryView(
                    ((PamelaEditorSession) element).getMetaModel());
        }
        if (element instanceof SourcePackage) {
            return new PackageSummaryView((SourcePackage) element);
        }
        if (element instanceof SourceModelEntity) {
            return new SourceCodeView((SourceModelEntity) element);
        }
        if (element instanceof PamelaClassDiagram) {
            PamelaClassDiagram diagram = (PamelaClassDiagram) element;
            // Find the session owning this diagram
            PamelaEditorSession session = getSessionForDiagram(diagram);
            PamelaClassDiagramEditor editor =
                    new PamelaClassDiagramEditor(session, diagram);
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
        if (element instanceof PamelaEditorSession) {
            SourceMetaModel mm = ((PamelaEditorSession) element).getMetaModel();
            return mm != null ? mm.getName() : "Project";
        }
        if (element instanceof SourcePackage) {
            return ((SourcePackage) element).getQualifiedName();
        }
        if (element instanceof SourceModelEntity) {
            return ((SourceModelEntity) element).getSimpleName();
        }
        if (element instanceof PamelaClassDiagram) {
            String name = ((PamelaClassDiagram) element).getName();
            return (name != null && !name.isEmpty()) ? name : "Diagram";
        }
        return element.toString();
    }

    /** Closes the central tab displaying the given diagram. */
    private void closeDiagramTab(PamelaClassDiagram diagram) {
        JComponent view = openTabs.remove(diagram);
        if (view != null) {
            int idx = centralTabbedPane.indexOfComponent(view);
            if (idx >= 0) {
                centralTabbedPane.remove(idx);
            }
        }
        diagramEditors.remove(diagram);
    }

    /** Called when the user switches tabs manually. */
    private void onCentralTabChanged() {
        int idx = centralTabbedPane.getSelectedIndex();
        if (idx < 0) {
            detachDianaTools();
            return;
        }
        JComponent view = (JComponent) centralTabbedPane.getComponentAt(idx);
        // Attach Diana tools if the selected tab is a diagram
        PamelaClassDiagramEditor diagramEditor = findDiagramEditor(view);
        if (diagramEditor != null) {
            attachDianaTools(diagramEditor);
        } else {
            detachDianaTools();
        }
    }

    /** Finds the {@link PamelaClassDiagramEditor} whose view is currently displayed. */
    private PamelaClassDiagramEditor findDiagramEditor(JComponent view) {
        for (Map.Entry<PamelaClassDiagram, PamelaClassDiagramEditor> entry
                : diagramEditors.entrySet()) {
            if (entry.getValue().getView() == view) {
                return entry.getValue();
            }
        }
        return null;
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
    }

    private void detachDianaTools() {
        toolSelector.attachToEditor(null);
        stylesWidget.attachToEditor(null);
        scaleSelector.attachToEditor(null);
        layoutWidget.attachToEditor(null);
    }

    // =========================================================================
    // Inspector (right top — Strategy B: FIB file swap)
    // =========================================================================

    private void refreshInspector(Object element) {
        Resource fibFile = getInspectorFibFor(element);
        if (fibFile == null) {
            // Clear inspector
            inspectorArea.removeAll();
            inspectorArea.add(new JLabel("No inspector", JLabel.CENTER),
                    BorderLayout.CENTER);
            currentInspector = null;
        } else {
            // If the same FIB file is already loaded for the same type, just rebind
            if (currentInspector != null
                    && currentInspector.getRepresentedType() != null
                    && currentInspector.getRepresentedType().isInstance(element)) {
                @SuppressWarnings("unchecked")
                FIBJPanel<Object> typed = (FIBJPanel<Object>) currentInspector;
                typed.setEditedObject(element);
            } else {
                inspectorArea.removeAll();
                FIBJPanel<Object> newInspector = buildInspector(fibFile, element);
                currentInspector = newInspector;
                if (newInspector != null) {
                    inspectorArea.add(newInspector, BorderLayout.CENTER);
                }
            }
        }
        inspectorArea.revalidate();
        inspectorArea.repaint();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private FIBJPanel<Object> buildInspector(Resource fibFile, Object element) {
        try {
            return new FIBJPanel(fibFile, element,
                    ApplicationFIBLibraryImpl.instance(),
                    PAMELA_EDITOR_LOCALIZATION) {
                @Override
                public Class getRepresentedType() {
                    return element != null ? element.getClass() : Object.class;
                }
                @Override
                public void delete() {}
            };
        } catch (Exception e) {
            logger.warning("Failed to build inspector for " + element + ": " + e.getMessage());
            return null;
        }
    }

    private Resource getInspectorFibFor(Object element) {
        if (element instanceof SourceModelEntity) {
            return ResourceLocator.locateResource("Fib/Inspector/EntityInspector.fib");
        }
        if (element instanceof SourceModelProperty) {
            return ResourceLocator.locateResource("Fib/Inspector/PropertyInspector.fib");
        }
        if (element instanceof SourcePackage) {
            return ResourceLocator.locateResource("Fib/Inspector/PackageInspector.fib");
        }
        if (element instanceof SourceMetaModel) {
            return ResourceLocator.locateResource("Fib/Inspector/MetaModelInspector.fib");
        }
        if (element instanceof PamelaEditorSession) {
            SourceMetaModel mm = ((PamelaEditorSession) element).getMetaModel();
            return mm != null
                    ? ResourceLocator.locateResource("Fib/Inspector/MetaModelInspector.fib")
                    : null;
        }
        if (element instanceof PamelaClassDiagram) {
            return ResourceLocator.locateResource("Fib/Inspector/DiagramInspector.fib");
        }
        return null;
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
        if (element instanceof PamelaClassDiagram) {
            // Double-click on a diagram in the browser: open/switch diagram tab
            openOrSwitchCentralView(element);
        } else {
            openOrSwitchCentralView(element);
        }
    }

    // =========================================================================
    // New diagram action
    // =========================================================================

    /** Creates a new {@link PamelaClassDiagram} for the most recently opened session. */
    public void newDiagram() {
        if (sessions.isEmpty()) {
            return;
        }
        PamelaEditorSession session = sessions.get(sessions.size() - 1);
        if (session.getDiagramFactory() == null) {
            return;
        }
        PamelaClassDiagram diagram = session.getDiagramFactory().newDiagram("New diagram");
        session.addDiagram(diagram);
        // Refresh browser tree
        metaModelBrowser.setEditedObject(this);
        // Open the diagram editor immediately
        openOrSwitchCentralView(diagram);
    }

    // =========================================================================
    // Application lifecycle
    // =========================================================================

    /** Makes the main frame visible. */
    public void showMainPanel() {
        frame.setVisible(true);
    }

    /** Exits the application. */
    public void quit() {
        frame.dispose();
        System.exit(0);
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
     * Returns the {@link PamelaClassDiagramEditor} currently shown in the active
     * central tab, or {@code null} if the active tab is not a diagram view.
     */
    public PamelaClassDiagramEditor getActiveDiagramEditor() {
        int idx = centralTabbedPane.getSelectedIndex();
        if (idx < 0) {
            return null;
        }
        JComponent view = (JComponent) centralTabbedPane.getComponentAt(idx);
        return findDiagramEditor(view);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Returns the session that owns the given diagram (or null). */
    private PamelaEditorSession getSessionForDiagram(PamelaClassDiagram diagram) {
        for (PamelaEditorSession session : sessions) {
            if (session.getDiagrams().contains(diagram)) {
                return session;
            }
        }
        // Fallback: most recent session
        return sessions.isEmpty() ? null : sessions.get(sessions.size() - 1);
    }

    /**
     * Returns the session that the given element belongs to, or {@code null}.
     * Used to decide which session's meta-model to show in the validation panel.
     */
    private PamelaEditorSession getSessionForElement(Object element) {
        if (element instanceof PamelaEditorSession) {
            return (PamelaEditorSession) element;
        }
        if (element instanceof SourceMetaModel) {
            for (PamelaEditorSession s : sessions) {
                if (s.getMetaModel() == element) {
                    return s;
                }
            }
        }
        if (element instanceof SourcePackage) {
            SourcePackage pkg = (SourcePackage) element;
            for (PamelaEditorSession s : sessions) {
                if (s.getMetaModel() != null
                        && s.getMetaModel().getAllPackages().contains(pkg)) {
                    return s;
                }
            }
        }
        if (element instanceof SourceModelEntity) {
            SourceModelEntity entity = (SourceModelEntity) element;
            for (PamelaEditorSession s : sessions) {
                if (s.getMetaModel() != null
                        && s.getMetaModel().getEntity(entity.getQualifiedName()) == entity) {
                    return s;
                }
            }
        }
        if (element instanceof SourceModelProperty) {
            return getSessionForElement(
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
