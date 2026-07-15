/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.controller.FIBController;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.gina.model.FIBContainer;
import org.openflexo.gina.model.widget.FIBCustom;
import org.openflexo.gina.model.widget.FIBCustom.FIBCustomComponent;
import org.openflexo.gina.swing.view.JFIBView;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.swing.TextFieldCustomPopup;
import org.openflexo.toolbox.HasPropertyChangeSupport;

/**
 * Generic base for the pamela-editor "selector" widgets, built on the same principle as
 * {@link org.openflexo.gina.swing.utils.TypeSelector}: a {@link javax.swing.JTextField} with a
 * leading triangle that opens a popup. The popup contains
 * <ol>
 * <li>a <b>search/filter</b> field (the widget's own text field) that lists matching values, and</li>
 * <li>a <b>browser</b> to pick a value from an enclosing context, subject to constraints.</li>
 * </ol>
 *
 * <p>This is a port of Flexo's {@code FIBFlexoObjectSelector} stripped of all Flexo-specific
 * dependencies ({@code FlexoObject}, {@code FlexoServiceManager}, {@code FlexoController},
 * {@code FlexoFIBController}); it is built only on workspace primitives
 * ({@link TextFieldCustomPopup}, {@link FIBCustomComponent}, {@link PamelaEditorFIBController}).
 * See {@code selector-widgets-design.md}.</p>
 *
 * <p>Subclasses provide: the popup FIB ({@link #getFIBResource()}), the value type
 * ({@link #getRepresentedType()}), the textual rendering ({@link #renderedString(Object)}), the
 * browser root ({@link #getRootObject()}) and the set of selectable values for completion
 * ({@link #getAllSelectableValues()}). Constraints are expressed by overriding
 * {@link #isAcceptableValue(Object)}.</p>
 *
 * @param <T> the type of value selected by this widget
 * @author sylvain
 */
