package org.openflexo.pamela.editor.editer.utils;

import java.util.List;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaMethod;

public class Location {
	
	//This method will be update in the future
	public static int getMethodLastLine(JavaMethod method){
		int methodLine = method.getLineNumber();
		return methodLine;
	}
	
	//This method will be update in the future
	public static int getMethodBeginLine(JavaMethod method){
		int methodLine = method.getLineNumber();
		List<JavaAnnotation> annotations = method.getAnnotations();
		int numAnotation = annotations.size();
		return methodLine-numAnotation;
	}
	

}
