package model.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.Constant;
import com.thoughtworks.qdox.model.expression.FieldRef;
import org.openflexo.pamela.annotations.ModelEntity;

/**
 * Created by adria on 27/02/2017.
 */
public class ModelEntityAnnotation extends PamelaAnnotation {
    public ModelEntityAnnotation(JavaAnnotation annotationSource) {
        super(annotationSource);
    }

    public boolean isAbstract() {
        return annotationSource.getProperty("isAbstract") != null
                && Boolean.valueOf(((Constant) annotationSource.getProperty("isAbstract")).getImage());
    }

    public ModelEntityAnnotation setIsAbstract(boolean isAbstract) {
        annotationSource.getPropertyMap().remove("isAbstract");
        annotationSource.getPropertyMap().put("isAbstract", Constant.newBooleanLiteral(Boolean.toString(isAbstract)));
        return this;
    }

    public boolean getInheritInitializers() {
        return annotationSource.getProperty("inheritInitializers") != null
                && Boolean.valueOf(((Constant) annotationSource.getProperty("inheritInitializers")).getImage());
    }

    public ModelEntityAnnotation setInheritInitializers(boolean inheritInitializers) {
        annotationSource.getPropertyMap().remove("inheritInitializers");
        annotationSource.getPropertyMap().put("inheritInitializers", Constant.newBooleanLiteral(Boolean.toString(inheritInitializers)));
        return this;
    }

    public ModelEntity.InitPolicy getInitPolicy() {
        return ModelEntity.InitPolicy.valueOf(((FieldRef) annotationSource.getProperty("initPolicy")).getName());
    }

    public ModelEntityAnnotation setInitPolicy(ModelEntity.InitPolicy initPolicy) {
        annotationSource.getPropertyMap().remove("initPolicy");
        annotationSource.getPropertyMap().put("initPolicy", new FieldRef(initPolicy.toString()));
        return this;
    }
}
