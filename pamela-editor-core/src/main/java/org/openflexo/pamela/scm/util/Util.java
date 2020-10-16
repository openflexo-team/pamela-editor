package org.openflexo.pamela.scm.util;

import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.impl.AbstractInheritableJavaEntity;
import com.thoughtworks.qdox.model.impl.DefaultJavaAnnotation;
import com.thoughtworks.qdox.model.impl.DefaultJavaClass;
import com.thoughtworks.qdox.model.impl.DefaultJavaMethod;
import org.openflexo.pamela.annotations.ModelEntity;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by adria on 26/01/2017.
 */
public class Util {

    public static boolean isPamelaEntity(JavaClass clazz) {
        return clazz.getAnnotations()
                .stream()
                .anyMatch(x -> Objects.equals(x.getType().getFullyQualifiedName(), ModelEntity.class.getName()));
    }

    public static boolean hasAnnotation(JavaAnnotatedElement element, Class<? extends Annotation> annotation) {
        return element.getAnnotations().stream().anyMatch(ann -> Objects.equals(ann.getType().getCanonicalName(), annotation.getCanonicalName()));
    }

    public static JavaAnnotation addAnnotation(JavaAnnotatedElement element, Class<? extends Annotation> annotation) {
        JavaAnnotation result = new DefaultJavaAnnotation(new DefaultJavaClass(annotation.getCanonicalName()), 0);
        ArrayList<JavaAnnotation> annotations = new ArrayList<>();
        annotations.add(result);
        annotations.addAll(element.getAnnotations());
        AbstractInheritableJavaEntity castedElement = null;
        if (element instanceof DefaultJavaClass) {
            castedElement = (DefaultJavaClass) element;
        }
        if (element instanceof DefaultJavaMethod) {
            castedElement = (DefaultJavaMethod) element;
        }
        castedElement.setAnnotations(annotations);
        return result;
    }

    public static JavaAnnotation getAnnotation(JavaAnnotatedElement element, Class<? extends Annotation> annotation) {
        Optional<JavaAnnotation> result = element.getAnnotations()
                .stream()
                .filter(ann -> Objects.equals(ann.getType().getCanonicalName(), annotation.getCanonicalName()))
                .findFirst();
        if (result.isPresent()) return result.get();
        return null;
    }

    public static String enquote(String value) {
        return "\"" + value + "\"";
    }
}
