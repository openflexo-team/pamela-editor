package org.openflexo.pamela.editor.editer.utils;

import java.io.StringReader;
import java.util.List;

import org.openflexo.pamela.editor.builder.EntityBuilder;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;

public class UtilPAMELA {

	/**
	 * get an annotation with its name for a JavaMethod
	 * 
	 * @param m
	 * @param annotationName
	 * @return JavaAnnotation
	 */
	public static JavaAnnotation getAnnotation(JavaMethod m, String annotationName) {
		JavaAnnotation annotation = null;
		List<JavaAnnotation> annotations = m.getAnnotations();
		for (JavaAnnotation a : annotations) {
			String value = a.getType().getValue();
			if (value.equals(annotationName)) {
				annotation = a;
				break;
			}
		}
		return annotation;
	}	
	
	 public static JavaMethod buildMethod(String methodSource) {
	        String source = "interface Something { " + methodSource + " }";
	        JavaSource javaSource = EntityBuilder.builder.addSource(new StringReader(source));
	        JavaClass javaClass = javaSource.getClasses().get(0);
	        JavaMethod javaMethod = javaClass.getMethods().get(0);
	        return javaMethod;
	    }
	 
	 public static JavaAnnotation buildAnnotation(String methodAnnotation) {
		 String source = "interface Something { \n" + methodAnnotation + "\nvoid someMethod(); }";
	        JavaSource javaSource = EntityBuilder.builder.addSource(new StringReader(source));
	        JavaClass javaClass = javaSource.getClasses().get(0);
	        JavaMethod javaMethod = javaClass.getMethods().get(0);
	        JavaAnnotation javaAnnotation = javaMethod.getAnnotations().get(0);
	        return javaAnnotation;
	    }
	 
	 
}
