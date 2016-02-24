package org.openflexo.pamela.editor.editer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.openflexo.pamela.editor.editer.utils.Location;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.expression.AnnotationValue;

public class EditableMethod {

	JavaMethod javaMethod = null;

	String methodName;

	/**
	 * key: Annotation name eg. Getter value: parameters eg.
	 * (key=ignoretype,value=true)
	 */
	Map<String,HashMap<String,String>> annotations = new HashMap<String, HashMap<String, String>>();

	/**
	 * location of method
	 */
	Integer begin;
	Integer end;

	/**
	 * load from java source
	 * 
	 * @param javaMethod
	 */
	public EditableMethod(@Nonnull JavaMethod methodload) {
		javaMethod = methodload;
		methodName = methodload.getName();
		// load annotations fron the javaMethod
		loadAnnotations(methodload);
		begin = Location.getMethodBeginLine(methodload);
		end = Location.getMethodLastLine(methodload);

	}

	public JavaMethod getJavaMethod() {
		return javaMethod;
	}

	/**
	 * 
	 * @param name
	 *            Name of annotaion
	 * @return
	 */
	public Map<String, String> getAnnotationByName(String name) {
		return annotations.get(name);
	}

	/**
	 * get the param value of an annotation
	 * 
	 * @param annotation
	 * @param paramkey
	 * @return paramValue, null - not found
	 */
	public String getAnnotationParam(String annotationName, String paramkey) {
		Map<String, String> annotation = annotations.get(annotationName);
		if (annotation != null) {
			return annotation.get(paramkey);
		} else
			return null;
	}

	private void loadAnnotations(JavaMethod methodload) {
		for (JavaAnnotation anno : methodload.getAnnotations()) {
			String aNameKey = anno.getType().getValue();
			for (Entry<String, AnnotationValue> entry : anno.getPropertyMap().entrySet()) {
				String paramKey = entry.getKey();
				String paramValue = entry.getValue().toString();
			}
		}
	}
}
