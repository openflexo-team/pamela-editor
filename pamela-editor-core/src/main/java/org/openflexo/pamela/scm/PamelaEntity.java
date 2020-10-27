package org.openflexo.pamela.scm;

import static org.openflexo.pamela.scm.util.Util.addAnnotation;
import static org.openflexo.pamela.scm.util.Util.getAnnotation;
import static org.openflexo.pamela.scm.util.Util.hasAnnotation;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.Imports;
import org.openflexo.pamela.annotations.Initializer;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Modify;
import org.openflexo.pamela.annotations.XMLElement;
import org.openflexo.pamela.scm.annotations.DeserializationFinalizerAnnotation;
import org.openflexo.pamela.scm.annotations.DeserializationInitializerAnnotation;
import org.openflexo.pamela.scm.annotations.ImplementationClassAnnotation;
import org.openflexo.pamela.scm.annotations.ImportsAnnotation;
import org.openflexo.pamela.scm.annotations.ModelEntityAnnotation;
import org.openflexo.pamela.scm.annotations.ModifyAnnotation;
import org.openflexo.pamela.scm.annotations.XMLElementAnnotation;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.impl.DefaultJavaClass;
import com.thoughtworks.qdox.model.impl.DefaultJavaField;
import com.thoughtworks.qdox.model.impl.DefaultJavaMethod;

/**
 * Created by adria on 11/12/2016.
 */
public class PamelaEntity {

	/* Interface containing this PamelaEntity */
	private JavaClass containingInterface;

	private List<PamelaProperty> properties;

	/* PamelaEntity Annotations */
	private ModelEntityAnnotation entityAnnotation;
	private ImplementationClassAnnotation implementationClass;
	private ImportsAnnotation imports;
	private ModifyAnnotation modify;
	private XMLElementAnnotation xmlElement;

	private DeserializationInitializerAnnotation deserializationInitializer;
	private DeserializationFinalizerAnnotation deserializationFinalizer;

	private List<PamelaEntity> superEntities;
	private List<PamelaEntity> referencedEntities;

	public static boolean isPamelaEntity(JavaClass javaClass) {
		boolean isPamelaEntity = javaClass.getAnnotations().stream()
				.anyMatch(annotation -> Objects.equals(annotation.getType().getCanonicalName(), ModelEntity.class.getCanonicalName()));
		return javaClass.isInterface() && isPamelaEntity;
	}

	public PamelaEntity(@Nonnull JavaClass containingInterface) {
		superEntities = new ArrayList<>();
		referencedEntities = new ArrayList<>();
		properties = new ArrayList<>();
		this.containingInterface = containingInterface;

		initializeAnnotations();
		for (JavaField field : containingInterface.getFields()) {
			String identifier = field.getName();
			properties.add(new PamelaProperty(identifier, containingInterface));
		}
	}

	public void updateWith(@Nonnull JavaClass containingInterface) {
		System.out.println("Tiens faut faire l'update de " + containingInterface.getCanonicalName());
	}

	private void initializeAnnotations() {
		if (hasAnnotation(containingInterface, ModelEntity.class))
			entityAnnotation = new ModelEntityAnnotation(getAnnotation(containingInterface, ModelEntity.class));
		if (hasAnnotation(containingInterface, ImplementationClass.class))
			implementationClass = new ImplementationClassAnnotation(getAnnotation(containingInterface, ImplementationClass.class));
		if (hasAnnotation(containingInterface, Imports.class))
			imports = new ImportsAnnotation(getAnnotation(containingInterface, Imports.class));
		if (hasAnnotation(containingInterface, Modify.class))
			modify = new ModifyAnnotation(getAnnotation(containingInterface, Modify.class));
		if (hasAnnotation(containingInterface, XMLElement.class))
			xmlElement = new XMLElementAnnotation(getAnnotation(containingInterface, XMLElement.class));
	}