@SuppressWarnings("serial")
public abstract class FIBPamelaObjectSelector<T> extends TextFieldCustomPopup<T>
		implements FIBCustomComponent<T>, HasPropertyChangeSupport {

	static final Logger LOGGER = Logger.getLogger(FIBPamelaObjectSelector.class.getPackage().getName());

	private static final String DELETED = "deleted";

	private T _revertValue;
	protected SelectorDetailsPanel _selectorPanel;

	/** Leading icon (left of the text field) reflecting the currently selected value. */
	private final JLabel iconLabel = new JLabel();

	private Object selectedObject;
	private T selectedValue;
	private final List<T> matchingValues;
	private Collection<T> restrictedValues;
	private boolean isFiltered = false;
	private boolean showReset = true;

	private FIBCustom component;
	private FIBController fibController;

	private PropertyChangeSupport pcSupport;

	public FIBPamelaObjectSelector(T editedObject) {
		super(editedObject);
		pcSupport = new PropertyChangeSupport(this);
		setRevertValue(editedObject);
		setFocusable(true);
		matchingValues = new ArrayList<>();
		// Show the icon of the selected value at the leading edge of the text field.
		iconLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
		getFrontComponent().add(iconLabel, BorderLayout.WEST);
		updateSelectedIcon();
		getTextField().setEditable(true);
		getTextField().getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				if (!textIsBeeingProgrammaticallyEditing()) {
					updateMatchingValues();
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (!textIsBeeingProgrammaticallyEditing()) {
					updateMatchingValues();
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				if (!textIsBeeingProgrammaticallyEditing()) {
					updateMatchingValues();
				}
			}
		});
		getTextField().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					SwingUtilities.invokeLater(() -> {
						updateMatchingValues();
						if (matchingValues.size() > 0) {
							setSelectedValue(matchingValues.get(0));
							apply();
						}
					});
				}
			}

			@Override
			public void keyTyped(KeyEvent e) {
				// if a command-key is pressed, do not open the popup
				if (e.isAltDown() || e.isAltGraphDown() || e.isControlDown() || e.isMetaDown()) {
					return;
				}
				boolean requestFocus = getTextField().hasFocus();
				final int selectionStart = getTextField().getSelectionStart() + 1;
				final int selectionEnd = getTextField().getSelectionEnd() + 1;
				if (!popupIsShown()) {
					openPopup();
				}
				if (requestFocus) {
					SwingUtilities.invokeLater(() -> {
						getTextField().requestFocusInWindow();
						getTextField().select(selectionStart, selectionEnd);
					});
				}
			}
		});
	}

	// -------------------------------------------------------------------------
	// Leading icon reflecting the selected value
	// -------------------------------------------------------------------------

	@Override
	public void fireEditedObjectChanged() {
		super.fireEditedObjectChanged();
		updateSelectedIcon();
	}

	/**
	 * The icon to display for the currently selected value. Default delegates to the shared
	 * {@link SelectorIcons} resolution (the same icons the popup browser uses). Override to customize.
	 */
	protected ImageIcon iconForValue(T value) {
		return SelectorIcons.iconFor(value);
	}

	private void updateSelectedIcon() {
		T value = getEditedObject();
		ImageIcon icon = value != null ? iconForValue(value) : null;
		iconLabel.setIcon(icon);
		iconLabel.setVisible(icon != null);
	}

	// -------------------------------------------------------------------------
	// Abstract contract for subclasses
	// -------------------------------------------------------------------------

	/** The FIB resource describing the popup (browser + filter list). */
	public abstract Resource getFIBResource();

	/** The root object the popup browser is bound to ({@code data.rootObject}). */
	public abstract Object getRootObject();

	/**
	 * All values that completion (the filter list) may select, computed directly from the model
	 * (never by recursively exploring the browser), to avoid the costly per-cell {@code equals()}
	 * traversal of large Spoon trees. May return {@code null} when no context is set.
	 *
	 * <p>Overridden by every concrete selector. Callers must use {@link #getAllSelectableValues()},
	 * which applies the optional {@link #setRestrictedValues(Collection) candidate restriction}.</p>
	 */
	protected abstract Collection<T> computeAllSelectableValues();

	/**
	 * The effective selectable set: the explicit {@link #setRestrictedValues(Collection) restricted
	 * candidates} when set, else {@link #computeAllSelectableValues()}.
	 */
	protected final Collection<T> getAllSelectableValues() {
		return restrictedValues != null ? restrictedValues : computeAllSelectableValues();
	}

	/**
	 * Optional restriction: when non-null, completion and acceptability are limited to exactly these
	 * candidates (membership tested by identity). Lets a dialog that already computes a constrained
	 * list (type-matched, lacking-accessor, cycle-free…) reuse it while gaining search + browse.
	 * Settable from a FIB via {@code <Assignment variable="component.restrictedValues" value="data.candidates"/>}.
	 */
	public void setRestrictedValues(Collection<T> restrictedValues) {
		this.restrictedValues = restrictedValues;
		refresh();
	}

	public Collection<T> getRestrictedValues() {
		return restrictedValues;
	}

	protected boolean isAmongRestricted(Object o) {
		if (restrictedValues == null) {
			return true;
		}
		for (T candidate : restrictedValues) {
			if (candidate == o) {
				return true;
			}
		}
		return false;
	}

	// -------------------------------------------------------------------------
	// FIBCustomComponent
	// -------------------------------------------------------------------------

	@Override
	public void init(FIBCustom component, FIBController controller) {
		this.component = component;
		this.fibController = controller;
	}

	public FIBCustom getComponent() {
		return component;
	}

	public FIBController getFIBController() {
		return fibController;
	}

	@Override
	public void setRevertValue(T oldValue) {
		_revertValue = oldValue;
	}

	@Override
	public T getRevertValue() {
		return _revertValue;
	}

	@Override
	public void delete() {
		super.delete();
		if (pcSupport != null) {
			pcSupport.firePropertyChange(DELETED, false, true);
		}
		matchingValues.clear();
		pcSupport = null;
		selectedObject = null;
		selectedValue = null;
	}

	@Override
	public PropertyChangeSupport getPropertyChangeSupport() {
		return pcSupport;
	}

	@Override
	public String getDeletedProperty() {
		return DELETED;
	}

	@Override
	protected boolean useEqualsLookup() {
		// Selectors choose a value; identity comparison avoids surprises with model equals()
		return false;
	}

	// -------------------------------------------------------------------------
	// Selection
	// -------------------------------------------------------------------------

	public Object getSelectedObject() {
		return selectedObject;
	}

	@SuppressWarnings("unchecked")
	public void setSelectedObject(Object selectedObject) {
		Object old = this.selectedObject;
		if (old != selectedObject) {
			this.selectedObject = selectedObject;
			pcSupport.firePropertyChange("selectedObject", old, selectedObject);
			if (isAcceptableValue(selectedObject)) {
				setSelectedValue((T) selectedObject);
			} else {
				setSelectedValue(null);
			}
		}
	}

	public T getSelectedValue() {
		return selectedValue;
	}

	public void setSelectedValue(T selectedValue) {
		T old = this.selectedValue;
		if (old != selectedValue) {
			this.selectedValue = selectedValue;
			pcSupport.firePropertyChange("selectedValue", old, selectedValue);
			if (getSelectedObject() != getSelectedValue()) {
				setSelectedObject(selectedValue);
			}
		}
	}

	// -------------------------------------------------------------------------
	// Filtering / completion
	// -------------------------------------------------------------------------

	public boolean isFiltered() {
		return isNotEmpty(getFilteredName()) && isFiltered;
	}

	public String getFilteredName() {
		return getTextField().getText();
	}

	public void setFilteredName(String aString) {
		getTextField().setText(aString);
	}

	public List<T> getMatchingValues() {
		return matchingValues;
	}

	/**
	 * Localized "Found N matches" label shown above the browser when the type-ahead filter
	 * narrows {@link #getMatchingValues()} to something other than exactly one candidate.
	 * Composed here (rather than baked as English text into the FIB binding expression) so it
	 * is translated in all supported languages.
	 */
	public String getMatchesLabel() {
		return org.openflexo.pamela.editor.ui.PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION
				.localizedForKey("found") + " " + getMatchingValues().size() + " "
				+ org.openflexo.pamela.editor.ui.PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION
						.localizedForKey("matches");
	}

	private void updateMatchingValues() {
		final List<T> oldMatchingValues = new ArrayList<>(getMatchingValues());
		matchingValues.clear();
		Collection<T> allValues = getAllSelectableValues();
		if (allValues != null && getFilteredName() != null) {
			isFiltered = true;
			for (T next : allValues) {
				if (isAcceptableValue(next) && matches(next, getFilteredName())) {
					matchingValues.add(next);
				}
			}
		}
		SwingUtilities.invokeLater(() -> {
			if (pcSupport != null) {
				pcSupport.firePropertyChange("matchingValues", oldMatchingValues, getMatchingValues());
			}
			if (matchingValues.size() == 1) {
				setSelectedValue(matchingValues.get(0));
			}
		});
	}

	private void clearMatchingValues() {
		isFiltered = false;
		List<T> oldMatchingValues = new ArrayList<>(getMatchingValues());
		matchingValues.clear();
		if (pcSupport != null) {
			pcSupport.firePropertyChange("matchingValues", oldMatchingValues, null);
		}
	}

	/** Default: case-insensitive substring match on {@link #renderedString(Object)}. */
	protected boolean matches(T o, String filteredName) {
		return o != null && isNotEmpty(renderedString(o))
				&& renderedString(o).toUpperCase().contains(filteredName.toUpperCase());
	}

	/**
	 * Whether a candidate is an acceptable value for this selector. Default: a non-null instance of
	 * {@link #getRepresentedType()}. Override to express constraints.
	 */
	public boolean isAcceptableValue(Object o) {
		return o != null && getRepresentedType().isAssignableFrom(o.getClass()) && isAmongRestricted(o);
	}

	// -------------------------------------------------------------------------
	// Popup panel lifecycle
	// -------------------------------------------------------------------------

	@Override
	public void openPopup() {
		super.openPopup();
		getTextField().requestFocusInWindow();
	}

	@Override
	protected SelectorDetailsPanel createCustomPanel(T editedObject) {
		_selectorPanel = makeCustomPanel(editedObject);
		return _selectorPanel;
	}

	protected SelectorDetailsPanel makeCustomPanel(T editedObject) {
		return new SelectorDetailsPanel(editedObject);
	}

	@Override
	public void updateCustomPanel(T editedObject) {
		setSelectedObject(editedObject);
		if (_selectorPanel != null) {
			_selectorPanel.update();
		}
	}

	@Override
	public SelectorDetailsPanel getCustomPanel() {
		return (SelectorDetailsPanel) super.getCustomPanel();
	}

	public SelectorDetailsPanel getSelectorPanel() {
		return _selectorPanel;
	}

	protected SelectorFIBController makeCustomFIBController(FIBComponent fibComponent) {
		return new SelectorFIBController(fibComponent, this);
	}

	/**
	 * Re-fires {@code rootObject} and refreshes the popup; call from a context/constraint setter so
	 * the browser tree and completion are recomputed.
	 */
	protected void refresh() {
		if (pcSupport != null) {
			pcSupport.firePropertyChange("rootObject", null, getRootObject());
		}
		updateCustomPanel(getEditedObject());
	}

	@Override
	public void apply() {
		clearMatchingValues();
		setEditedObject(getSelectedValue());
		setRevertValue(getEditedObject());
		closePopup();
		super.apply();
	}

	@Override
	public void cancel() {
		setEditedObject(getRevertValue());
		closePopup();
		super.cancel();
	}

	@Override
	protected void deletePopup() {
		if (_selectorPanel != null) {
			_selectorPanel.delete();
		}
		_selectorPanel = null;
		super.deletePopup();
	}

	public boolean isShowReset() {
		return showReset;
	}

	public void setShowReset(boolean showReset) {
		this.showReset = showReset;
	}

	private static boolean isNotEmpty(String s) {
		return s != null && s.length() > 0;
	}

	// =========================================================================
	// The popup content: a FIB (browser + filter list) bound to this selector
	// =========================================================================

	public class SelectorDetailsPanel extends ResizablePanel {
		private final FIBContainer fibComponent;
		private JFIBView<?, ?> fibView;
		private SelectorFIBController controller;

		protected SelectorDetailsPanel(T anObject) {
			super();
			fibComponent = (FIBContainer) ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(getFIBResource());
			controller = makeCustomFIBController(fibComponent);
			fibView = (JFIBView<?, ?>) controller.buildView(fibComponent, null, true);
			controller.setDataObject(FIBPamelaObjectSelector.this, true);
			setLayout(new BorderLayout());
			add(fibView.getResultingJComponent(), BorderLayout.CENTER);
		}

		public void update() {
			if (controller != null) {
				controller.setDataObject(FIBPamelaObjectSelector.this);
			}
		}

		@Override
		public Dimension getDefaultSize() {
			return new Dimension(fibComponent.getWidth(), fibComponent.getHeight());
		}

		public void delete() {
			if (controller != null) {
				controller.delete();
			}
			if (fibView != null) {
				fibView.delete();
			}
			controller = null;
			fibView = null;
		}

		public SelectorFIBController getController() {
			return controller;
		}

		public JFIBView<?, ?> getFIBView() {
			return fibView;
		}

		public FIBContainer getFIBComponent() {
			return fibComponent;
		}
	}

	// =========================================================================
	// The FIB controller for the popup. Bound as controllerClassName in every
	// selector FIB; instantiated programmatically with a back-reference to the
	// selector so the FIB actions can drive selection/apply/cancel/reset.
	// =========================================================================

	public static class SelectorFIBController extends PamelaEditorFIBController<Object> {

		private FIBPamelaObjectSelector<?> selector;

		public SelectorFIBController(FIBComponent component) {
			super(component);
		}

		public SelectorFIBController(FIBComponent component, FIBPamelaObjectSelector<?> selector) {
			super(component);
			this.selector = selector;
		}

		public FIBPamelaObjectSelector<?> getSelector() {
			return selector;
		}

		/** Called from the browser/list clickAction: a new value was picked. */
		public void selectedObjectChanged() {
			if (selector != null) {
				selector.setEditedObject(uncheckedSelectedValue());
			}
		}

		public void apply() {
			if (selector != null) {
				selector.apply();
			}
		}

		public void cancel() {
			if (selector != null) {
				selector.cancel();
			}
		}

		public void reset() {
			if (selector != null) {
				selector.setSelectedObject(null);
				selector.setSelectedValue(null);
				selector.setEditedObject(null);
				selector.apply();
			}
		}

		@SuppressWarnings("unchecked")
		private <X> X uncheckedSelectedValue() {
			return (X) selector.getSelectedValue();
		}

		/** Label helper for FIB browsers that show Spoon nodes (CtType / CtMethod / …). */
		public String labelForObject(Object object) {
			String spoon = SpoonOutlineController.labelFor(object);
			return spoon != null ? spoon : (object != null ? object.toString() : "");
		}

		/** Children helper for the Java-method browser ({@code CtType} → members). */
		public java.util.List<spoon.reflect.declaration.CtTypeMember> membersOf(
				spoon.reflect.declaration.CtType<?> type) {
			return SpoonOutlineController.memberList(type);
		}

		@Override
		protected javax.swing.ImageIcon retrieveIconForObject(Object object) {
			javax.swing.ImageIcon icon = SelectorIcons.iconFor(object);
			return icon != null ? icon : super.retrieveIconForObject(object);
		}
	}
}
