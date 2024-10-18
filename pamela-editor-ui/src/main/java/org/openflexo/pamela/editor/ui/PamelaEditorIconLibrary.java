/**
 * 
 * Copyright (c) 2013-2014, Openflexo
 * Copyright (c) 2011-2012, AgileBirds
 * 
 * This file is part of Gina-swing-editor, a component of the software infrastructure 
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

import javax.swing.ImageIcon;

import org.openflexo.icon.IconMarker;
import org.openflexo.icon.ImageIconResource;
import org.openflexo.rm.ResourceLocator;

/**
 * Provides graphical resources used in the context of Pamela editor
 * 
 * 
 * @author sylvain
 * 
 */
public class PamelaEditorIconLibrary {

	public static final ImageIcon DELETE_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Delete.gif"));
	public static final ImageIcon HELP_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Help.gif"));
	public static final ImageIcon REFRESH_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Refresh.gif"));
	public static final ImageIcon INSPECT_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Inspect.gif"));
	public static final ImageIcon COPY_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Copy.gif"));
	public static final ImageIcon CUT_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Cut.gif"));
	public static final ImageIcon PASTE_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Paste.gif"));
	public static final ImageIcon UNDO_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Undo.gif"));
	public static final ImageIcon REDO_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Redo.gif"));

	public static final ImageIcon TOP_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Top.png"));
	public static final ImageIcon BOTTOM_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Bottom.png"));
	public static final ImageIcon UP_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Up.png"));
	public static final ImageIcon DOWN_ICON = new ImageIconResource(ResourceLocator.locateResource("Icons/Actions/Down.png"));

	public static final IconMarker DELETE = new IconMarker(
			new ImageIconResource(ResourceLocator.locateResource("Icons/Markers/Delete.png")), 8, 8);
	public static final IconMarker DUPLICATE = new IconMarker(
			new ImageIconResource(ResourceLocator.locateResource("Icons/Markers/Plus.png")), 8, 0);

}
