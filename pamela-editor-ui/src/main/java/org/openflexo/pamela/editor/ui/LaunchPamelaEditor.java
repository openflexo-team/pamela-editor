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

import java.awt.Image;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.openflexo.localization.FlexoLocalization;
import org.openflexo.logging.FlexoLoggingManager;
import org.openflexo.toolbox.ToolBox;

public class LaunchPamelaEditor {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> init());
	}

	private static void init() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			if (ToolBox.isMacOS()) {
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("com.apple.mrj.application.apple.menu.about.name", "PamelaEditor");
				//ToolBox.updateSystemProperty("apple.awt.application.name", "Prout");
				//System.setProperty("apple.awt.application.name", "Prout");
			}

			FlexoLoggingManager.initialize(-1, true, null, Level.INFO, null);
			FlexoLocalization.initWith(PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		try {
			Class<?> eawtApplication = Class.forName("com.apple.eawt.Application");
			/*Class<?> quitHandler = findHandlerClass("QuitHandler");
			Class<?> aboutHandler = findHandlerClass("AboutHandler");
			Class<?> openFilesHandler = findHandlerClass("OpenFilesHandler");
			Class<?> preferencesHandler = findHandlerClass("PreferencesHandler");
			Object proxy = Proxy.newProxyInstance(PlatformHookOsx.class.getClassLoader(),
					new Class<?>[] { quitHandler, aboutHandler, openFilesHandler, preferencesHandler }, this);*/
			Object appli = eawtApplication.getConstructor((Class[]) null).newInstance((Object[]) null);
			/*if (ToolBox.getJavaVersion() >= 9) {
				setHandlers(Desktop.class, quitHandler, aboutHandler, openFilesHandler, preferencesHandler, proxy, Desktop.getDesktop());
			}
			else {
				setHandlers(eawtApplication, quitHandler, aboutHandler, openFilesHandler, preferencesHandler, proxy, appli);
				// this method has been deprecated, but without replacement. To remove with Java 9 migration
				eawtApplication.getDeclaredMethod("setEnabledPreferencesMenu", boolean.class).invoke(appli, Boolean.TRUE);
			}*/
			// setup the dock icon. It is automatically set with application bundle and Web start but we need
			// to do it manually if run with `java -jar``
			eawtApplication.getDeclaredMethod("setDockIconImage", Image.class).invoke(appli, PamelaEditorIconLibrary.APPLICATION_ICON.getImage());
			// enable full screen
			//enableOSXFullscreen(FlexoFrame.getActiveFrame());
		} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException ex) {
			// We'll just ignore this for now. The user will still be able to close Openflexo by closing all its windows.
			System.err.println("Failed to register with OSX: " + ex);
		}

		// StringEncoder.getDefaultInstance()._addConverter(DataBinding.CONVERTER);

		PamelaEditorApplication application = new PamelaEditorApplication();
		application.showMainPanel();
		// application.newDiagramEditor();
	}

}
