package org.openflexo.pamela.scm.annotations;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.expression.Constant;
import com.thoughtworks.qdox.model.expression.FieldRef;
import org.openflexo.pamela.annotations.Getter;

/**
 * Created by adria on 03/01/2017.
 */
public class GetterAnnotation extends PamelaAnnotation {

    public GetterAnnotation(JavaAnnotation javaAnnotation) {
        super(javaAnnotation);
    }

    /**
     * Returns the value of value, or null if the value couldn't be found.
     *
     * @return the value of value. If not found, returns null.
     */
    public String getValue() {
        return ((FieldRef) annotationSource.getProperty("value")).getName();
    }

    /**
     * @param value the new value of value.
     * @return this instance.
     */
    public GetterAnnotation setValue(String value) {
        annotationSource.getPropertyMap().remove("value");
        annotationSource.getPropertyMap().put("value", new FieldRef(value));
        return this;
    }

    /**
     * Returns the value of cardinality, or SINGLE if the value couldn't be found.
     *
     * @return the value of cardinality. If not found, returns SINGLE.
     */
    public Getter.Cardinality getCardinality() {
        return Getter.Cardinality.valueOf(((FieldRef) annotationSource.getProperty("cardinality")).getName());
    }

    /**
     * @param cardinality the new value of getCardinality.
     * @return this instance.
     */
    public GetterAnnotation setCardinality(Getter.Cardinality cardinality) {
        annotationSource.getPropertyMap().remove("cardinality");
        annotationSource.getPropertyMap().put("cardinality", new FieldRef(cardinality.toString()));
        return this;
    }

    /**
     * Returns the value of defaultValue, or null if the value couldn't be found.
     *
     * @return the value of defaultValue. If not found, returns null.
     */
    public String getDefaultValue() {
        return ((Constant) annotationSource.getProperty("defaultValue")).getImage();
    }

    /**
     * @param defaultValue the new value of defaultValue.
     * @return this instance.
     */
    public GetterAnnotation setDefaultValue(String defaultValue) {
        annotationSource.getPropertyMap().remove("defaultValue");
        annotationSource.getPropertyMap().put("defaultValue", Constant.newStringLiteral(defaultValue));
        return this;
    }

    /**
     * Returns the value of inverse, or null if the value couldn't be found.
     *
     * @return the value of inverse. If not found, returns null.
     */
    public String getInverse() {
        return ((FieldRef) annotationSource.getProperty("inverse")).getName();
    }

    /**
     * @param inverse the new value of inverse.
     * @return this instance.
     */
    public GetterAnnotation setInverse(String inverse) {
        annotationSource.getPropertyMap().remove("inverse");
        annotationSource.getPropertyMap().put("inverse", new FieldRef(inverse));
        return this;
    }

    /**
     * Returns the value of isStringConvertable, or false if the value couldn't be found.
     *
     * @return the value of isStringConvertable. If not found, returns false.
     */
    public boolean isStringConvertable() {
        return annotationSource.getProperty("isStringConvertable") != null
                && Boolean.valueOf(((Constant) annotationSource.getProperty("isStringConvertable")).getImage());
    }

    /**
     * @param isStringConvertable the new value of isStringConvertable.
     * @return this instance.
     */
    public GetterAnnotation setIsStringConvertable(boolean isStringConvertable) {
        annotationSource.getPropertyMap().remove("isStringConvertable");
        annotationSource.getPropertyMap().put("isStringConvertable", Constant.newBooleanLiteral(Boolean.toString(isStringConvertable)));
        return this;
    }

    /**
     * Returns the value of ignoreType, or false if the value couldn't be found.
     *
     * @return the value of ignoreType. If not found, returns false.
     */
    public boolean getIgnoreType() {
        return annotationSource.getProperty("ignoreType") != null
                && Boolean.valueOf(((Constant) annotationSource.getProperty("ignoreType")).getImage());
    }

    /**
     * @param ignoreType the new value of ignoreType.
     * @return this instance.
     */
    public GetterAnnotation setIgnoreType(boolean ignoreType) {
        annotationSource.getPropertyMap().remove("ignoreType");
        annotationSource.getPropertyMap().put("ignoreType", Constant.newBooleanLiteral(Boolean.toString(ignoreType)));
        return this;
    }

}
