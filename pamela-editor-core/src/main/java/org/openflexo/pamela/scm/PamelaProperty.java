package org.openflexo.pamela.scm;

import static org.openflexo.pamela.scm.util.Util.addAnnotation;
import static org.openflexo.pamela.scm.util.Util.getAnnotation;
import static org.openflexo.pamela.scm.util.Util.hasAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.CloningStrategy;
import org.openflexo.pamela.annotations.Embedded;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.PastingPoint;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.ReturnedValue;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.XMLAttribute;
import org.openflexo.pamela.annotations.XMLElement;
import org.openflexo.pamela.scm.annotations.AdderAnnotation;
import org.openflexo.pamela.scm.annotations.CloningStrategyAnnotation;
import org.openflexo.pamela.scm.annotations.EmbeddedAnnotation;
import org.openflexo.pamela.scm.annotations.GetterAnnotation;
import org.openflexo.pamela.scm.annotations.PastingPointAnnotation;
import org.openflexo.pamela.scm.annotations.RemoverAnnotation;
import org.openflexo.pamela.scm.annotations.ReturnedValueAnnotation;
import org.openflexo.pamela.scm.annotations.SetterAnnotation;
import org.openflexo.pamela.scm.annotations.XMLAttributeAnnotation;
import org.openflexo.pamela.scm.annotations.XMLElementAnnotation;

import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.impl.DefaultJavaClass;
import com.thoughtworks.qdox.model.impl.DefaultJavaMethod;
import com.thoughtworks.qdox.model.impl.DefaultJavaParameter;

/**
 * Created by adria on 07/12/2016.
 */
public class PamelaProperty {

	private JavaClass containingInterface;
	private String identifier;

	/* Getter Annotations */
	private GetterAnnotation getter;
	private ReturnedValueAnnotation returnedValue;
	private EmbeddedAnnotation embedded;
	private CloningStrategyAnnotation cloningStrategy;
	private XMLAttributeAnnotation xmlAttribute;
	private XMLElementAnnotation xmlElement;

	/* Setter Annotations */
	private SetterAnnotation setter;
	private PastingPointAnnotation setterPastingPoint;

	/* Adder Annotations */
	private AdderAnnotation adder;
	private PastingPointAnnotation adderPastingPoint;

	private RemoverAnnotation remover;

	private JavaMethod getterMethod;
	private JavaMethod setterMethod;
	private JavaMethod adderMethod;
	private JavaMethod removerMethod;

	private JavaClass type;

	/**
	 * Creates the PamelaProperty instance of an existing property.
	 *
	 * @param identifier
	 * @param containingInterface
	 */
	protected PamelaProperty(String identifier, JavaClass containingInterface) {
		this.identifier = identifier;
		this.containingInterface = containingInterface;

		initializeGetter();
		initializeSetter();
		initializeAdder();
		initializeRemover();
	}

	/**
	 * Creates the PamelaProperty instance of a new property.
	 *
	 * @param identifier
	 * @param type
	 * @param containingInterface
	 */
	protected PamelaProperty(String identifier, JavaClass type, JavaClass containingInterface) {
		this.identifier = identifier;
		this.containingInterface = containingInterface;
		this.type = type;
	}

	private void initializeGetter() {
		List<JavaMethod> methods = containingInterface.getMethods().stream().filter(method -> hasAnnotation(method, Getter.class))
				.collect(Collectors.toList());
		methods.stream().filter(
				method -> Objects.equals(getAnnotation(method, Getter.class).getNamedParameter("value").toString().trim(), identifier))
				.findFirst().ifPresent(method -> {
					getterMethod = method;
					getter = new GetterAnnotation(getAnnotation(getterMethod, Getter.class));
					type = getterMethod.getReturns();
					if (hasAnnotation(getterMethod, ReturnedValue.class))
						returnedValue = new ReturnedValueAnnotation(getAnnotation(getterMethod, ReturnedValue.class));
					if (hasAnnotation(getterMethod, Embedded.class))
						embedded = new EmbeddedAnnotation(getAnnotation(getterMethod, Embedded.class));
					if (hasAnnotation(getterMethod, CloningStrategy.class))
						cloningStrategy = new CloningStrategyAnnotation(getAnnotation(getterMethod, CloningStrategy.class));
					if (hasAnnotation(getterMethod, XMLAttribute.class))
						xmlAttribute = new XMLAttributeAnnotation(getAnnotation(getterMethod, XMLAttribute.class));
					if (hasAnnotation(getterMethod, XMLElement.class))
						xmlElement = new XMLElementAnnotation(getAnnotation(getterMethod, XMLElement.class));
				});
	}

