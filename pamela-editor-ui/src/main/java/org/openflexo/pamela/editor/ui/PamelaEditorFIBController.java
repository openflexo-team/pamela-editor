/**
 * 
 * Copyright (c) 2014, Openflexo
 * 
 * This file is part of Gina-swing, a component of the software infrastructure 
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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.openflexo.connie.annotations.NotificationUnsafe;
import org.openflexo.gina.controller.FIBController;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.gina.model.FIBModelObject;
import org.openflexo.gina.model.FIBValidationReport;
import org.openflexo.gina.swing.utils.FIBUtilsIconLibrary;
import org.openflexo.gina.swing.view.SwingViewFactory;
import org.openflexo.icon.IconFactory;
import org.openflexo.localization.LocalizedDelegate;
import org.openflexo.localization.LocalizedDelegateImpl;
import org.openflexo.pamela.editor.model.SourceElement;
import org.openflexo.pamela.validation.FixProposal;
import org.openflexo.pamela.validation.InformationIssue;
import org.openflexo.pamela.validation.ValidationError;
import org.openflexo.pamela.validation.ValidationModel;
import org.openflexo.pamela.validation.ValidationReport;
import org.openflexo.pamela.validation.ValidationWarning;
import org.openflexo.rm.ResourceLocator;

/**
 * A {@link FIBController} used in GINA SwingEditor, and addressing a data object of type T
 * 
 * @author sylvain
 *
 * @param <T>
 *            type of data object beeing managed by this controller
 */
public class PamelaEditorFIBController<T> extends FIBController implements PropertyChangeListener {

	static final Logger LOGGER = Logger.getLogger(PamelaEditorFIBController.class.getPackage().getName());

	public static LocalizedDelegate EDITOR_LOCALIZATION = new LocalizedDelegateImpl(
			ResourceLocator.locateResource("PamelaLocalization/PamelaEditor"), null, true, true);

	private final Map<Object, ImageIcon> cachedIcons = new HashMap<>();
	private final List<ValidationReport> observedReports = new ArrayList<>();
	private FIBModelObject selectedObject;

	public PamelaEditorFIBController(FIBComponent rootComponent) {
		super(rootComponent, SwingViewFactory.INSTANCE);
		// Default parent localizer is the main localizer
		setParentLocalizer(EDITOR_LOCALIZATION);
	}

	protected ImageIcon retrieveIconForObject(Object object) {

		if (object instanceof FIBModelObject) {
			FIBValidationReport report = getValidationReport((FIBModelObject) object);
			if (report != null) {
				if (!observedReports.contains(report)) {
					report.getPropertyChangeSupport().addPropertyChangeListener(this);
					// System.out.println("Observing " + report);
					observedReports.add(report);
				}
			}
		}

		if (object instanceof ValidationError) {
			if (((ValidationError<?, ?>) object).isFixable()) {
				return FIBUtilsIconLibrary.FIXABLE_ERROR_ICON;
			}
			else {
				return FIBUtilsIconLibrary.UNFIXABLE_ERROR_ICON;
			}
		}
		else if (object instanceof ValidationWarning) {
			if (((ValidationWarning<?, ?>) object).isFixable()) {
				return FIBUtilsIconLibrary.FIXABLE_WARNING_ICON;
			}
			else {
				return FIBUtilsIconLibrary.UNFIXABLE_WARNING_ICON;
			}
		}
		else if (object instanceof InformationIssue) {
			return FIBUtilsIconLibrary.INFO_ISSUE_ICON;
		}
		else if (object instanceof FixProposal) {
			return FIBUtilsIconLibrary.FIX_PROPOSAL_ICON;
		}

		return null;
	}