	public void resolveDependencies(Collection<PamelaEntity> context) {
		resolveImports(context);
		resolveInheritance(context);
		resolveEmbeddedEntities(context);
	}

	public List<PamelaEntity> getSuperEntities() {
		return superEntities;
	}

	public List<PamelaEntity> getReferencedEntities() {
		return referencedEntities;
	}

	public void resolveInheritance(Collection<PamelaEntity> context) {
		for (JavaClass superClass : containingInterface.getInterfaces()) {
			context.stream()
					.filter(entity -> Objects.equals(entity.getContainingInterface().getCanonicalName(), superClass.getCanonicalName()))
					.findFirst().ifPresent(entity -> superEntities.add(entity));
		}

	}

	public void resolveImports(Collection<PamelaEntity> context) {
		if (imports == null)
			return;
		for (String className : imports.getValue()) {
			Optional<PamelaEntity> entity = context.stream().filter(ent -> Objects.equals(ent.getCanonicalName(), className)).findFirst();
			if (entity.isPresent())
				referencedEntities.add(entity.get());
			else
				System.err.println("Referenced import entity " + className + " is missing.");
		}
	}

	private void resolveEmbeddedEntities(Collection<PamelaEntity> context) {
		for (JavaMethod method : containingInterface.getMethods()) {
			String returnType = method.getReturnType().getCanonicalName();
			context.stream().filter(entity -> Objects.equals(entity.getCanonicalName(), returnType)).findFirst()
					.ifPresent(entity -> referencedEntities.add(entity));
		}
	}

	public String getCanonicalName() {
		return containingInterface.getCanonicalName();
	}

	public JavaClass getContainingInterface() {
		return containingInterface;
	}

	private File sourceFile = null;

	public File getSourceFile() {
		if (sourceFile == null) {
			try {
				sourceFile = Paths.get(containingInterface.getSource().getURL().toURI()).toFile();
			} catch (URISyntaxException e) {
				System.err.println("URISyntaxException: " + containingInterface.getSource().getURL());
			}
		}
		return sourceFile;
	}

	/**
	 * Adds a new PamelaProperty in this entity, and returns the instance.
	 *
	 * @param identifier
	 *            identifier for the property.
	 * @param returnType
	 *            type of the property.
	 * @return the new property.
	 */
	public PamelaProperty addProperty(String identifier, JavaClass returnType) {
		PamelaProperty property = new PamelaProperty(identifier, returnType, containingInterface);
		DefaultJavaField field = new DefaultJavaField(new DefaultJavaClass("String"), property.getIdentifier());
		field.setInitializationExpression("\"" + property.getIdentifier() + "\"");
		containingInterface.getFields().add(field);
		if (property.getGetterMethod() != null)
			containingInterface.getMethods().add(property.getGetterMethod());
		if (property.getSetterMethod() != null)
			containingInterface.getMethods().add(property.getSetterMethod());
		if (property.getAdderMethod() != null)
			containingInterface.getMethods().add(property.getAdderMethod());
		if (property.getRemoverMethod() != null)
			containingInterface.getMethods().add(property.getRemoverMethod());
		properties.add(property);
		return property;
	}

	/**
	 * Removes the specified property.
	 *
	 * @param identifier
	 *            the identifier of the property to be removed.
	 */
	public void removeProperty(String identifier) {
		Optional<PamelaProperty> optProperty = properties.stream().filter(x -> Objects.equals(x.getIdentifier(), identifier)).findFirst();
		if (!optProperty.isPresent())
			return;
		PamelaProperty property = optProperty.get();
		containingInterface.getFields().remove(containingInterface.getFieldByName(property.getIdentifier()));
		if (property.getGetterMethod() != null)
			containingInterface.getMethods().remove(property.getGetterMethod());
		if (property.getSetterMethod() != null)
			containingInterface.getMethods().remove(property.getSetterMethod());
		if (property.getAdderMethod() != null)
			containingInterface.getMethods().remove(property.getAdderMethod());
		if (property.getRemoverMethod() != null)
			containingInterface.getMethods().remove(property.getRemoverMethod());
		properties.remove(property);
	}

