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
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

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

import org.openflexo.diana.control.DianaInteractiveViewer;
import org.openflexo.pamela.editor.ui.diagram.DianaDrawingEditor;
import org.openflexo.pamela.undo.UndoManager;
import org.openflexo.toolbox.HasPropertyChangeSupport;

public class PamelaEditorMenuBar extends JMenuBar implements PreferenceChangeListener {

	private final JMenu fileMenu;
	private final JMenu editMenu;
	private final JMenu viewMenu;
	private final JMenu toolsMenu;
	private final JMenu helpMenu;

	private final JMenuItem newItem;
	private final JMenuItem loadItem;
	private final JMenuItem saveItem;
	private final JMenuItem saveAsItem;
	private final JMenuItem closeItem;
	private final JMenuItem quitItem;

	final SynchronizedMenuItem copyItem;
	final SynchronizedMenuItem cutItem;
	final SynchronizedMenuItem pasteItem;
	final SynchronizedMenuItem undoItem;
	final SynchronizedMenuItem redoItem;

	private final JMenuItem logsItem;
	private final JMenuItem localizedItem;

	private final JMenu openRecent;

	private final JMenuItem showPaletteItem;

	private final PamelaEditorApplication application;

	public PamelaEditorMenuBar(PamelaEditorApplication application) {

		this.application = application;

		fileMenu = new JMenu(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("file"));
		editMenu = new JMenu(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("edit"));
		viewMenu = new JMenu(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("view"));
		toolsMenu = new JMenu(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("tools"));
		helpMenu = new JMenu(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("help"));

		newItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("new_metamodel"));
		newItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.newMetaModel();
			}
		});

		loadItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("open_metamodel"));
		loadItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// application.loadDiagramEditor();
			}
		});

		/*newItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("new_diagram"));
		newItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.newDiagramEditor();
			}
		});
		
		loadItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("open_diagram"));
		loadItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.loadDiagramEditor();
			}
		});*/

		openRecent = new JMenu(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("open_recent"));
		PamelaEditorPreferences.addPreferenceChangeListener(this);
		updateOpenRecent();
		saveItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("save_diagram"));
		saveItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.saveDiagramEditor();
			}
		});

		saveAsItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("save_diagram_as"));
		saveAsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.saveDrawingAs();
			}
		});

		closeItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("close"));
		closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.closeDrawing();
			}
		});

		quitItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("quit"));
		quitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.quit();
			}
		});

		fileMenu.add(newItem);
		fileMenu.add(loadItem);
		fileMenu.add(openRecent);
		fileMenu.add(saveItem);
		fileMenu.add(saveAsItem);
		fileMenu.add(closeItem);
		fileMenu.addSeparator();

		fileMenu.add(quitItem);

		showPaletteItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("show_palette"));
		showPaletteItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				application.paletteDialog.setVisible(true);
			}
		});
		logsItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("logs"));
		logsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.showLogs();
			}
		});

		localizedItem = new JMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("localized_editor"));
		localizedItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				application.showLocalizedEditor();
			}
		});

		copyItem = makeSynchronizedMenuItem("copy", PamelaEditorIconLibrary.COPY_ICON,
				KeyStroke.getKeyStroke(KeyEvent.VK_C, PamelaEditorApplication.META_MASK), new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						application.copy();
					}
				}, new Synchronizer() {
					@Override
					public void synchronize(HasPropertyChangeSupport observable, SynchronizedMenuItem menuItem) {
						if (observable instanceof DianaDrawingEditor) {
							menuItem.setEnabled(((DianaDrawingEditor) observable).isCopiable());
						}
					}
				});

		cutItem = makeSynchronizedMenuItem("cut", PamelaEditorIconLibrary.CUT_ICON,
				KeyStroke.getKeyStroke(KeyEvent.VK_X, PamelaEditorApplication.META_MASK), new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						application.cut();
					}
				}, new Synchronizer() {
					@Override
					public void synchronize(HasPropertyChangeSupport observable, SynchronizedMenuItem menuItem) {
						if (observable instanceof DianaDrawingEditor) {
							menuItem.setEnabled(((DianaDrawingEditor) observable).isCutable());
						}
					}
				});

		pasteItem = makeSynchronizedMenuItem("paste", PamelaEditorIconLibrary.PASTE_ICON,
				KeyStroke.getKeyStroke(KeyEvent.VK_V, PamelaEditorApplication.META_MASK), new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						application.paste();
					}
				}, new Synchronizer() {
					@Override
					public void synchronize(HasPropertyChangeSupport observable, SynchronizedMenuItem menuItem) {
						if (observable instanceof DianaInteractiveViewer) {
							menuItem.setEnabled(application.currentClassDiagramEditor.getController().isPastable());
						}
					}
				});

		undoItem = makeSynchronizedMenuItem("undo", PamelaEditorIconLibrary.UNDO_ICON,
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, PamelaEditorApplication.META_MASK), new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						System.out.println("undo");
						application.currentClassDiagramEditor.getController().undo();
					}
				}, new Synchronizer() {
					@Override
					public void synchronize(HasPropertyChangeSupport observable, SynchronizedMenuItem menuItem) {
						if (observable instanceof UndoManager) {
							menuItem.setEnabled(application.currentClassDiagramEditor.getController().canUndo());
							if (application.currentClassDiagramEditor.getController().canUndo()) {
								menuItem.setText(application.currentClassDiagramEditor.getController().getFactory().getUndoManager()
										.getUndoPresentationName());
							}
							else {
								menuItem.setText(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("undo"));
							}
						}
					}
				});

		redoItem = makeSynchronizedMenuItem("redo", PamelaEditorIconLibrary.REDO_ICON,
				KeyStroke.getKeyStroke(KeyEvent.VK_R, PamelaEditorApplication.META_MASK), new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						System.out.println("redo");
						application.currentClassDiagramEditor.getController().redo();
					}
				}, new Synchronizer() {
					@Override
					public void synchronize(HasPropertyChangeSupport observable, SynchronizedMenuItem menuItem) {
						if (observable instanceof UndoManager) {
							menuItem.setEnabled(application.currentClassDiagramEditor.getController().canRedo());
							if (application.currentClassDiagramEditor.getController().canRedo()) {
								menuItem.setText(application.currentClassDiagramEditor.getController().getFactory().getUndoManager()
										.getRedoPresentationName());
							}
							else {
								menuItem.setText(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("redo"));
							}
						}
					}
				});

		editMenu.add(copyItem);
		editMenu.add(cutItem);
		editMenu.add(pasteItem);
		editMenu.addSeparator();
		editMenu.add(undoItem);
		editMenu.add(redoItem);

		/*WindowMenuItem foregroundInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("foreground_inspector"),
				pamelaEditorApplication.inspectors.getForegroundStyleInspector());
		WindowMenuItem backgroundInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("background_inspector"),
				pamelaEditorApplication.inspectors.getBackgroundStyleInspector());
		WindowMenuItem textInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("text_inspector"),
				pamelaEditorApplication.inspectors.getTextPropertiesInspector());
		WindowMenuItem shapeInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("shape_inspector"),
				pamelaEditorApplication.inspectors.getShapeInspector());
		WindowMenuItem connectorInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("connector_inspector"),
				pamelaEditorApplication.inspectors.getConnectorInspector());
		WindowMenuItem shadowInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("shadow_inspector"),
				pamelaEditorApplication.inspectors.getShadowStyleInspector());
		WindowMenuItem locationSizeInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("location_size_inspector"),
				pamelaEditorApplication.inspectors.getLocationSizeInspector());
		WindowMenuItem controlInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("control_inspector"),
				pamelaEditorApplication.inspectors.getControlInspector());
		WindowMenuItem layoutManagerInspectorItem = new WindowMenuItem(
				PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("layout_manager_inspector"),
				pamelaEditorApplication.inspectors.getLayoutManagersInspector());
		
		WindowMenuItem paletteItem = new WindowMenuItem(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey("palette"),
				pamelaEditorApplication.paletteDialog);
		
		viewMenu.add(foregroundInspectorItem);
		viewMenu.add(backgroundInspectorItem);
		viewMenu.add(textInspectorItem);
		viewMenu.add(shapeInspectorItem);
		viewMenu.add(connectorInspectorItem);
		viewMenu.add(shadowInspectorItem);
		viewMenu.add(locationSizeInspectorItem);
		viewMenu.add(controlInspectorItem);
		viewMenu.add(layoutManagerInspectorItem);
		viewMenu.addSeparator();
		viewMenu.add(paletteItem);*/

		toolsMenu.add(showPaletteItem);
		toolsMenu.add(logsItem);
		toolsMenu.add(localizedItem);

		add(fileMenu);
		add(editMenu);
		add(viewMenu);
		add(toolsMenu);
		add(helpMenu);
	}

	private boolean willUpdate = false;

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().startsWith(PamelaEditorPreferences.LAST_FILE)) {
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
		openRecent.setEnabled(files.size() != 0);
		for (final File file : files) {
			JMenuItem item = new JMenuItem(file.getName());
			item.setToolTipText(file.getAbsolutePath());
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					application.loadDiagramEditor(file);
				}
			});
			openRecent.add(item);
		}
	}

	SynchronizedMenuItem makeSynchronizedMenuItem(String actionName, Icon icon, KeyStroke accelerator, AbstractAction action,
			Synchronizer synchronizer) {

		String localizedName = PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey(actionName);
		SynchronizedMenuItem returned = new SynchronizedMenuItem(localizedName, synchronizer);
		action.putValue(Action.NAME, localizedName);
		returned.setAction(action);
		returned.setIcon(icon);
		returned.setAccelerator(accelerator);
		application.frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accelerator, actionName);
		application.frame.getRootPane().getActionMap().put(actionName, action);
		returned.setEnabled(false);
		return returned;
	}

	public interface Synchronizer {
		public void synchronize(HasPropertyChangeSupport observable, SynchronizedMenuItem menuItem);
	}

	public class SynchronizedMenuItem extends JMenuItem implements PropertyChangeListener {

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
			getAction().setEnabled(b);
		}

	}

	public class WindowMenuItem extends JCheckBoxMenuItem implements WindowListener {

		private final Window window;

		public WindowMenuItem(String menuName, Window aWindow) {
			super(menuName);
			this.window = aWindow;
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					window.setVisible(!window.isVisible());
				}
			});
			aWindow.addWindowListener(this);
		}

		@Override
		public void windowOpened(WindowEvent e) {
			setState(window.isVisible());
		}

		@Override
		public void windowIconified(WindowEvent e) {
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
			setState(window.isVisible());
		}

		@Override
		public void windowClosing(WindowEvent e) {
			setState(window.isVisible());
		}

		@Override
		public void windowClosed(WindowEvent e) {
			setState(window.isVisible());
		}

		@Override
		public void windowActivated(WindowEvent e) {
			setState(window.isVisible());
		}

	}

}
