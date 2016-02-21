package org.openflexo.pamela.editor.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openflexo.pamela.editor.editer.PAMELAContext;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.PAMELAEntityLibrary;
import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;

public class EntityBuilder {

	public static JavaProjectBuilder builder = new JavaProjectBuilder();
	public static List<File> filelist = new ArrayList<File>();
	public static List<String> filepathlist = new ArrayList<String>();
	public static PAMELAEntityLibrary entityLibrary = new PAMELAEntityLibrary();
	public static PAMELAContext context = null;

	/**
	 * load single entry class
	 * 
	 * @param codeSourcePath
	 *            the path which includes the class
	 * @param baseClassName
	 *            the qualified name of entry class
	 */
	public static void load(String codeSourcePath, String baseClassName) {
		try {

			builder.addSourceTree(new File(codeSourcePath));
			PAMELAEntity pe = null;

			JavaClass baseClass = builder.getClassByName(baseClassName);
			context = new PAMELAContext(baseClass);

		} catch (ModelDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * load multi entry classes
	 * 
	 * @param codeSourcePath
	 * @param baseClassesName
	 */
	public static void load(String codeSourcePath, String... baseClassesName) {
		try {

			builder.addSourceTree(new File(codeSourcePath));
			PAMELAEntity pe = null;

			JavaClass[] baseClasses = new JavaClass[baseClassesName.length];
			for (int i = 0; i < baseClasses.length; i++) {
				baseClasses[i] = builder.getClassByName(baseClassesName[i]);
			}

			context = new PAMELAContext(baseClasses);

		} catch (ModelDefinitionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * use className to find a entity in PAMELAEntityLibrary
	 * 
	 * @param className
	 * @return
	 */
	public static PAMELAEntity getEntityByClassName(String className) {
		JavaClass jclass = builder.getClassByName(className);
		PAMELAEntity entity = PAMELAEntityLibrary.get(jclass);
		return entity;
	}

	/**
	 * not use now traverse the folder to get the file list of *.java
	 * 
	 * @param strPath
	 * @return
	 */
	public static List<File> getFileList(String strPath) {
		File dir = new File(strPath);
		File[] files = dir.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i].getName();
				if (files[i].isDirectory()) {
					getFileList(files[i].getAbsolutePath());
				} else if (fileName.endsWith("java")) {
					String strFileName = files[i].getAbsolutePath();
					filepathlist.add(strFileName);
					filelist.add(files[i]);
				} else {
					continue;
				}
			}

		}
		return filelist;
	}

}