	private void initializeSetter() {
		List<JavaMethod> methods = containingInterface.getMethods().stream().filter(method -> hasAnnotation(method, Setter.class))
				.collect(Collectors.toList());
		methods.stream().filter(
				method -> Objects.equals(getAnnotation(method, Setter.class).getNamedParameter("value").toString().trim(), identifier))
				.findFirst().ifPresent(method -> {
					setterMethod = method;
					setter = new SetterAnnotation(getAnnotation(setterMethod, Setter.class));
					if (hasAnnotation(setterMethod, PastingPoint.class))
						setterPastingPoint = new PastingPointAnnotation(getAnnotation(setterMethod, PastingPoint.class));
				});
	}

	private void initializeAdder() {
		List<JavaMethod> methods = containingInterface.getMethods().stream().filter(method -> hasAnnotation(method, Adder.class))
				.collect(Collectors.toList());
		methods.stream().filter(
				method -> Objects.equals(getAnnotation(method, Adder.class).getNamedParameter("value").toString().trim(), identifier))
				.findFirst().ifPresent(method -> {
					adderMethod = method;
					adder = new AdderAnnotation(getAnnotation(adderMethod, Adder.class));
					if (hasAnnotation(adderMethod, PastingPoint.class))
						adderPastingPoint = new PastingPointAnnotation(getAnnotation(adderMethod, PastingPoint.class));
				});
	}

	private void initializeRemover() {
		List<JavaMethod> methods = containingInterface.getMethods().stream().filter(method -> hasAnnotation(method, Remover.class))
				.collect(Collectors.toList());
		methods.stream()
				.filter(method -> Objects.equals(getAnnotation(method, Remover.class).getNamedParameter("value").toString(), identifier))
				.findFirst().ifPresent(method -> {
					removerMethod = method;
					remover = new RemoverAnnotation(getAnnotation(removerMethod, Remover.class));
				});
	}

	public String getIdentifier() {
		return identifier;
	}

	public GetterAnnotation getGetter() {
		return getter;
	}

	public SetterAnnotation getSetter() {
		return setter;
	}

	public AdderAnnotation getAdder() {
		return adder;
	}

	public RemoverAnnotation getRemover() {
		return remover;
	}

	protected JavaMethod getGetterMethod() {
		return getterMethod;
	}

	protected JavaMethod getSetterMethod() {
		return setterMethod;
	}

	protected JavaMethod getAdderMethod() {
		return adderMethod;
	}

	protected JavaMethod getRemoverMethod() {
		return removerMethod;
	}

	public SetterAnnotation addSetter() {
		DefaultJavaMethod setterMethod = new DefaultJavaMethod(new DefaultJavaClass("void"), "set" + identifier);
		ArrayList<JavaParameter> parameters = new ArrayList<>();
		parameters.add(new DefaultJavaParameter(type, "value"));
		setterMethod.setParameters(parameters);
		setter = new SetterAnnotation(addAnnotation(setterMethod, Setter.class));
		setter.setValue(identifier);
		this.setterMethod = setterMethod;
		containingInterface.getMethods().add(setterMethod);
		return setter;
	}

	public PamelaProperty removeSetter() {
		if (setterMethod == null)
			return this;
		containingInterface.getMethods().remove(setterMethod);
		setter = null;
		setterMethod = null;
		return this;
	}

	public AdderAnnotation addAdder(JavaClass parameterType) {
		DefaultJavaMethod adderMethod = new DefaultJavaMethod(new DefaultJavaClass("void"), "addTo" + identifier);
		ArrayList<JavaParameter> parameters = new ArrayList<>();
		parameters.add(new DefaultJavaParameter(parameterType, "value"));
		adderMethod.setParameters(parameters);
		adder = new AdderAnnotation(addAnnotation(adderMethod, Adder.class));
		adder.setValue(identifier);
		this.adderMethod = adderMethod;
		containingInterface.getMethods().add(adderMethod);
		return adder;
	}

	public PamelaProperty removeAdder() {
		if (adderMethod == null)
			return this;
		containingInterface.getMethods().remove(adderMethod);
		adder = null;
		adderMethod = null;
		return this;
	}

