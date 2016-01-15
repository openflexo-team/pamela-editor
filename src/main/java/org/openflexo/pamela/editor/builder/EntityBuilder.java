package org.openflexo.pamela.editor.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.openflexo.model.exceptions.ModelDefinitionException;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.exceptions.EntityRedefineException;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;

public class EntityBuilder {

	public static JavaProjectBuilder builder = new JavaProjectBuilder();
	public static List<File> filelist = new ArrayList<File>();
	public static List<String> filepathlist = new ArrayList<String>();
	public static Map<String,PAMELAEntity> entityLibrary = new HashMap<String,PAMELAEntity>();
	
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
					if(entityLibrary.put(pe.getName(), pe)!=null){
						throw new EntityRedefineException(pe.getName());
					}					
				}
				
			}

		} catch (ModelDefinitionException | EntityRedefineException | FileNotFoundException e) {
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
