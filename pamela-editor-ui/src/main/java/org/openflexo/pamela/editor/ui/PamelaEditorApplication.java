/**
 * 
 * Copyright (c) 2013-2014, Openflexo
 * Copyright (c) 2011-2012, AgileBirds
 * 
 * This file is part of Diana-drawing-editor, a component of the software infrastructure 
 * developed at Openflexo.
 * 
 * 
 * Openflexo is dual-licensed under the European Union Public License (EUPL, either 
 * version 1.1 of the License, or any later version ), which is available at 
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * and the GNU General Public License (GPL, either version 3 of the License, or any 
 * later version), which is available at http://www.gnu.org/licenses/gpl.html .
 * 
 * You can redistribute it and/or modify under the terms of either of these licenses
 * 
 * If you choose to redistribute it and/or modify under the terms of the GNU GPL, you
 * must include the following additional permission.
 *
 *          Additional permission under GNU GPL version 3 section 7
 *
 *          If you modify this Program, or any covered work, by linking or 
 *          combining it with software containing parts covered by the terms 
 *          of EPL 1.0, the licensors of this Program grant you additional permission
 *          to convey the resulting work. * 
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. 
 *
 * See http://www.openflexo.org/license.html for details.
 * 
 * 
 * Please contact Openflexo (openflexo-contacts@openflexo.org)
 * or visit www.openflexo.org if you need additional information.
 * 
 */

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
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.diana.swing.control.tools.JDianaDialogInspectors;
import org.openflexo.diana.swing.control.tools.JDianaLayoutWidget;
import org.openflexo.diana.swing.control.tools.JDianaPalette;
import org.openflexo.diana.swing.control.tools.JDianaScaleSelector;
import org.openflexo.diana.swing.control.tools.JDianaStyles;
import org.openflexo.diana.swing.control.tools.JDianaToolSelector;
import org.openflexo.exceptions.CopyException;
import org.openflexo.exceptions.CutException;
import org.openflexo.exceptions.PasteException;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.localization.LocalizedEditor;
import org.openflexo.gina.swing.utils.logging.FlexoLoggingViewer;
import org.openflexo.localization.FlexoLocalization;
import org.openflexo.localization.LocalizedDelegate;
import org.openflexo.localization.LocalizedDelegateImpl;
import org.openflexo.logging.FlexoLogger;
import org.openflexo.logging.FlexoLoggingManager;
import org.openflexo.pamela.editor.SourceMetaModel;
import org.openflexo.pamela.editor.diagram.ClassDiagram;
import org.openflexo.pamela.editor.diagram.ClassDiagramFactory;
import org.openflexo.pamela.editor.ui.diagram.ClassDiagramEditingContext;
import org.openflexo.pamela.editor.ui.diagram.ClassDiagramEditor;
import org.openflexo.pamela.editor.ui.diagram.ClassDiagramEditorPalette;
import org.openflexo.pamela.editor.ui.diagram.ClassDiagramEditorView;
import org.openflexo.pamela.editor.ui.widget.MetaModelBrowser;
import org.openflexo.pamela.exceptions.ModelDefinitionException;
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
 * Represents the Pamela Editor application
 * 
 * @author sylvain
 * 
 */
public class PamelaEditorApplication {

	private static final Logger logger = FlexoLogger.getLogger(PamelaEditorApplication.class.getPackage().getName());

	public static LocalizedDelegate PAMELA_EDITOR_LOCALIZATION = new LocalizedDelegateImpl(
			ResourceLocator.locateResource("PamelaLocalization/PamelaEditor"), FlexoLocalization.getMainLocalizer(), true, true);

	static final int META_MASK = ToolBox.isMacOS() ? InputEvent.META_MASK : InputEvent.CTRL_MASK;

	protected static final MultiSplitLayoutFactory MSL_FACTORY = new MultiSplitLayoutFactory.DefaultMultiSplitLayoutFactory();

	private JXMultiSplitPane centerPanel;

	final JFrame frame;
	final JDialog paletteDialog;
	private final FlexoFileChooser fileChooser;
	private final SwingToolFactory toolFactory;

	// private JFIBInspectorController inspector;

	private ClassDiagramFactory factory;

	private MetaModelBrowser metaModelBrowser;

	private final Vector<ClassDiagramEditor> classDiagramEditors = new Vector<>();
	private final JPanel mainPanel;
	private JTabbedPane tabbedPane;

	ClassDiagramEditor currentClassDiagramEditor;

	private final JDianaToolSelector toolSelector;
	private final JDianaScaleSelector scaleSelector;
	private final JDianaLayoutWidget layoutWidget;
	private final JDianaStyles stylesWidget;
	private final JDianaPalette commonPalette;
	private final ClassDiagramEditorPalette commonPaletteModel;
	final JDianaDialogInspectors inspectors;