	/**
	 * Returns the specified property
	 *
	 * @param identifier
	 *            the identifier of the property.
	 * @return the instance of the wanted property.
	 */
	public PamelaProperty getProperty(String identifier) {
		for (PamelaProperty property : properties) {
			if (Objects.equals(identifier, property.getIdentifier())) {
				return property;
			}
		}
		return null;
	}

	/**
	 * @return the list of all properties of this entity
	 */
	public List<PamelaProperty> getProperties() {
		return properties;
	}

	public ImplementationClassAnnotation getImplementationClass() {
		return implementationClass;
	}

	public ImplementationClassAnnotation addImplementationClass() {
		implementationClass = new ImplementationClassAnnotation(addAnnotation(containingInterface, ImplementationClass.class));
		return implementationClass;
	}

	public void removeImplementationClass() {
		containingInterface.getAnnotations().remove(implementationClass.getAnnotationSource());
		implementationClass = null;
	}

	public ImportsAnnotation addImports() {
		JavaAnnotation annotation = addAnnotation(containingInterface, Imports.class);
		this.imports = new ImportsAnnotation(annotation);
		return imports;
	}

	public ImportsAnnotation getImportsAnnotation() {
		return imports;
	}

	public void removeImportsAnnotation() {
		containingInterface.getAnnotations().remove(imports);
		imports = null;
	}

	/**
	 * Adds a method with the specified name, and the @Initializer annotation. Returns it so it can be completed (return type, arguments).
	 *
	 * @param name
	 *            Name of the method
	 * @return
	 */
	public JavaMethod addInitializer(String name) {
		JavaMethod method = new DefaultJavaMethod(name);
		addAnnotation(method, Initializer.class);
		containingInterface.getMethods().add(method);
		return method;
	}

	public ModelEntityAnnotation getModelEntityAnnotation() {
		return entityAnnotation;
	}

	public ModelEntityAnnotation addModelEntityAnnotation() {
		entityAnnotation = new ModelEntityAnnotation(addAnnotation(containingInterface, ModelEntity.class));
		return entityAnnotation;
	}

	public PamelaEntity removeModelEntityAnnotation() {
		containingInterface.getAnnotations().remove(entityAnnotation.getAnnotationSource());
		entityAnnotation = null;
		return this;
	}

	public ModifyAnnotation getModify() {
		return modify;
	}

	public ModifyAnnotation addModify() {
		modify = new ModifyAnnotation(addAnnotation(containingInterface, Modify.class));
		return modify;
	}

	public PamelaEntity removeModify() {
		containingInterface.getAnnotations().remove(modify.getAnnotationSource());
		modify = null;
		return this;
	}

	public DeserializationInitializerAnnotation getDeserializationInitializer() {
		return deserializationInitializer;
	}

	public DeserializationInitializerAnnotation addDeserializationInitializer() {
		return deserializationInitializer;
	}

	public PamelaEntity removeDeserializationInitializer() {
		return this;
	}

	public DeserializationFinalizerAnnotation getDeserializationFinalizer() {
		return deserializationFinalizer;
	}

	public DeserializationFinalizerAnnotation addDeserializationFinalizer() {
		return deserializationFinalizer;
	}

	public PamelaEntity removeDeserializationFinalizer() {
		return this;
	}

	public XMLElementAnnotation getXmlElement() {
		return xmlElement;
	}

	public XMLElementAnnotation addXmlElement() {
		xmlElement = new XMLElementAnnotation(addAnnotation(containingInterface, XMLElement.class));
		return xmlElement;
	}

	public PamelaEntity removeXmlElement() {
		containingInterface.getAnnotations().remove(xmlElement.getAnnotationSource());
		xmlElement = null;
		return this;
	}

	@Override
	public String toString() {
		return "Entity[" + getCanonicalName() + "]";
	}
}
