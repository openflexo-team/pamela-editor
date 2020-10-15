package model.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.TypeRef;
import com.thoughtworks.qdox.model.impl.DefaultJavaType;

/**
 * Created by adria on 24/01/2017.
 */
public class ImplementationClassAnnotation extends PamelaAnnotation {

    public ImplementationClassAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    public String getValue() {
        return ((TypeRef) annotationSource.getProperty("value")).getType().getValue();
    }

    public ImplementationClassAnnotation setValue(String value) {
        annotationSource.getPropertyMap().remove("value");
        annotationSource.getPropertyMap().put("value", new TypeRef(new DefaultJavaType(value)));
        return this;
    }
}
