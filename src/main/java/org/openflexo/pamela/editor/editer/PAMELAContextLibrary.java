package org.openflexo.pamela.editor.editer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;

import com.thoughtworks.qdox.model.JavaClass;
/**
 * Same with pamela-core
 * @author lenovo
 *
 */
public class PAMELAContextLibrary {

	private static Map<JavaClass,PAMELAContext> contexts = new Hashtable<JavaClass,PAMELAContext>();
	private static Map<Set<JavaClass>, PAMELAContext> setContexts = new Hashtable<Set<JavaClass>, PAMELAContext>(); 
	
	public static synchronized PAMELAContext getModelContext(JavaClass baseClass) throws ModelDefinitionException {
		PAMELAContext context = contexts.get(baseClass);
		if(context == null){
			contexts.put(baseClass, context = new PAMELAContext(baseClass));
		}
		return context;
	}

	public static boolean hasContext(Class<?> baseClass) {
		return contexts.get(baseClass) != null;
	}

	public static PAMELAContext getCompoundModelContext(JavaClass... classes) throws ModelDefinitionException {
		if (classes.length == 1) {
			return getModelContext(classes[0]);
		}

		Set<JavaClass> set = new HashSet<JavaClass>(Arrays.asList(classes));
		PAMELAContext context = setContexts.get(set);
		if (context == null) {
			setContexts.put(set, context = new PAMELAContext(classes));
		}
		return context;
	}

	public static PAMELAContext getCompoundModelContext(JavaClass baseClass, JavaClass[] classes) throws ModelDefinitionException {
		JavaClass[] newArray = new JavaClass[classes.length + 1];
		for (int i = 0; i < classes.length; i++) {
			newArray[i] = classes[i];
		}
		newArray[classes.length] = baseClass;

		return getCompoundModelContext(newArray);
	}
	
	
}
