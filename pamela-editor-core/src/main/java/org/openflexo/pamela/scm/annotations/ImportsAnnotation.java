package org.openflexo.pamela.scm.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openflexo.pamela.annotations.Import;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.AnnotationValue;
import com.thoughtworks.qdox.model.expression.AnnotationValueList;
import com.thoughtworks.qdox.model.expression.TypeRef;
import com.thoughtworks.qdox.model.impl.DefaultJavaAnnotation;
import com.thoughtworks.qdox.model.impl.DefaultJavaClass;

/**
 * Created by adria on 01/03/2017.
 */
public class ImportsAnnotation extends PamelaAnnotation {

	public ImportsAnnotation(JavaAnnotation annotationSource) {
		super(annotationSource);
	}

	/**
	 * Returns all the canonical class names contained in the @Imports annotation.
	 *
	 * @return
	 */
	public List<String> getValue() {
		if (annotationSource.getProperty("value") == null)
			return new ArrayList<>();
		List<AnnotationValue> annotationValues = ((AnnotationValueList) annotationSource.getProperty("value")).getValueList();
		List<JavaAnnotation> javaAnnotations = annotationValues.stream().map(annotationValue -> (JavaAnnotation) annotationValue)
				.collect(Collectors.toList());
		return javaAnnotations.stream().map(ann -> ((TypeRef) ann.getProperty("value")).getType().getCanonicalName())
				.collect(Collectors.toList());
	}

	/**
	 * @param imports
	 */
	public ImportsAnnotation setValue(List<String> imports) {
		List<AnnotationValue> annotationValues = new ArrayList<>();
		imports.forEach(imp -> annotationValues.add(new TypeRef(new DefaultJavaClass(imp))));
		List<AnnotationValue> annotationList = new ArrayList<>();
		annotationValues.stream().forEach(annotationValue -> {
			DefaultJavaAnnotation ann = new DefaultJavaAnnotation(new DefaultJavaClass(Import.class.getTypeName()), 0);
			ann.getPropertyMap().put("value", annotationValue);
			annotationList.add(ann);
		});
		AnnotationValueList annotationValueList = new AnnotationValueList(annotationList);
		annotationSource.getPropertyMap().remove("value");
		annotationSource.getPropertyMap().put("value", annotationValueList);
		return this;
	}

	/**
	 * Add the specified import to the imports annotation.
	 *
	 * @param className
	 *            The canonical name of the class to import.
	 * @return
	 */
	public ImportsAnnotation addImport(String className) {
		List<String> oldImports = getValue();
		oldImports.add(className);
		setValue(oldImports);
		return this;
	}

	/**
	 * Remove the specified import to the imports annotation.
	 *
	 * @param className
	 *            The canonical name of the class to remove.
	 * @return
	 */
	public ImportsAnnotation removeImport(String className) {
		List<String> oldImports = getValue();
		System.out.println("oldImports=" + oldImports);
		oldImports.remove(className);
		setValue(oldImports);
		return this;
	}
}
