package org.openflexo.pamela.scm.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openflexo.toolbox.FileUtils;

/**
 *
 */
public abstract class PamelaSCMModelTest {

	private static File rootTempDir;

	public static List<File> makeNewTempSourceDirectories(File rootDirectory, File... directoriesToCopy) throws IOException {
		File tempFile = File.createTempFile("Temp", "");
		rootTempDir = new File(tempFile.getParentFile(), tempFile.getName() + "/Sources");
		tempFile.delete();

		System.out.println("Working on " + getRootTempDir());
		List<File> returned = new ArrayList<>();

		for (File dir : directoriesToCopy) {
			String relativePath = FileUtils.makeFilePathRelativeToDir(dir, rootDirectory);
			File newDirectory = new File(rootTempDir, relativePath);
			newDirectory.mkdirs();
			FileUtils.copyContentDirToDir(dir, newDirectory);
			returned.add(newDirectory);
			System.out.println("  > Working on specific directory " + newDirectory);
		}
		return returned;
	}

	public static void deleteTempSourceDirectories() {
		System.out.println("A la fin je supprime " + rootTempDir);
		FileUtils.recursiveDeleteFile(rootTempDir);
	}

	public static File getRootTempDir() {
		return rootTempDir;
	}

}
