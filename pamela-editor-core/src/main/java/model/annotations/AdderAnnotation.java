package model.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.FieldRef;

/**
 * Created by adria on 03/01/2017.
 */
public class AdderAnnotation extends PamelaAnnotation {

    public AdderAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    /**
     * @return the value of value. If not found, returns null.
     */
    public String getValue() {
        return ((FieldRef) annotationSource.getProperty("value")).getName();
    }

    /**
     * @param value the new value of value.
     * @return this instance
     */
    public AdderAnnotation setValue(String value) {
        annotationSource.getPropertyMap().remove("value");
        annotationSource.getPropertyMap().put("value", new FieldRef(value));
        return this;
    }
}
