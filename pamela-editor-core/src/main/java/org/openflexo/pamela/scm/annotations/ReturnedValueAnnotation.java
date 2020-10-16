package org.openflexo.pamela.scm.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.FieldRef;

/**
 * Created by adria on 21/02/2017.
 */
public class ReturnedValueAnnotation extends PamelaAnnotation {

    public ReturnedValueAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    public String getValue() {
        return ((FieldRef) annotationSource.getProperty("value")).getName();
    }

    public ReturnedValueAnnotation setValue(String value) {
        annotationSource.getPropertyMap().remove("value");
        annotationSource.getPropertyMap().put("value", new FieldRef(value));
        return this;
    }
}
