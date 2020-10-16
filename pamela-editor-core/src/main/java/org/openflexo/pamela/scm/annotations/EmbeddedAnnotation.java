package org.openflexo.pamela.scm.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.AnnotationValue;
import com.thoughtworks.qdox.model.expression.AnnotationValueList;
import com.thoughtworks.qdox.model.expression.FieldRef;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by adria on 21/02/2017.
 */
public class EmbeddedAnnotation extends PamelaAnnotation {

    public EmbeddedAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    public List<String> getClosureConditions() {
        List<AnnotationValue> annotationValues = ((AnnotationValueList) annotationSource.getProperty("closureConditions")).getValueList();
        return annotationValues.stream().map(annotationValue -> ((FieldRef) annotationValue).getName()).collect(Collectors.toList());
    }

    public EmbeddedAnnotation setClosureConditions(List<String> properties) {
        annotationSource.getPropertyMap().remove("closureConditions");
        List<AnnotationValue> annotationValues = properties.stream().map(property -> new FieldRef(property)).collect(Collectors.toList());
        annotationSource.getPropertyMap().put("closureConditions", new AnnotationValueList(annotationValues));
        return this;
    }

    public EmbeddedAnnotation addClosureCondition(String property) {
        if (annotationSource.getProperty("closureConditions") == null) {
            annotationSource.getPropertyMap().remove("closureConditions");
            List<AnnotationValue> annotationValues = Collections.singletonList(new FieldRef(property));
            annotationSource.getPropertyMap().put("closureConditions", new AnnotationValueList(annotationValues));
        } else {
            List<String> properties = getClosureConditions();
            properties.add(property);
            annotationSource.getPropertyMap().remove("closureConditions");
            List<AnnotationValue> annotationValues = properties.stream().map(prop -> new FieldRef(prop)).collect(Collectors.toList());
            annotationSource.getPropertyMap().put("closureConditions", new AnnotationValueList(annotationValues));
        }
        return this;
    }

    public EmbeddedAnnotation removeClosureCondition(String property) {
        if (annotationSource.getProperty("closureConditions") != null) {
            List<String> properties = getClosureConditions();
            properties.remove(property);
            annotationSource.getPropertyMap().remove("closureConditions");
            List<AnnotationValue> annotationValues = properties.stream().map(prop -> new FieldRef(prop)).collect(Collectors.toList());
            annotationSource.getPropertyMap().put("closureConditions", new AnnotationValueList(annotationValues));
        }
        return this;
    }

    public List<String> getDeletionConditions() {
        List<AnnotationValue> annotationValues = ((AnnotationValueList) annotationSource.getProperty("deletionConditions")).getValueList();
        return annotationValues.stream().map(annotationValue -> ((FieldRef) annotationValue).getName()).collect(Collectors.toList());
    }

    public EmbeddedAnnotation setDeletionConditions(List<String> properties) {
        annotationSource.getPropertyMap().remove("deletionConditions");
        List<AnnotationValue> annotationValues = properties.stream().map(property -> new FieldRef(property)).collect(Collectors.toList());
        annotationSource.getPropertyMap().put("deletionConditions", new AnnotationValueList(annotationValues));
        return this;
    }

    public EmbeddedAnnotation addDeletionCondition(String property) {
        if (annotationSource.getProperty("deletionConditions") == null) {
            annotationSource.getPropertyMap().remove("deletionConditions");
            List<AnnotationValue> annotationValues = Collections.singletonList(new FieldRef(property));
            annotationSource.getPropertyMap().put("deletionConditions", new AnnotationValueList(annotationValues));
        } else {
            List<String> properties = getClosureConditions();
            properties.add(property);
            annotationSource.getPropertyMap().remove("deletionConditions");
            List<AnnotationValue> annotationValues = properties.stream().map(prop -> new FieldRef(prop)).collect(Collectors.toList());
            annotationSource.getPropertyMap().put("deletionConditions", new AnnotationValueList(annotationValues));
        }
        return this;
    }

    public EmbeddedAnnotation removeDeletionCondition(String property) {
        if (annotationSource.getProperty("deletionConditions") != null) {
            List<String> properties = getClosureConditions();
            properties.remove(property);
            annotationSource.getPropertyMap().remove("deletionConditions");
            List<AnnotationValue> annotationValues = properties.stream().map(prop -> new FieldRef(prop)).collect(Collectors.toList());
            annotationSource.getPropertyMap().put("deletionConditions", new AnnotationValueList(annotationValues));
        }
        return this;
    }
}
