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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
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

}
