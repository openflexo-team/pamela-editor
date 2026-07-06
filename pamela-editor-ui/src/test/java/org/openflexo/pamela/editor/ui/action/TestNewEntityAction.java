/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;

/**
 * Coverage for {@link NewEntityAction}'s context-sensitive applicability and pre-fill logic — the
 * part that does not require a live {@link org.openflexo.pamela.editor.ui.PamelaEditorApplication}
 * (neither {@link NewEntityAction#isApplicable} nor {@link NewEntityAction#prepareDialog} reads the
 * {@code app} argument). Actual entity creation via the new 4-arg
 * {@code SourceMetaModel.createEntity(name, pkg, folder, isAbstract)} primitive is covered at the
 * core level by {@code TestMutations.testCreateEntityWithFolderAbstractAndSuperEntity}.
 */
public class TestNewEntityAction {

	private static SourceMetaModel metaModel;
	private static SourceFolder aFolder;
	private static SourcePackage aPackage;
	private static SourceModelEntity anEntity;

	@BeforeClass
	public static void loadMetaModel() throws Exception {
		File pamelaFile = new File(System.getProperty("user.dir"),
				"../pamela-editor-core/src/test/java/test/model2-full.pamela");
		assertTrue("Test metamodel not found: " + pamelaFile.getAbsolutePath(), pamelaFile.exists());
		metaModel = SourceMetaModelSerializer.load(pamelaFile);
		assertFalse("Metamodel should have source folders", metaModel.getSourceFolders().isEmpty());
		aFolder = metaModel.getSourceFolders().get(0);
		assertFalse("The folder should have packages", aFolder.getPackages().isEmpty());
		aPackage = aFolder.getPackages().get(0);
		assertFalse("The package should have entities", aPackage.getEntities().isEmpty());
		anEntity = aPackage.getEntities().get(0);
	}

	@Test
	public void testApplicableFromEveryContext() {
		NewEntityAction action = new NewEntityAction();
		assertTrue("Applicable on a SourcePackage", action.isApplicable(aPackage));
		assertTrue("Applicable on a SourceFolder", action.isApplicable(aFolder));
		assertTrue("Applicable on a SourceModelEntity", action.isApplicable(anEntity));
		assertTrue("Applicable on the SourceMetaModel itself", action.isApplicable(metaModel));
		assertFalse("Not applicable on an unrelated object", action.isApplicable("not a source element"));
		assertFalse("Not applicable on null", action.isApplicable(null));
	}

	@Test
	public void testPrepareDialogFromPackagePrefillsFolderAndPackage() {
		NewEntityAction action = new NewEntityAction();
		assertTrue(action.prepareDialog(aPackage, null));
		assertEquals(metaModel, action.getMetaModel());
		assertEquals(aPackage, action.getSourcePackage());
		assertEquals(aFolder, action.getSourceFolder());
		assertNull("No parent entity pre-selected from a package", action.getSuperEntity());
		assertFalse("Not abstract by default", action.isAbstractEntity());
		assertEquals("NewEntity", action.getName());
	}

	@Test
	public void testPrepareDialogFromFolderPrefillsOnlyFolder() {
		NewEntityAction action = new NewEntityAction();
		assertTrue(action.prepareDialog(aFolder, null));
		assertEquals(metaModel, action.getMetaModel());
		assertEquals(aFolder, action.getSourceFolder());
		assertNull("Package must be chosen by the user from a folder context", action.getSourcePackage());
		assertFalse("Not input-valid until a package is chosen", action.isInputValid());
	}

	@Test
	public void testPrepareDialogFromEntityPrefillsFolderPackageAndParent() {
		NewEntityAction action = new NewEntityAction();
		assertTrue(action.prepareDialog(anEntity, null));
		assertEquals(metaModel, action.getMetaModel());
		assertEquals(anEntity.getSourcePackage(), action.getSourcePackage());
		assertEquals(aFolder, action.getSourceFolder());
		assertEquals("The right-clicked entity is offered as the parent (create-a-subtype case)",
				anEntity, action.getSuperEntity());
	}

	@Test
	public void testPrepareDialogFromMetaModelPrefillsNothing() {
		NewEntityAction action = new NewEntityAction();
		assertTrue(action.prepareDialog(metaModel, null));
		assertEquals(metaModel, action.getMetaModel());
		assertNull(action.getSourceFolder());
		assertNull(action.getSourcePackage());
		assertNull(action.getSuperEntity());
	}

	@Test
	public void testChangingFolderClearsPackageOutsideOfIt() {
		NewEntityAction action = new NewEntityAction();
		action.prepareDialog(aPackage, null);
		assertEquals(aPackage, action.getSourcePackage());

		// Picking a different folder that does not contain the current package clears it.
		for (SourceFolder other : metaModel.getSourceFolders()) {
			if (other != aFolder && !other.getPackages().contains(aPackage)) {
				action.setSourceFolder(other);
				assertNull("Package outside the newly chosen folder must be cleared",
						action.getSourcePackage());
				return;
			}
		}
		// Only one folder in this fixture: re-selecting the same folder keeps the package.
		action.setSourceFolder(aFolder);
		assertEquals(aPackage, action.getSourcePackage());
	}

	@Test
	public void testIsInputValidRejectsDuplicateOrIllegalNames() {
		NewEntityAction action = new NewEntityAction();
		action.prepareDialog(aPackage, null);

		action.setName(anEntity.getSimpleName());
		assertFalse("A name already used in the package must be rejected", action.isInputValid());

		action.setName("not a valid identifier");
		assertFalse("An illegal Java identifier must be rejected", action.isInputValid());

		action.setName("BrandNewEntity");
		assertTrue("A fresh, legal identifier must be accepted", action.isInputValid());
	}
}
