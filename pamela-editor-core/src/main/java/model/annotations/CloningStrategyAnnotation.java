package model.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.Constant;
import com.thoughtworks.qdox.model.expression.FieldRef;
import org.openflexo.pamela.annotations.CloningStrategy;

/**
 * Created by adria on 27/02/2017.
 */
public class CloningStrategyAnnotation extends PamelaAnnotation {
    public CloningStrategyAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    public CloningStrategy.StrategyType getValue() {
        return CloningStrategy.StrategyType.valueOf(((FieldRef) annotationSource.getProperty("value")).getName());
    }

    public CloningStrategyAnnotation setValue(CloningStrategy.StrategyType strategyType) {
        annotationSource.getPropertyMap().remove("value");
        annotationSource.getPropertyMap().put("value", new FieldRef(strategyType.toString()));
        return this;
    }

    public String getFactory() {
        return ((Constant) annotationSource.getProperty("factory")).getImage();
    }

    public CloningStrategyAnnotation setFactory(String factory) {
        annotationSource.getPropertyMap().remove("factory");
        annotationSource.getPropertyMap().put("factory", Constant.newStringLiteral(factory));
        return this;
    }

    public String getCloneAfterProperty() {
        return ((FieldRef) annotationSource.getProperty("cloneAfterProperty")).getName();
    }

    public CloningStrategyAnnotation setCloneAfterProperty(String cloneAfterProperty) {
        annotationSource.getPropertyMap().remove("cloneAfterProperty");
        annotationSource.getPropertyMap().put("cloneAfterProperty", new FieldRef(cloneAfterProperty));
        return this;
    }
}
