package org.openflexo.pamela.editor.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.openflexo.model.exceptions.ModelDefinitionException;
import org.openflexo.pamela.editor.editer.PAMELAEntity;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;

public class EntityBuilder {

	public static JavaProjectBuilder builder = new JavaProjectBuilder();
	
	public static PAMELAEntity load(String codeSourcePath, String className) {	
		try {
			/* TODO there's a problem when i load as a folder, it can't find the annotation*/
			// load entity
			//builder.addSourceFolder(new File(codeSourcePath));
			builder.addSourceTree(new File(codeSourcePath));
			//builder.addSource(new File(codeSourcePath));
			/* get implementedClass from source */
			
			JavaClass cls = builder.getClassByName(className);
			//verify if the cls is exist
			if(cls.getPackage()==null){
				throw new ClassNotFoundException();
			}


			PAMELAEntity pamelaEntity = new PAMELAEntity(cls);
			return pamelaEntity;


		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ModelDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
