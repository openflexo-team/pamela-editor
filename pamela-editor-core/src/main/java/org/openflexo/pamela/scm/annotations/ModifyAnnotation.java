package org.openflexo.pamela.scm.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.Constant;

/**
 * Created by adria on 27/02/2017.
 */
public class ModifyAnnotation extends PamelaAnnotation {
    public ModifyAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    public String getForward() {
        return ((Constant) annotationSource.getProperty("forward")).getImage();
    }

    public ModifyAnnotation setForward(String forward) {
        annotationSource.getPropertyMap().remove("forward");
        annotationSource.getPropertyMap().put("forward", Constant.newStringLiteral(forward));
        return this;
    }

    public boolean getSynchWithForward() {
        return annotationSource.getProperty("synchWithForward") != null
                && Boolean.valueOf(((Constant) annotationSource.getProperty("synchWithForward")).getImage());
    }

    public ModifyAnnotation setSynchWithForward(boolean synchWithForward) {
        annotationSource.getPropertyMap().remove("synchWithForward");
        annotationSource.getPropertyMap().put("synchWithForward", Constant.newBooleanLiteral(Boolean.toString(synchWithForward)));
        return this;
    }
}