	public RemoverAnnotation addRemover(JavaClass parameterType) {
		DefaultJavaMethod removerMethod = new DefaultJavaMethod(new DefaultJavaClass("void"), "removeFrom" + identifier);
		ArrayList<JavaParameter> parameters = new ArrayList<>();
		parameters.add(new DefaultJavaParameter(parameterType, "value"));
		removerMethod.setParameters(parameters);
		remover = new RemoverAnnotation(addAnnotation(removerMethod, Remover.class));
		remover.setValue(identifier);
		containingInterface.getMethods().add(removerMethod);
		return remover;
	}

	public PamelaProperty removeRemover() {
		if (removerMethod == null)
			return this;
		containingInterface.getMethods().remove(removerMethod);
		remover = null;
		removerMethod = null;
		return this;
	}

	public GetterAnnotation addGetter() {
		getterMethod = new DefaultJavaMethod(type, "get" + identifier);
		getter = new GetterAnnotation(addAnnotation(getterMethod, Getter.class));
		getter.setValue(identifier);
		containingInterface.getMethods().add(getterMethod);
		return getter;
	}

	public PamelaProperty removeGetter() {
		if (getterMethod == null)
			return this;
		containingInterface.getMethods().remove(getterMethod);
		getter = null;
		getterMethod = null;
		return this;
	}

	public EmbeddedAnnotation addEmbedded() {
		embedded = new EmbeddedAnnotation(addAnnotation(getterMethod, Embedded.class));
		return embedded;
	}

	public PamelaProperty removeEmbedded() {
		getterMethod.getAnnotations().remove(embedded.getAnnotationSource());
		embedded = null;
		return this;
	}

	public EmbeddedAnnotation getEmbedded() {
		return embedded;
	}

	public boolean isEmbedded() {
		return hasAnnotation(getterMethod, Embedded.class);
	}

	public ReturnedValueAnnotation addReturnedValue() {
		returnedValue = new ReturnedValueAnnotation(addAnnotation(getterMethod, ReturnedValue.class));
		return returnedValue;
	}

	public PamelaProperty removeReturnedValue() {
		getterMethod.getAnnotations().remove(returnedValue.getAnnotationSource());
		returnedValue = null;
		return this;
	}

	public ReturnedValueAnnotation getReturnedValue() {
		return returnedValue;
	}

	public CloningStrategyAnnotation getCloningStrategy() {
		return cloningStrategy;
	}

	public CloningStrategyAnnotation addCloningStrategy() {
		cloningStrategy = new CloningStrategyAnnotation(addAnnotation(getterMethod, CloningStrategy.class));
		return cloningStrategy;
	}

	public PamelaProperty removeCloningStrategy() {
		getterMethod.getAnnotations().remove(cloningStrategy.getAnnotationSource());
		cloningStrategy = null;
		return this;
	}

	public XMLAttributeAnnotation getXmlAttribute() {
		return xmlAttribute;
	}

	public XMLAttributeAnnotation addXmlAttribute() {
		xmlAttribute = new XMLAttributeAnnotation(addAnnotation(getterMethod, XMLAttribute.class));
		return xmlAttribute;
	}

	public PamelaProperty removeXmlAttribute() {
		getterMethod.getAnnotations().remove(xmlAttribute.getAnnotationSource());
		xmlAttribute = null;
		return this;
	}

	public XMLElementAnnotation getXmlElement() {
		return xmlElement;
	}

	public XMLElementAnnotation addXmlElement() {
		xmlElement = new XMLElementAnnotation(addAnnotation(getterMethod, XMLElement.class));
		return xmlElement;
	}

	public PamelaProperty removeXmlElement() {
		getterMethod.getAnnotations().remove(xmlElement.getAnnotationSource());
		xmlElement = null;
		return this;
	}

	public PastingPointAnnotation getSetterPastingPoint() {
		return setterPastingPoint;
	}

	public PastingPointAnnotation addSetterPastingPoint() {
		setterPastingPoint = new PastingPointAnnotation(addAnnotation(setterMethod, PastingPoint.class));
		return setterPastingPoint;
	}

	public PamelaProperty removeSetterPastingPoint() {
		setterMethod.getAnnotations().remove(setterPastingPoint.getAnnotationSource());
		setterPastingPoint = null;
		return this;
	}

	public PastingPointAnnotation getAdderPastingPoint() {
		return adderPastingPoint;
	}

	public PastingPointAnnotation addAdderPastingPoint() {
		adderPastingPoint = new PastingPointAnnotation(addAnnotation(adderMethod, PastingPoint.class));
		return adderPastingPoint;
	}

	public PamelaProperty removeAdderPastingPoint() {
		adderMethod.getAnnotations().remove(adderPastingPoint.getAnnotationSource());
		adderPastingPoint = null;
		return this;
	}
}
