package org.openflexo.pamela.scm.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.FieldRef;

/**
 * Created by adria on 03/01/2017.
 */
public class RemoverAnnotation extends PamelaAnnotation {

    public RemoverAnnotation(JavaAnnotation javaAnnotation) {
        super(javaAnnotation);
    }

    /**
     * @return the value of value. If not found, returns null.
     */
    public String getValue() {
        return ((FieldRef) annotationSource.getProperty("value")).getName();
    }

    /**
     * @param value the new value of value.
     * @return this instance.
     */
    public RemoverAnnotation setValue(String value) {
        annotationSource.getPropertyMap().remove("value");
        annotationSource.getPropertyMap().put("value", new FieldRef(value));
        return this;
    }
}
