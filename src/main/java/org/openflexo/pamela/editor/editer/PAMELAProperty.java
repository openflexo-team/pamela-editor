package org.openflexo.pamela.editor.editer;

import java.util.HashMap;
import java.util.Map;

import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;
import org.openflexo.pamela.editor.editer.exceptions.PropertyException;
import org.openflexo.pamela.editor.editer.utils.Location;
import org.openflexo.pamela.editor.editer.utils.UtilPAMELA;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameterizedType;
import com.thoughtworks.qdox.model.JavaType;

/**
 * the property of the entity
 * 
 * @author ddcat1991
 *
 */
public class PAMELAProperty {

	private String identifier;

	/**
	 * the entity which this property belongs to
	 */
	private PAMELAEntity pamelaEntity;
	/**
	 * A property can be a single, a List or a Map
	 */
	private Cardinality cardinality;

	/**
	 * type of the property
	 */
	private JavaType type;

	/**
	 * if this property is Map cardinality, this is the key type of the map's
	 * key, if not, keyType is null
	 */
	private JavaType keyType;

	private EditableMethod getter;
	private EditableMethod setter;
	private EditableMethod adder;
	private EditableMethod remover;

	/**
	 * the location of this property
	 */
	private Integer begin;
	private Integer end;
	/**
	 * indicate if this property has been modified -> used for the serialization
	 * in java file
	 */
	private boolean ismodified;

	/**
	 * use for loading a property from the java file and create a property
	 * 
	 * @param propertyIdentifier
	 * @param pamelaEntity
	 * @return
	 * @throws ModelDefinitionException
	 */
	public static PAMELAProperty loadPAMELAproperty(String propertyIdentifier, PAMELAEntity pamelaEntity)
			throws ModelDefinitionException {
		JavaMethod getter = null;
		JavaMethod setter = null;
		JavaMethod adder = null;
		JavaMethod remover = null;

		int beginLine = Integer.MAX_VALUE;
		int endLine = Integer.MIN_VALUE;

		JavaClass inplementedInterface = pamelaEntity.getImplementedInterface();

		// get methods from inplementedInterface
		for (JavaMethod m : inplementedInterface.getMethods()) {
			JavaAnnotation aGetter = UtilPAMELA.getAnnotation(m, "Getter");
			JavaAnnotation aSetter = UtilPAMELA.getAnnotation(m, "Setter");
			JavaAnnotation anAdder = UtilPAMELA.getAnnotation(m, "Adder");
			JavaAnnotation aRemover = UtilPAMELA.getAnnotation(m, "Remover");

			if (aGetter == null && aSetter == null && anAdder == null && aRemover == null) {
				// TODO
			}

			// getter exist and identifier corresponding
			if (aGetter != null && aGetter.getNamedParameter("value").toString().equals(propertyIdentifier)) {
				if (getter != null) {
					throw new ModelDefinitionException("Duplicate getter '" + propertyIdentifier
							+ "' defined for interface " + pamelaEntity.getName());
				} else {
					beginLine = java.lang.Math.min(beginLine, Location.getMethodBeginLine(m));
					endLine = java.lang.Math.max(beginLine, Location.getMethodLastLine(m));
					getter = m;
				}
			}
			// setter exist and identifier corresponding
			if (aSetter != null && aSetter.getNamedParameter("value").toString().equals(propertyIdentifier)) {
				if (setter != null) {
					throw new ModelDefinitionException("Duplicate setter '" + propertyIdentifier
							+ "' defined for interface " + pamelaEntity.getName());
				} else {
					beginLine = java.lang.Math.min(beginLine, Location.getMethodBeginLine(m));
					endLine = java.lang.Math.max(beginLine, Location.getMethodLastLine(m));
					setter = m;
				}
			}
			// adder exist and identifier corresponding
			if (anAdder != null && anAdder.getNamedParameter("value").toString().equals(propertyIdentifier)) {
				if (adder != null) {
					throw new ModelDefinitionException("Duplicate adder '" + propertyIdentifier
							+ "' defined for interface " + pamelaEntity.getName());
				} else {
					beginLine = java.lang.Math.min(beginLine, Location.getMethodBeginLine(m));
					endLine = java.lang.Math.max(beginLine, Location.getMethodLastLine(m));
					adder = m;
				}
			}
			// remover exist and identifier corresponding
			if (aRemover != null && aRemover.getNamedParameter("value").toString().equals(propertyIdentifier)) {
				if (remover != null) {
					throw new ModelDefinitionException("Duplicate remover '" + propertyIdentifier
							+ "' defined for interface " + pamelaEntity.getName());
				} else {
					beginLine = java.lang.Math.min(beginLine, Location.getMethodBeginLine(m));
					endLine = java.lang.Math.max(beginLine, Location.getMethodLastLine(m));
					remover = m;
				}
			}

		}

		return new PAMELAProperty(propertyIdentifier, pamelaEntity, getter, setter, adder, remover, beginLine, endLine);
	}

