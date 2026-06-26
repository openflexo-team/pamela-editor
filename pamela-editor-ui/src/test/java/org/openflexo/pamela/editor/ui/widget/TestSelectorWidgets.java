/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.rm.Resource;

/**
 * Sanity checks for the selector widget family: each popup FIB loads (well-formed XML, bindings
 * resolve against its {@code dataClassName}) and each selector enumerates selectable values from a
 * real metamodel. The interactive popup/filter/browser behaviour is validated by hand via
 * {@link SelectorWidgetsDemo} (see {@code selector-widgets-design.md}).
 */
public class TestSelectorWidgets {

	private static SourceMetaModel metaModel;
	private static SourceModelEntity anEntity;

	@BeforeClass
	public static void loadMetaModel() throws Exception {
		File pamelaFile = new File(System.getProperty("user.dir"),
				"../pamela-editor-core/src/test/java/test/model2-full.pamela");
		assertTrue("Test metamodel not found: " + pamelaFile.getAbsolutePath(), pamelaFile.exists());
		metaModel = SourceMetaModelSerializer.load(pamelaFile);
		assertNotNull(metaModel);
		assertFalse("Metamodel should have entities", metaModel.getEntities().isEmpty());
		anEntity = metaModel.getEntities().values().iterator().next();
	}

	private static FIBComponent loadFIB(Resource resource) {
		assertNotNull("FIB resource not located", resource);
		FIBComponent component = ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(resource);
		assertNotNull("FIB did not load: " + resource, component);
		return component;
	}

	@Test
	public void testModelEntitySelector() {
		ModelEntitySelector selector = new ModelEntitySelector(null);
		selector.setMetaModel(metaModel);
		loadFIB(selector.getFIBResource());
		assertFalse("Entities should be selectable", selector.getAllSelectableValues().isEmpty());
		maybeBuildPanel(selector);
	}

	@Test
	public void testModelPropertySelector() {
		ModelPropertySelector selector = new ModelPropertySelector(null);
		selector.setMetaModel(metaModel);
		loadFIB(selector.getFIBResource());
		assertFalse("Properties should be selectable", selector.getAllSelectableValues().isEmpty());
		maybeBuildPanel(selector);
	}

	@Test
	public void testInitializerSelector() {
		SourceModelEntity entity = entityWith(true, false);
		InitializerSelector selector = new InitializerSelector(null);
		selector.setEntity(entity != null ? entity : anEntity);
		loadFIB(selector.getFIBResource());
		maybeBuildPanel(selector);
	}

	@Test
	public void testOperationSelector() {
		SourceModelEntity entity = entityWith(false, true);
		OperationSelector selector = new OperationSelector(null);
		selector.setEntity(entity != null ? entity : anEntity);
		loadFIB(selector.getFIBResource());
		maybeBuildPanel(selector);
	}

	@Test
	public void testJavaClassSelector() {
		JavaClassSelector selector = new JavaClassSelector(null);
		selector.setMetaModel(metaModel);
		loadFIB(selector.getFIBResource());
		assertFalse("Java classes should be selectable", selector.getAllSelectableValues().isEmpty());
		maybeBuildPanel(selector);
	}

	@Test
	public void testJavaMethodSelector() {
		JavaMethodSelector selector = new JavaMethodSelector(null);
		selector.setEntity(anEntity);
		loadFIB(selector.getFIBResource());
		assertNotNull("Entity type should resolve", selector.getResolvedType());
		maybeBuildPanel(selector);
	}

	// Build the popup panel (parses + wires the FIB view) when a display is available.
	private static <T> void maybeBuildPanel(FIBPamelaObjectSelector<T> selector) {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		FIBPamelaObjectSelector<T>.SelectorDetailsPanel panel = selector.createCustomPanel(null);
		assertNotNull(panel);
		assertNotNull(panel.getFIBView());
	}

	private static SourceModelEntity entityWith(boolean initializers, boolean operations) {
		for (SourceModelEntity e : metaModel.getEntities().values()) {
			if (initializers && e.getInitializers().isEmpty()) {
				continue;
			}
			if (operations && e.getDeclaredCustomMethods().isEmpty()) {
				continue;
			}
			return e;
		}
		return null;
	}
}
