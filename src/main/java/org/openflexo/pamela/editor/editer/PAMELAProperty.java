package org.openflexo.pamela.editor.editer;

import java.util.Map;

import org.openflexo.model.exceptions.ModelDefinitionException;
import org.openflexo.pamela.editor.annotations.AnnotationA;
import org.openflexo.pamela.editor.annotations.GetterA;
import org.openflexo.pamela.editor.editer.utils.UtilPAMELA;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameterizedType;
import com.thoughtworks.qdox.model.JavaType;

public class PAMELAProperty {

	public enum Cardinality {
		SINGLE, LIST, MAP
	};

	private String identifier;

	private PAMELAEntity PAMELAEntity;
	/**
	 * A property can be a single, a List or a Map
	 */
	private Cardinality cardinality;

	private JavaType type;
	private JavaType keyType;

	private JavaMethod getter;
	private JavaMethod setter;
	private JavaMethod adder;
	private JavaMethod remover;
	

	public static PAMELAProperty getPAMELAproperty(String propertyIdentifier, PAMELAEntity pamelaEntity)
			throws ModelDefinitionException {
		JavaMethod getter = null;
		JavaMethod setter = null;
		JavaMethod adder = null;
		JavaMethod remover = null;

		JavaClass inplementedInterface = pamelaEntity.getInplementedInterface();
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

					getter = m;
				}
			}
			// setter exist and identifier corresponding
			if (aSetter != null && aSetter.getNamedParameter("value").toString().equals(propertyIdentifier)) {
				if (setter != null) {
					throw new ModelDefinitionException("Duplicate setter '" + propertyIdentifier
							+ "' defined for interface " + pamelaEntity.getName());
				} else {

					setter = m;
				}
			}
			// adder exist and identifier corresponding
			if (anAdder != null && anAdder.getNamedParameter("value").toString().equals(propertyIdentifier)) {
				if (adder != null) {
					throw new ModelDefinitionException("Duplicate setter '" + propertyIdentifier
							+ "' defined for interface " + pamelaEntity.getName());
				} else {

					adder = m;
				}
			}
			// remover exist and identifier corresponding
			if (aRemover != null && aRemover.getNamedParameter("value").toString().equals(propertyIdentifier)) {
				if (remover != null) {
					throw new ModelDefinitionException("Duplicate setter '" + propertyIdentifier
							+ "' defined for interface " + pamelaEntity.getName());
				} else {

					remover = m;
				}
			}

		}

		return new PAMELAProperty(propertyIdentifier, pamelaEntity, getter, setter, adder, remover);
	}

	public PAMELAProperty(String identifier, org.openflexo.pamela.editor.editer.PAMELAEntity pAMELAEntity, JavaMethod getter,
			JavaMethod setter, JavaMethod adder, JavaMethod remover) {
		this.identifier = identifier;
		this.PAMELAEntity = pAMELAEntity;
		this.getter = getter;
		this.setter = setter;
		this.adder = adder;
		this.remover = remover;

		if (getter != null) {
			setTypeAndKeyType();
		}
	}

	public Cardinality getCardinality() {
		if (cardinality == null && getter != null) {
			Object aCardinality = UtilPAMELA.getAnnotation(getter, "Getter").getNamedParameter("cardinality");
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

	public JavaMethod getGetter() {
		return getter;
	}

	public JavaMethod getSetter() {
		return setter;
	}

	public JavaMethod getAdder() {
		return adder;
	}

	public JavaMethod getRemover() {
		return remover;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setGetter(JavaMethod getter) {
		this.getter = getter;
	}

	public void setSetter(JavaMethod setter) {
		this.setter = setter;
	}

	public void setAdder(JavaMethod adder) {
		this.adder = adder;
	}

	public void setRemover(JavaMethod remover) {
		this.remover = remover;
	}

	// -----------------code accordant with
	// gui----------------------------------------------

	public void setGetter(Map<String, AnnotationA> annotations) {

		// annotation for getter
		GetterA gA = (GetterA) annotations.get("Getter");
		this.cardinality = gA.getCardinality();

		// create annotations source
		StringBuilder sb = new StringBuilder();
		sb.append(gA.toString()).append("\n");
		// TODO

		// keep the annotations of this getMethod except Getter
		// TODO
		for (JavaAnnotation ja : getter.getAnnotations()) {
			if (!ja.getType().getValue().equals("Getter"))
				sb.append(ja).append("\n");
		}

		// create method source
		switch (this.cardinality) {
		case SINGLE:
			sb.append(gA.getType());
			break;
		case LIST:
			sb.append("List<").append(gA.getType()).append(">");
			break;
		case MAP:
			sb.append("Map<").append(gA.getKeyType()).append(",").append(gA.getType()).append(">");
			break;
		}

		sb.append(" ");

		// name
		sb.append("get").append(identifier).append(" ( ); ");

		// jaGetter.getNamedParameter(key)
		System.out.println(sb);

		// build method
		JavaMethod newGetter = UtilPAMELA.buildMethod(sb.toString());
		this.getter = newGetter;

		// update Type
		updateMethod();

	}

	private void setTypeAndKeyType() {

		switch (getCardinality()) {
		case SINGLE:
			type = getter.getReturnType();
			break;
		case LIST:
			// type is the parameterizedType of the List
			type = ((JavaParameterizedType) getter.getReturnType()).getActualTypeArguments().get(0);

			break;
		case MAP:
			keyType = ((JavaParameterizedType) getter.getReturnType()).getActualTypeArguments().get(0);
			type = ((JavaParameterizedType) getter.getReturnType()).getActualTypeArguments().get(1);

			break;
		}

		// update related method

	}

	//update the setter adder remover method by the new type 
	private void updateMethod() {
		setTypeAndKeyType();	
		String targetType = null;
		StringBuilder sb;
		String str2;
		// update setter method
		if (setter != null) {
			sb = new StringBuilder();
			for (JavaAnnotation ja : setter.getAnnotations()) {
				sb.append(ja).append("\n");
			}
			switch (getCardinality()) {
			case SINGLE:
				targetType = type.getGenericCanonicalName();
				break;
			case LIST:
				targetType = String.format("List<%s>", type.getGenericCanonicalName());
				break;
			case MAP:
				targetType = String.format("Map<%s,%s>", keyType.getGenericCanonicalName(),
						type.getGenericCanonicalName());
			}

			str2 = String.format("void %s(%s %s);", setter.getName(), targetType,
					setter.getParameters().get(0).getName());
			sb.append(str2);
			// System.out.println("update====\n"+sb);
			setter = UtilPAMELA.buildMethod(sb.toString());
		}
		// adder ???????????????????????????????/
		if (adder != null && cardinality != Cardinality.SINGLE) {
			sb = new StringBuilder();
			for (JavaAnnotation ja : adder.getAnnotations()) {
				sb.append(ja).append("\n");
			}

			if (cardinality == Cardinality.LIST) {
				str2 = String.format("void %s(%s %s);", adder.getName(), targetType, "value");
			} else {
				str2 = String.format("void %s(%s %s,$s %s);", adder.getName(), keyType.getGenericCanonicalName(), "key",
						type.getCanonicalName(), "value");
			}

			sb.append(str2);
			adder = UtilPAMELA.buildMethod(sb.toString());
		}
		// remover
		// TODO
		if (remover != null && cardinality != Cardinality.SINGLE) {
			sb = new StringBuilder();
			for (JavaAnnotation ja : remover.getAnnotations()) {
				sb.append(ja).append("\n");
			}

			if (cardinality == Cardinality.LIST) {
				str2 = String.format("void %s(%s %s);", remover.getName(), type, "value");
			} else {
				// ?????
				str2 = String.format("void %s(%s %s);", remover.getName(), keyType.getGenericCanonicalName(), "key");
			}

		}

	}

	@Override
	public String toString() {
		return "\n id:" + identifier + "\n" + cardinality + " type:" + type.getCanonicalName() + "\n"
				+ (getter != null ? getter.getCodeBlock() : "") + "\n" + (setter != null ? setter.getCodeBlock() : "")
				+ "\n" + (adder != null ? adder.getCodeBlock() : "") + "\n"
				+ (remover != null ? remover.getCodeBlock() : "");
	}

}
