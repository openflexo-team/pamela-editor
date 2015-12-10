package org.openflexo.pamela.editor.editer.utils;

import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.html.parser.Entity;

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
	
	

	/**
	 * use the name of the class to get JavaClass from the builder of qdox
	 * 
	 * @param name
	 * @return
	 */
	/*
	public static JavaClass getJavaClassFromName(String name) {
		JavaClass cls = EntityBuilder.builder.getClassByName(name);
		return cls;

	}*/

	
	
	/**
	 * use regex to get the content of the List or Map
	 * 
	 * @param list
	 * @return
	 */
	/*
	public static JavaClass getActualTypeArgument(String list) {
		String regex = "\\<.*\\>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(list);
		// System.out.println("replace: "+list.replaceAll(regex, "O"));
		while (matcher.find()) {
			int length = matcher.group(0).length();
			String match = matcher.group(0).substring(1, length - 1);
			// System.out.println("find class:"+match);
			return getJavaClassFromName(match);
		}
		return null;
	}*/
	
	
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
	 
	 /*
	 public static String firstUpperRestLowCase(String str){
		
	 }*/
	 
}
