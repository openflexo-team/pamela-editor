package org.openflexo.pamela.editor.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openflexo.pamela.editor.editer.PAMELAContext;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;

public class EntityBuilder {

	public static JavaProjectBuilder builder = new JavaProjectBuilder();
	public static List<File> filelist = new ArrayList<File>();
	public static List<String> filepathlist = new ArrayList<String>();
	public static Map<String,PAMELAEntity> entities = new HashMap<String,PAMELAEntity>();
	//use for import Entities
	public static Map<String,PAMELAEntity> entityLibrary = new HashMap<String,PAMELAEntity>();
	
	public static PAMELAContext context = null;
	
	
	/*
	public static void load(String codeSourcePath, String className) {	
		try {
			
			builder.addSourceTree(new File(codeSourcePath));		
			PAMELAEntity pe=null;
			for(JavaSource js:builder.getSources()){
				System.out.println("url:"+js.getURL());
				for(JavaClass jc:js.getClasses()){
					//System.out.println("class LOAD:"+jc.getCanonicalName());
					pe = new PAMELAEntity(jc);
					//read file content and save in PAMELAEntity								
					Scanner scanner = new Scanner(new File(js.getURL().getPath()));
					String content = scanner.useDelimiter("\\Z").next();				
					pe.addsource(js.getURL().getPath(), content);									
					if(entities.put(pe.getName(), pe)!=null){
						throw new EntityRedefineException(pe.getName());
					}					
				}
				
			}

		} catch (EntityRedefineException | FileNotFoundException | org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	public static void load(String codeSourcePath, String baseClassName) {	
		try {
			
			builder.addSourceTree(new File(codeSourcePath));		
			PAMELAEntity pe=null;
			
			JavaClass baseClass = builder.getClassByName(baseClassName);
			context = new PAMELAContext(baseClass);

		} catch (ModelDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * not use now
	 * traverse the folder to get the file list of *.java
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