	protected PropertyChangeListenerRegistrationManager manager;

	private final ClassDiagramEditingContext editingContext;

	LocalizedEditor localizedEditor;

	final FileSystemResourceLocatorImpl resourceLocator;

	private PamelaEditorMenuBar menuBar;

	public PamelaEditorApplication() {
		super();

		editingContext = new ClassDiagramEditingContext();

		try {
			factory = new ClassDiagramFactory(editingContext);
		} catch (ModelDefinitionException e1) {
			e1.printStackTrace();
		}

		frame = new JFrame();

		frame.setBounds(PamelaEditorPreferences.getFrameBounds());
		new ComponentBoundSaver(frame) {

			@Override
			public void saveBounds(Rectangle bounds) {
				PamelaEditorPreferences.setFrameBounds(bounds);
			}
		};
		fileChooser = new FlexoFileChooser(frame);
		fileChooser.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "*.fib *.inspector";
			}

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".drw");
			}
		});
		fileChooser.setCurrentDirectory(PamelaEditorPreferences.getLastDirectory());

		resourceLocator = new FileSystemResourceLocatorImpl();
		if (PamelaEditorPreferences.getLastDirectory() != null) {
			resourceLocator.appendToDirectories(PamelaEditorPreferences.getLastDirectory().getAbsolutePath());
		}
		resourceLocator.appendToDirectories(System.getProperty("user.home"));
		ResourceLocator.appendDelegate(resourceLocator);

		frame.setPreferredSize(new Dimension(1100, 800));

		toolFactory = new SwingToolFactory(frame);

		// inspector = new JFIBInspectorController(frame);

		frame.setTitle("PAMELA Editor");

		Split<?> defaultLayout = getDefaultLayout();

		MultiSplitLayout centerLayout = new MultiSplitLayout(true, MSL_FACTORY);
		centerLayout.setLayoutMode(MultiSplitLayout.NO_MIN_SIZE_LAYOUT);
		centerLayout.setModel(defaultLayout);

		centerPanel = new JXMultiSplitPane(centerLayout);
		centerPanel.setDividerSize(DIVIDER_SIZE);
		centerPanel.setDividerPainter(new DividerPainter() {

			@Override
			protected void doPaint(Graphics2D g, Divider divider, int width, int height) {
				if (!divider.isVisible()) {
					return;
				}
				if (divider.isVertical()) {
					int x = (width - KNOB_SIZE) / 2;
					int y = (height - DIVIDER_KNOB_SIZE) / 2;
					for (int i = 0; i < 3; i++) {
						Graphics2D graph = (Graphics2D) g.create(x, y + i * (KNOB_SIZE + KNOB_SPACE), KNOB_SIZE + 1, KNOB_SIZE + 1);
						graph.setPaint(KNOB_PAINTER);
						graph.fillOval(0, 0, KNOB_SIZE, KNOB_SIZE);
					}
				}
				else {
					int x = (width - DIVIDER_KNOB_SIZE) / 2;
					int y = (height - KNOB_SIZE) / 2;
					for (int i = 0; i < 3; i++) {
						Graphics2D graph = (Graphics2D) g.create(x + i * (KNOB_SIZE + KNOB_SPACE), y, KNOB_SIZE + 1, KNOB_SIZE + 1);
						graph.setPaint(KNOB_PAINTER);
						graph.fillOval(0, 0, KNOB_SIZE, KNOB_SIZE);
					}
				}

			}
		});

		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(centerPanel, BorderLayout.CENTER);

		metaModelBrowser = new MetaModelBrowser(null) {
			@Override
			public void doubleClickOnComponentResource(Resource selectedComponentResource) {
				System.out.println("doubleClickOnComponentResource " + selectedComponentResource);
				/*if (selectedComponentResource != null) {
					editor.openFIBComponent(selectedComponentResource, null, null, frame);
				}*/
			}
		};

		mainPanel = new JPanel(new BorderLayout());

		toolSelector = toolFactory.makeDianaToolSelector(null);
		stylesWidget = toolFactory.makeDianaStyles();
		scaleSelector = toolFactory.makeDianaScaleSelector(null);
		layoutWidget = toolFactory.makeDianaLayoutWidget();
		inspectors = toolFactory.makeDianaDialogInspectors();

		inspectors.getForegroundStyleInspector().setLocation(1000, 100);
		inspectors.getTextPropertiesInspector().setLocation(1000, 300);
		inspectors.getShadowStyleInspector().setLocation(1000, 400);
		inspectors.getBackgroundStyleInspector().setLocation(1000, 500);
		inspectors.getShapeInspector().setLocation(1000, 600);
		inspectors.getConnectorInspector().setLocation(1000, 700);
		inspectors.getLocationSizeInspector().setLocation(1000, 50);
		inspectors.getControlInspector().setLocation(1000, 150);
		inspectors.getLayoutManagersInspector().setLocation(1000, 300);

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(toolSelector.getComponent());
		topPanel.add(stylesWidget.getComponent());
		topPanel.add(layoutWidget.getComponent());
		topPanel.add(scaleSelector.getComponent());

		mainPanel.add(topPanel, BorderLayout.NORTH);

		mainPanel.add(metaModelBrowser, BorderLayout.WEST);

		commonPaletteModel = new ClassDiagramEditorPalette();
		commonPalette = toolFactory.makeDianaPalette(commonPaletteModel);

		paletteDialog = new JDialog(frame, "Palette", false);
		paletteDialog.getContentPane().add(commonPalette.getComponent());
		paletteDialog.setLocation(1010, 0);
		paletteDialog.pack();
		paletteDialog.setVisible(true);
		paletteDialog.setFocusableWindowState(false);

		centerPanel.add(metaModelBrowser, LayoutPosition.TOP_LEFT.name());
		centerPanel.add(mainPanel, LayoutPosition.CENTER.name());
		centerPanel.add(new JLabel("TOP_RIGHT"), LayoutPosition.TOP_RIGHT.name());
		// centerPanel.add(inspectors.getPanelGroup(), LayoutPosition.BOTTOM_RIGHT.name());

		manager = new PropertyChangeListenerRegistrationManager();

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		menuBar = new PamelaEditorMenuBar(this);
		frame.setJMenuBar(menuBar);

		// frame.getContentPane().add(mainPanel);

		frame.validate();
		frame.pack();

	}

	public JFrame getFrame() {
		return frame;
	}

	public ClassDiagramEditingContext getEditingContext() {
		return editingContext;
	}

	private class MyDrawingViewScrollPane extends JScrollPane {
		private final ClassDiagramEditorView drawingView;

		private MyDrawingViewScrollPane(ClassDiagramEditorView v) {
			super(v);
			drawingView = v;
		}
	}

	public ClassDiagramFactory getFactory() {
		return factory;
	}

	public SwingToolFactory getToolFactory() {
		return toolFactory;
	}

	/*public Injector getInjector() {
		return injector;
	}*/

	public void addDiagramEditor(ClassDiagramEditor classDiagramEditor) {
		if (tabbedPane == null) {
			tabbedPane = new JTabbedPane();
			tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					MyDrawingViewScrollPane c = (MyDrawingViewScrollPane) tabbedPane.getSelectedComponent();
					if (c != null) {
						drawingSwitched(c.drawingView.getDrawing().getModel());
					}
				}
			});
			mainPanel.add(tabbedPane, BorderLayout.CENTER);
			// mainPanel.add(drawing.getEditedDrawing().getPalette().getPaletteView(),BorderLayout.EAST);
		}
		classDiagramEditors.add(classDiagramEditor);

		// DianaEditor controller = new DianaEditor(diagramEditor.getEditedDrawing(), diagramEditor.getFactory());
		// AbstractDianaEditor<DiagramDrawing> controller = new AbstractDianaEditor<DiagramDrawing>(aDrawing, factory)

		tabbedPane.add(classDiagramEditor.getTitle(), new MyDrawingViewScrollPane(classDiagramEditor.getController().getDrawingView()));
		// diagramEditor.getController().getToolbox().getForegroundInspector().setVisible(true);
		switchToDiagramEditor(classDiagramEditor);

		/*frame.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) 
			{
				if (event.getKeyCode() == KeyEvent.VK_UP) {
					drawing.getEditedDrawing().getController().upKeyPressed();
					event.consume();
					return;
				}
				else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
					drawing.getEditedDrawing().getController().downKeyPressed();
					event.consume();
					return;
				}
				else if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
					drawing.getEditedDrawing().getController().rightKeyPressed();
					event.consume();
					return;
				}
				else if (event.getKeyCode() == KeyEvent.VK_LEFT) {
					drawing.getEditedDrawing().getController().leftKeyPressed();
					event.consume();
					return;
				}
				super.keyPressed(event);
			}
		});*/
	}

	// FD : never used
	// private void removeDiagramEditor(DiagramEditor diagramEditor) {

	// }

	public void switchToDiagramEditor(ClassDiagramEditor classDiagramEditor) {
		tabbedPane.setSelectedIndex(classDiagramEditors.indexOf(classDiagramEditor));
	}

	private void drawingSwitched(ClassDiagram classDiagram) {
		for (ClassDiagramEditor editor : classDiagramEditors) {
			if (editor.getDiagram() == classDiagram) {
				drawingSwitched(editor);
				return;
			}
		}
	}

	private void drawingSwitched(ClassDiagramEditor classDiagramEditor) {

		logger.info("Switch to editor " + classDiagramEditor);

		/*if (currentDiagramEditor != null) {
			// mainPanel.remove(currentDiagramEditor.getController().getScalePanel());
			currentDiagramEditor.getController().deleteObserver(inspector);
		}*/
		currentClassDiagramEditor = classDiagramEditor;
		toolSelector.attachToEditor(classDiagramEditor.getController());
		stylesWidget.attachToEditor(classDiagramEditor.getController());
		scaleSelector.attachToEditor(classDiagramEditor.getController());
		layoutWidget.attachToEditor(classDiagramEditor.getController());
		commonPaletteModel.setEditor(classDiagramEditor.getController());
		commonPalette.attachToEditor(classDiagramEditor.getController());
		inspectors.attachToEditor(classDiagramEditor.getController());

		/*JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(currentDiagramEditor.getController().getToolbox().getStyleToolBar());
		topPanel.add(currentDiagramEditor.getController().getScalePanel());
		
		mainPanel.add(topPanel, BorderLayout.NORTH);*/

		menuBar.copyItem.synchronizeWith(classDiagramEditor.getController());
		menuBar.cutItem.synchronizeWith(classDiagramEditor.getController());
		menuBar.pasteItem.synchronizeWith(classDiagramEditor.getController());

		menuBar.undoItem.synchronizeWith(classDiagramEditor.getController().getFactory().getUndoManager());
		menuBar.redoItem.synchronizeWith(classDiagramEditor.getController().getFactory().getUndoManager());

		// currentDiagramEditor.getController().addObserver(inspector);
		updateFrameTitle();
		mainPanel.revalidate();
		mainPanel.repaint();
	}

	private void updateFrameTitle() {
		frame.setTitle("Pamela editor - " + currentClassDiagramEditor.getTitle());
	}

	private void updateTabTitle() {
		tabbedPane.setTitleAt(classDiagramEditors.indexOf(currentClassDiagramEditor), currentClassDiagramEditor.getTitle());
	}

	public void showMainPanel() {

		frame.setVisible(true);

	}

	public JPanel getMainPanel() {
		return mainPanel;
	}

	public void quit() {
		frame.dispose();
		System.exit(0);
	}

	public void closeDrawing() {
		if (currentClassDiagramEditor == null) {
			return;
		}
		if (currentClassDiagramEditor.getDiagram().hasChanged()) {
			int result = JOptionPane.showOptionDialog(frame, "Would you like to save drawing changes?", "Save changes",
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, JOptionPane.YES_OPTION);
			switch (result) {
				case JOptionPane.YES_OPTION:
					if (!currentClassDiagramEditor.save()) {
						return;
					}
					break;
				case JOptionPane.NO_OPTION:
					break;
				default:
					return;
			}
		}
		classDiagramEditors.remove(currentClassDiagramEditor);
		tabbedPane.remove(tabbedPane.getSelectedIndex());
		if (classDiagramEditors.size() == 0) {
			newDiagramEditor();
		}
	}

	public void newMetaModel() {
		SourceMetaModel newMetaModel = new SourceMetaModel();
		newMetaModel.setName("tutu");
		metaModelBrowser.setEditedObject(newMetaModel);
	}

	public void newDiagramEditor() {
		ClassDiagramEditor newDiagramEditor = ClassDiagramEditor.newDiagramEditor(factory, this);
		addDiagramEditor(newDiagramEditor);
	}

	public void loadDiagramEditor() {
		if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			loadDiagramEditor(file);
		}
	}

	public void loadDiagramEditor(File file) {
		ClassDiagramEditor loadedDiagramEditor = ClassDiagramEditor.loadDiagramEditor(file, factory, this);
		if (loadedDiagramEditor != null) {
			addDiagramEditor(loadedDiagramEditor);
		}
		PamelaEditorPreferences.setLastFile(file);
	}

	public boolean saveDiagramEditor() {
		if (currentClassDiagramEditor == null) {
			return false;
		}
		if (currentClassDiagramEditor.getFile() == null) {
			return saveDrawingAs();
		}
		return currentClassDiagramEditor.save();
	}

	public boolean saveDrawingAs() {
		if (currentClassDiagramEditor == null) {
			return false;
		}
		if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (!file.getName().endsWith(".drw")) {
				file = new File(file.getParentFile(), file.getName() + ".drw");
			}
			currentClassDiagramEditor.setFile(file);
			PamelaEditorPreferences.setLastFile(file);
			updateFrameTitle();
			updateTabTitle();
			return currentClassDiagramEditor.save();
		}
		return false;
	}

	public void showLogs() {
		FlexoLoggingViewer.showLoggingViewer(FlexoLoggingManager.instance(), ApplicationFIBLibraryImpl.instance(), frame);
	}

	public void showLocalizedEditor() {
		if (localizedEditor == null) {
			localizedEditor = new LocalizedEditor(getFrame(), "localized_editor", PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION,
					PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION, true, false);
		}
		localizedEditor.setVisible(true);
	}

	public void copy() {
		System.out.println("copy");
		try {
			currentClassDiagramEditor.getController().copy();
		} catch (CopyException e1) {
			e1.printStackTrace();
		}
	}

	public void cut() {
		System.out.println("cut");
		try {
			currentClassDiagramEditor.getController().cut();
		} catch (CutException e1) {
			e1.printStackTrace();
		}
	}

	public void paste() {
		System.out.println("paste");
		if (currentClassDiagramEditor != null) {
			try {
				currentClassDiagramEditor.getController().paste();
			} catch (PasteException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void undo() {

	}

	public void redo() {

	}

	public void setExitOnClose() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static enum LayoutPosition {
		TOP_LEFT, BOTTOM_LEFT, CENTER, TOP_RIGHT, BOTTOM_RIGHT;
	}

	public static enum LayoutColumns {
		LEFT, CENTER, RIGHT;
	}

	protected static final int KNOB_SIZE = 5;
	protected static final int KNOB_SPACE = 2;
	protected static final int DIVIDER_SIZE = KNOB_SIZE + 2 * KNOB_SPACE;
	protected static final int DIVIDER_KNOB_SIZE = 3 * KNOB_SIZE + 2 * KNOB_SPACE;

	protected static final Paint KNOB_PAINTER = new RadialGradientPaint(new Point((KNOB_SIZE - 1) / 2, (KNOB_SIZE - 1) / 2),
			(KNOB_SIZE - 1) / 2, new float[] { 0.0f, 1.0f }, new Color[] { Color.GRAY, Color.LIGHT_GRAY });

	protected static Split<?> getDefaultLayout() {
		Split root = MSL_FACTORY.makeSplit();
		root.setName("ROOT");
		Split<?> left = getVerticalSplit(LayoutPosition.TOP_LEFT, 0.5, LayoutPosition.BOTTOM_LEFT, 0.5);
		left.setWeight(0.2);
		left.setName(LayoutColumns.LEFT.name());
		Node<?> center = MSL_FACTORY.makeLeaf(LayoutPosition.CENTER.name());
		center.setWeight(0.55);
		center.setName(LayoutColumns.CENTER.name());
		Split<?> right = getVerticalSplit(LayoutPosition.TOP_RIGHT, 0.4, LayoutPosition.BOTTOM_RIGHT, 0.6);
		right.setWeight(0.25);
		right.setName(LayoutColumns.RIGHT.name());
		root.setChildren(left, MSL_FACTORY.makeDivider(), center, MSL_FACTORY.makeDivider(), right);
		return root;
	}

	protected static Split<?> getVerticalSplit(LayoutPosition position1, double weight1, LayoutPosition position2, double weight2) {
		Split split = MSL_FACTORY.makeSplit();
		split.setRowLayout(false);
		Leaf<?> l1 = MSL_FACTORY.makeLeaf(position1.name());
		l1.setWeight(weight1);
		Leaf<?> l2 = MSL_FACTORY.makeLeaf(position2.name());
		l2.setWeight(weight2);
		split.setChildren(l1, MSL_FACTORY.makeDivider(), l2);
		return split;
	}

	protected static Split<?> getVerticalSplit(LayoutPosition position1, LayoutPosition position2, LayoutPosition position3) {
		Split split = MSL_FACTORY.makeSplit();
		split.setRowLayout(false);
		Leaf<?> l1 = MSL_FACTORY.makeLeaf(position1.name());
		l1.setWeight(0.2);
		Leaf<?> l2 = MSL_FACTORY.makeLeaf(position2.name());
		l2.setWeight(0.6);
		Leaf<?> l3 = MSL_FACTORY.makeLeaf(position3.name());
		l3.setWeight(0.2);
		split.setChildren(l1, MSL_FACTORY.makeDivider(), l2, MSL_FACTORY.makeDivider(), l3);
		return split;
	}

}