	@NotificationUnsafe
	public final ImageIcon iconForObject(Object object) {

		ImageIcon returned = cachedIcons.get(object);
		if (returned == null) {
			returned = retrieveIconForObject(object);
			if (object instanceof FIBModelObject && hasValidationReport((FIBModelObject) object)) {
				if (hasErrors((FIBModelObject) object)) {
					returned = IconFactory.getImageIcon(returned, FIBUtilsIconLibrary.ERROR);
				}
				else if (hasWarnings((FIBModelObject) object)) {
					returned = IconFactory.getImageIcon(returned, FIBUtilsIconLibrary.WARNING);
				}
				cachedIcons.put(object, returned);
			}
			else if (object instanceof SourceElement) {
				// Decorate a metamodel element's own icon with an error/warning marker when
				// pamela-editor's own Issue model (source-metamodel-design.md §12,
				// validation-log-panel-design.md) raised a diagnostic against it — the same
				// base-icon-plus-marker composition already used for action icons
				// (context-menu-design.md, PamelaEditorIconLibrary#decorate). hasErrors/
				// hasWarnings roll up through the containment hierarchy (package → entity →
				// property…), so a container's icon reflects any issue nested underneath it,
				// not only issues raised directly against it. Not cached in cachedIcons:
				// IconFactory#getImageIcon already caches the composed image per (base icon,
				// marker) pair, and the underlying lookup is a cheap scan over the metamodel's
				// (typically small) issue list.
				returned = PamelaEditorIconLibrary.decorateWithSeverity(returned, (SourceElement) object);
			}
			else if (object instanceof PamelaProject) {
				// PamelaProject (the top-level browser node) is a UI-level wrapper, not a
				// SourceElement — decorate it via the SourceMetaModel it owns.
				returned = PamelaEditorIconLibrary.decorateWithSeverity(
						returned, ((PamelaProject) object).getMetaModel());
			}
		}
		return returned;
	}

	protected void clearCachedIcons() {
		cachedIcons.clear();
	}

	@Override
	public T getDataObject() {
		return (T) super.getDataObject();
	}

	public FIBModelObject getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(FIBModelObject selectedObject) {
		if ((selectedObject == null && this.selectedObject != null)
				|| (selectedObject != null && !selectedObject.equals(this.selectedObject))) {
			FIBModelObject oldValue = this.selectedObject;
			this.selectedObject = selectedObject;
			getPropertyChangeSupport().firePropertyChange("selectedObject", oldValue, selectedObject);
		}
	}

	public boolean hasValidationReport(FIBModelObject object) {
		FIBValidationReport validationReport = getValidationReport(object);
		return (validationReport != null);
	}

	public boolean hasErrors(FIBModelObject object) {
		FIBValidationReport validationReport = getValidationReport(object);
		if (validationReport != null) {
			return validationReport.hasErrors(object);
		}
		return false;
	}

	public boolean hasWarnings(FIBModelObject object) {
		FIBValidationReport validationReport = getValidationReport(object);
		if (validationReport != null) {
			return validationReport.hasWarnings(object);
		}
		return false;
	}

	public FIBValidationReport getValidationReport(FIBModelObject object) {
		return null;
	}

	public ValidationModel getValidationModel(FIBModelObject object) {
		if (object != null) {
			return object.getComponent().getModelFactory().getValidationModel();
		}
		return null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() instanceof ValidationReport) {
			clearCachedIcons();
		}
	}

	// =========================================================================
	// Contextual menu helpers
	// =========================================================================

	/**
	 * Builds and shows a {@link JPopupMenu} for the given target object, using the
	 * contextual actions registered on {@code app}.
	 *
	 * <p>If {@code event} is a {@link MouseEvent}, the popup is positioned at the
	 * event's on-screen coordinates. Otherwise it falls back to the current
	 * mouse-pointer position.</p>
	 *
	 * <p>The menu is not shown when no applicable actions are found.</p>
	 *
	 * @param target the object that was right-clicked (may be null — no-op)
	 * @param event  the raw event object passed by the Gina FIB binding (may be
	 *               a {@link MouseEvent} or another type)
	 * @param app    the running application instance
	 */
	protected static void showContextualMenu(Object target, Object event,
	                                          PamelaEditorApplication app) {
		if (target == null || app == null) {
			return;
		}
		// Delegate to the application's shared menu builder (single facet for browsers).
		// The diagram uses a multi-facet variant — see ui-design.md §18.4.
		if (event instanceof MouseEvent) {
			MouseEvent me = (MouseEvent) event;
			app.showContextualMenuFor(Collections.singletonList(target),
					me.getComponent(), me.getX(), me.getY());
		} else {
			// Fallback: no source component → show at the current cursor (screen coords).
			Point screenPos = MouseInfo.getPointerInfo().getLocation();
			app.showContextualMenuFor(Collections.singletonList(target),
					null, screenPos.x, screenPos.y);
		}
	}

}
