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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;

/**
 * Coverage for {@link NewPackageAction}'s context-sensitive applicability and pre-fill logic —
 * the part that does not require a live {@link org.openflexo.pamela.editor.ui.PamelaEditorApplication}
 * (neither {@link NewPackageAction#isApplicable} nor {@link NewPackageAction#prepareDialog} reads
 * the {@code app} argument). Actual package creation via
 * {@code SourceMetaModel.createPackage(name, folder)} is covered at the core level by
 * {@code TestMutations.testCreatePackage}.
 */
public class TestNewPackageAction {

	private static SourceMetaModel metaModel;
	private static SourceFolder aFolder;

	@BeforeClass
	public static void loadMetaModel() throws Exception {
		File pamelaFile = new File(System.getProperty("user.dir"),
				"../pamela-editor-core/src/test/java/test/model2-full.pamela");
		assertTrue("Test metamodel not found: " + pamelaFile.getAbsolutePath(), pamelaFile.exists());
		metaModel = SourceMetaModelSerializer.load(pamelaFile);
		assertFalse("Metamodel should have source folders", metaModel.getSourceFolders().isEmpty());
		aFolder = metaModel.getSourceFolders().get(0);
	}

	@Test
	public void testApplicableFromMetaModelAndFolderOnly() {
		NewPackageAction action = new NewPackageAction();
		assertTrue("Applicable on a SourceFolder", action.isApplicable(aFolder));
		assertTrue("Applicable on the SourceMetaModel itself", action.isApplicable(metaModel));
		assertFalse("Not applicable on an unrelated object", action.isApplicable("not a source element"));
		assertFalse("Not applicable on null", action.isApplicable(null));
		// Not meant to be triggered from a package or an entity node.
		assertFalse("Not applicable on a SourcePackage",
				action.isApplicable(aFolder.getPackages().isEmpty() ? null : aFolder.getPackages().get(0)));
	}

	@Test
	public void testPrepareDialogFromFolderPrefillsFolder() {
		NewPackageAction action = new NewPackageAction();
		assertTrue(action.prepareDialog(aFolder, null));
		assertEquals(metaModel, action.getMetaModel());
		assertEquals(aFolder, action.getSourceFolder());
		assertEquals("", action.getName());
		assertFalse("Not input-valid until a name is typed", action.isInputValid());
	}

	@Test
	public void testPrepareDialogFromMetaModelPrefillsNothing() {
		NewPackageAction action = new NewPackageAction();
		assertTrue(action.prepareDialog(metaModel, null));
		assertEquals(metaModel, action.getMetaModel());
		assertNull("Folder must be chosen by the user from a project-root context",
				action.getSourceFolder());
		assertFalse("Not input-valid without a folder", action.isInputValid());
	}

	@Test
	public void testIsInputValidRejectsIllegalOrDuplicateNames() {
		NewPackageAction action = new NewPackageAction();
		action.prepareDialog(aFolder, null);

		action.setName("not a legal package name");
		assertFalse("An illegal package name must be rejected", action.isInputValid());

		action.setName("");
		assertFalse("An empty package name must be rejected", action.isInputValid());

		if (!aFolder.getPackages().isEmpty()) {
			action.setName(aFolder.getPackages().get(0).getQualifiedName());
			assertFalse("A package already present in the folder must be rejected",
					action.isInputValid());
		}

		action.setName("brand.new.subpackage");
		assertTrue("A fresh, legal, dotted package name must be accepted", action.isInputValid());
	}
}
