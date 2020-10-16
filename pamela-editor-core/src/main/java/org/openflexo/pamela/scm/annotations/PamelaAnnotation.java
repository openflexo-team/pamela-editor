package org.openflexo.pamela.scm.annotations;


import com.thoughtworks.qdox.model.JavaAnnotation;

/**
 * Created by adria on 25/01/2017.
 */
public abstract class PamelaAnnotation {
    protected JavaAnnotation annotationSource;

    public PamelaAnnotation(JavaAnnotation annotationSource) {
        this.annotationSource = annotationSource;
    }

    public JavaAnnotation getAnnotationSource() {
        return annotationSource;
    }
}