	/**
	 * create a pamelaProperty from loading java sources
	 * 
	 * @param identifier
	 * @param pAMELAEntity
	 * @param getter
	 * @param setter
	 * @param adder
	 * @param remover
	 * @param beginLine
	 * @param endLine
	 */
	public PAMELAProperty(String identifier, org.openflexo.pamela.editor.editer.PAMELAEntity pAMELAEntity,
			JavaMethod getter, JavaMethod setter, JavaMethod adder, JavaMethod remover, int beginLine, int endLine) {
		this.identifier = identifier;
		this.pamelaEntity = pAMELAEntity;
		if (getter != null)
			this.getter = new EditableMethod(getter);
		if (setter != null)
			this.setter = new EditableMethod(setter);
		if (adder != null)
			this.adder = new EditableMethod(adder);
		if (remover != null)
			this.remover = new EditableMethod(remover);
		this.begin = beginLine;
		this.end = endLine;

		if (getter != null) {
			setTypeAndKeyType();
		}
	}

	/**
	 * use for user create a new property auto cover the identifier to upper
	 * case
	 * 
	 * @param identifier
	 * @param cardinality
	 * @param keyType
	 * @param type
	 */
	public PAMELAProperty(String identifier, Cardinality cardinality, JavaType keyType, JavaType type) {
		identifier = identifier.toUpperCase();
		this.keyType = keyType;
		this.type = type;
		this.identifier = identifier;
		this.cardinality = cardinality;

		// TODO initial PamelapProperty with these parameters
		// -> decide the type according to the cardinality
		// -> bind the embeded type from the EntityLibray

		try {
			init();
		} catch (PropertyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * initialize a PamelaProperty when user create a property by the
	 * constructor PAMELAProperty: 1.initialize the default getter and setter
	 * method with annotation 2. set the ignoreType, when the new property is an
	 * embedded entity
	 * 
	 * @throws PropertyException
	 */
	void init() throws PropertyException {
		// if the cardinality is not map, set the keyType = null
		switch (cardinality) {
		case SINGLE:
		case LIST:
			this.keyType = null;
			break;

		case MAP:
			break;
		}

		setGetter(new EditableMethod("get" + identifier));
		setSetter(new EditableMethod("set" + identifier));

		// if this javaType is embeddedType, find it in EntityLibrary, if not
		// exist -> set the ignoreType = true
		if (!TypeConverterLibrary.getInstance().hasConverter(type.getFullyQualifiedName())) {
			if (PAMELAEntityLibrary.get(type.getFullyQualifiedName()) == null) {
				setIgnoreType(true);
			}
		}

	}

	/**
	 * get Cardinality, if the cardinality not initialized,
	 * 
	 * @return
	 */
	public Cardinality getCardinality() {
		// load cardinality from javaMethod
		if (cardinality == null && getter != null) {
			Object aCardinality = UtilPAMELA.getAnnotation(getter.getJavaMethod(), "Getter")
					.getNamedParameter("cardinality");
			if (aCardinality != null) {
				cardinality = strToCaedinality((String) aCardinality);
			} else {
				cardinality = Cardinality.SINGLE;
			}
		}
		return cardinality;
	}

	private Cardinality strToCaedinality(String str) {
		Cardinality enumCardi = Cardinality.SINGLE;
		if (str.equals("Cardinality.LIST")) {
			enumCardi = Cardinality.LIST;
		}
		if (str.equals("Cardinality.MAP")) {
			enumCardi = Cardinality.MAP;
		}

		return enumCardi;

	}

	public JavaType getType() {
		return type;
	}

	public EditableMethod getGetter() {
		return getter;
	}

	public EditableMethod getSetter() {
		return setter;
	}

	public EditableMethod getAdder() {
		return adder;
	}

	public EditableMethod getRemover() {
		return remover;
	}

	public String getIdentifier() {
		return identifier;
	}

	/**
	 * set getter method and add anootation TODO support params in annotation in
	 * the future
	 * 
	 * @param getter
	 */
	public void setGetter(EditableMethod getter) {
		if (getter.getAnnotationByName("Getter") == null) {
			// add initial @Getter annotation
			HashMap<String, String> initAnnotations = new HashMap<String, String>();
			initAnnotations.put("value", identifier);
			getter.setAnnotation("Getter", initAnnotations);
		}
		this.getter = getter;
	}

	public void setSetter(EditableMethod setter) {
		if (getter.getAnnotationByName("Setter") == null) {
			HashMap<String, String> initAnnotations = new HashMap<String, String>();
			initAnnotations.put("value", identifier);
			setter.setAnnotation("Setter", initAnnotations);
		}
		this.setter = setter;
	}

	public void setAdder(EditableMethod adder) {
		if (getter.getAnnotationByName("Adder") == null) {
			HashMap<String, String> initAnnotations = new HashMap<String, String>();
			initAnnotations.put("value", identifier);
			adder.setAnnotation("Adder", initAnnotations);
		}
		this.adder = adder;
	}

	public void setRemover(EditableMethod remover) {
		if (getter.getAnnotationByName("Remover") == null) {
			HashMap<String, String> initAnnotations = new HashMap<String, String>();
			initAnnotations.put("value", identifier);
			remover.setAnnotation("Remover", initAnnotations);
		}
		this.remover = remover;
	}

	public PAMELAEntity getPamelaEntity() {
		return pamelaEntity;
	}

	public void setPamelaEntity(PAMELAEntity pamelaEntity) {
		this.pamelaEntity = pamelaEntity;
	}

	/**
	 * used for loading from the java files, to set the type and keyType
	 * corresponding the cardinality
	 */
	private void setTypeAndKeyType() {

		switch (getCardinality()) {
		case SINGLE:
			type = getter.getJavaMethod().getReturnType();
			break;
		case LIST:
			// type is the parameterizedType of the List
			type = ((JavaParameterizedType) getter.getJavaMethod().getReturnType()).getActualTypeArguments().get(0);
			break;
		case MAP:
			keyType = ((JavaParameterizedType) getter.getJavaMethod().getReturnType()).getActualTypeArguments().get(0);
			type = ((JavaParameterizedType) getter.getJavaMethod().getReturnType()).getActualTypeArguments().get(1);
			break;
		}

		// TODO update related method

	}

	/**
	 * to know if the ignoreType is true or false in @Getter
	 * 
	 * @return
	 */
	public boolean ignoreType() {
		if (getGetter() != null) {
			String ignoreValue = getGetter().getAnnotationParam("Getter", "ignoreType");
			if (ignoreValue != null && ignoreValue.equals("true"))
				return true;
		}
		return false;
	}

	/**
	 * set the ignoreType and modify the Getter annotation in EditableMethod
	 * 
	 * @param b
	 * @return
	 */
	public boolean setIgnoreType(boolean b) {
		if (getGetter() != null) {
			Map<String, String> aGetter = getter.getAnnotationByName("Getter");
			aGetter.put("ignoreType", java.lang.Boolean.toString(b));
			System.out.println(getter.toString());
			return true;
		}
		return false;
	}

	public int getBegin() {
		return begin;
	}

	public int getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return "\n id:" + identifier + "\n" + cardinality + " type:" + type.getCanonicalName() + "\n"
				+ (getter != null ? getter.getJavaMethod().getCodeBlock() : "") + "\n"
				+ (setter != null ? setter.getJavaMethod().getCodeBlock() : "") + "\n"
				+ (adder != null ? adder.getJavaMethod().getCodeBlock() : "") + "\n"
				+ (remover != null ? remover.getJavaMethod().getCodeBlock() : "");
	}

}
