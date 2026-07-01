/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Sanity checks for the "source folders under a project" browser structure: {@code MetaModelBrowser.fib}
 * and {@code DetailedBrowser.fib} still load (well-formed XML, bindings resolve), and
 * {@link SourceMetaModel#getSourceFolders()} / {@link SourceFolder#getPackages()} correctly nest the
 * discovered packages under their source directory.
 */
public class TestSourceFolderBrowser {

	private static SourceMetaModel metaModel;

	@BeforeClass
	public static void loadMetaModel() throws Exception {
		File pamelaFile = new File(System.getProperty("user.dir"),
				"../pamela-editor-core/src/test/java/test/model2-full.pamela");
		assertTrue("Test metamodel not found: " + pamelaFile.getAbsolutePath(), pamelaFile.exists());
		metaModel = SourceMetaModelSerializer.load(pamelaFile);
		assertNotNull(metaModel);
	}

	private static FIBComponent loadFIB(Resource resource) {
		assertNotNull("FIB resource not located", resource);
		FIBComponent component = ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(resource);
		assertNotNull("FIB did not load: " + resource, component);
		return component;
	}

	@Test
	public void testMetaModelBrowserFIBLoads() {
		loadFIB(ResourceLocator.locateResource("Fib/MetaModelBrowser.fib"));
	}

	@Test
	public void testDetailedBrowserFIBLoads() {
		loadFIB(ResourceLocator.locateResource("Fib/DetailedBrowser.fib"));
	}

	@Test
	public void testSourceFoldersMatchSourceDirectories() {
		List<File> dirs = metaModel.getSourceDirectories();
		List<SourceFolder> folders = metaModel.getSourceFolders();
		assertEquals(dirs.size(), folders.size());
		for (int i = 0; i < dirs.size(); i++) {
			assertEquals(dirs.get(i), folders.get(i).getDirectory());
			assertEquals(metaModel, folders.get(i).getMetaModel());
		}
	}

	@Test
	public void testFolderPackagesCoverAllPackages() {
		assertFalse("Test metamodel should have at least one source folder", metaModel.getSourceFolders().isEmpty());
		SourceFolder folder = metaModel.getSourceFolders().get(0);
		List<SourcePackage> folderPackages = folder.getPackages();
		assertFalse("Folder should list at least one package", folderPackages.isEmpty());
		// Single-source-directory project: the folder's packages must equal getAllPackages().
		assertEquals(metaModel.getAllPackages(), folderPackages);
	}

	@Test
	public void testGetPackagesForSourceDirectoryMatchesFolder() {
		File dir = metaModel.getSourceDirectories().get(0);
		assertEquals(metaModel.getSourceFolders().get(0).getPackages(),
				metaModel.getPackagesForSourceDirectory(dir));
	}
}
