package org.openflexo.pamela.editor.editer;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.qdox.model.JavaClass;

/**
 *  to save all the entity
 * @author lenovo
 *
 */
public class PAMELAEntityLiberary {
	
	private static Map<JavaClass,PAMELAEntity> entities = new HashMap<JavaClass,PAMELAEntity>();
	
	
	public static PAMELAEntity get(JavaClass inplementedInterface){
		return entities.get(inplementedInterface);
	}
	
	
	
}
