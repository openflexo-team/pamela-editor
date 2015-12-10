package org.openflexo.pamela.editor.annotations;



import org.openflexo.pamela.editor.editer.PAMELAProperty.Cardinality;
import org.openflexo.pamela.editor.editer.utils.UtilPAMELA;

import com.thoughtworks.qdox.model.JavaAnnotation;


public class GetterA  extends AnnotationA{

	String UNDEFINED = "";

	/**
	 * The property identifier of this getter
	 * 
	 * @return the property identifier of this getter
	 */
	String value;

	/**
	 * The cardinality of this getter
	 * 
	 * @return the cardinality of the getter
	 */
	Cardinality cardinality = Cardinality.SINGLE;

	/**
	 * The inverse property identifier of this getter. Depending on the cardinality of this property and its inverse, this property will be
	 * either set or added to the inverse property of this property value.
	 * 
	 * @return
	 */
	String inverse = UNDEFINED;

	/**
	 * A string convertable value that is set by default on the property identified by this getter
	 * 
	 * @return the string converted default value.
	 */
	String defaultValue = UNDEFINED;

	/**
	 * Indicates that the type returned by this getter can be converted to a string using a {@link Converter}. Upon
	 * serialization/deserialization, the {@link ModelFactory} will provide, through its {@link StringEncoder}, an appropriate
	 * {@link Converter}. Failing to do that will result in an Exception
	 * 
	 * The default value is <code>false</code>
	 * 
	 * @return true if the type returned by this getter can be converted to a string.
	 */
	boolean isStringConvertable = false;

	/**
	 * Indicates that the type returned by this getter should not be included in the model. This flag allows to manipulate types that are
	 * unknown to PAMELA. If set to true, PAMELA will not try to interpret those classes and will not be able to serialize them
	 * 
	 * @return true if PAMELA should not import the type of the property identified by this getter.
	 */
	boolean ignoreType = false;
	
	String type = "java.lang.Object";
	
	String keyType = "";
	
	
	
	
	public GetterA(String value, Cardinality cardinality, String inverse, String defaultValue,
			boolean isStringConvertable, boolean ignoreType,String type, String keyType) {
		this.value = value;
		if(cardinality!=null)
			this.cardinality = cardinality;
		if(inverse!=null)
			this.inverse = inverse;
		if(defaultValue!=null)
			this.defaultValue = defaultValue;
		this.isStringConvertable = isStringConvertable;
		this.ignoreType = ignoreType;
		
		this.type = type;
		this.keyType = keyType;
		
	}
	
	//getJavaAnnotation
	public JavaAnnotation getJavaAnnotation(){
		return UtilPAMELA.buildAnnotation(this.toString());
	}

	/* use for javaAnnotation builder*/
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("@");
		sb.append("Getter(");
		sb.append("value = ").append(value);
		if(this.cardinality!= Cardinality.SINGLE)
			sb.append(", ").append("cardinality = ").append("Cardinality."+this.cardinality);
		if(!this.inverse.equals(UNDEFINED))
			sb.append(", ").append("inverse = ").append(this.inverse);
		if(!this.defaultValue.equals(UNDEFINED))
			sb.append(", ").append("defaultValue = ").append(this.defaultValue);
		if(this.isStringConvertable)
			sb.append(", ").append("isStringConvertable = ").append("true");
		if(this.ignoreType)
			sb.append(", ").append("ignoreType = ").append("true");
		sb.append(")");
		return sb.toString();
		
	}

	
	//====getters
	public String getValue() {
		return value;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	public String getInverse() {
		return inverse;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isStringConvertable() {
		return isStringConvertable;
	}

	public boolean isIgnoreType() {
		return ignoreType;
	}

	public String getType() {
		return type;
	}

	public String getKeyType() {
		return keyType;
	}

	

}
