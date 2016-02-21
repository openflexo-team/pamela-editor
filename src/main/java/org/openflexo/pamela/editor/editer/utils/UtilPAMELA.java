package org.openflexo.pamela.editor.editer.utils;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openflexo.pamela.editor.annotations.GetterA;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.Cardinality;

import com.thoughtworks.qdox.model.JavaAnnotatedElement;
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
	public static JavaAnnotation getAnnotation(JavaAnnotatedElement m, String annotationName) {
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
	 * use qdox builder to get the
	 * 
	 * @param implementedInterface
	 * @param className:
	 *            can be simple or qualified name, if we use simple name, this
	 *            function will find source class in the same package of
	 *            implementedInterface
	 * @return
	 */
	public static JavaClass getClassByName(JavaClass implementedInterface, String className) {
		JavaClass clazz = null;
		if (className.split("\\.").length > 1)
			// full qualified name
			clazz = EntityBuilder.builder.getClassByName(className);
		else {
			// find with simple name
			clazz = EntityBuilder.builder.getClassByName(implementedInterface.getPackageName() + "." + className);
		}
		return clazz;
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

	/**
	 * create annotation string like @AnnotationName(key1=value1,key2,value2...)
	 * 
	 * @param annotationName
	 * @param params
	 * @return
	 */
	public static String buildAnnotationString(String annotationName, Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("@").append(annotationName);
		if (params.size() > 0) {
			sb.append("(");
			for (Entry<String, String> param : params.entrySet()) {
				sb.append(param.getKey()).append(" = ").append(param.getValue()).append(",");
			}
			sb.setLength(sb.length() - 1);
			sb.append(")");
		}
		return sb.toString();
	}

	/**
	 * Build method getXXX with Annotation Getter
	 * 
	 * @param identify
	 * @param cardinality
	 * @param inverse
	 * @param defaultValue
	 * @param isStringConvertable
	 * @param ignoreType
	 * @param type
	 * @param keyType
	 * @return
	 */
	public static String getterCreator(String identify, Cardinality cardinality, String inverse, String defaultValue,
			boolean isStringConvertable, boolean ignoreType, String type, String keyType) {
		StringBuilder sb = new StringBuilder();
		// create annotation
		GetterA getterAnnotation = new GetterA(identify, cardinality, inverse, defaultValue, isStringConvertable,
				ignoreType);
		sb.append(getterAnnotation.toString());
		// create method
		sb.append(System.getProperty("line.separator"));
		// returnType
		switch (cardinality) {
		case SINGLE:
			sb.append(type);
			break;
		case LIST:
			sb.append("List<").append(type).append(">");
			break;
		case MAP:
			// TODO keyType==null
			sb.append("Map<").append(type).append(",").append(keyType).append(">");
			break;
		}
		sb.append(" ");
		// method name
		sb.append("get").append(identify).append(" ( ); ");
		System.out.println(sb.toString());
		return sb.toString();
	}

}
