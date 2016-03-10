package org.openflexo.pamela.editor.editer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.PropertyException;

import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;
import org.openflexo.pamela.editor.editer.utils.UtilPAMELA;
import org.openflexo.pamela.editor.editer.utils.UtilString;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.expression.AnnotationValue;
import com.thoughtworks.qdox.model.expression.AnnotationValueList;
import com.thoughtworks.qdox.model.impl.DefaultJavaAnnotation;

/**
 * The entity correspondent with the ModelEntity in Pamela-core, now support
 * initialize the entity load from java file; create a entity; remove a
 * entity(the relationship embedding and inherit has been considered);
 * @author ddcat1991
 *
 */
public class PAMELAEntity {
	/**
	 * source.java path
	 */
	private String sourceUrl;
	/**
	 * source.java String TODO: used for serializaton in java file
	 */
	private String sourceString;

	private boolean initialized;

	/**
	 * qualified name of entity
	 */
	private String name;

	/**
	 * the inplementedInterface is the code source
	 */
	private JavaClass implementedInterface;

	/**
	 * the properties declared in the Entity
	 */
	private Map<String, PAMELAProperty> declaredProperties;

	/**
	 * The list of super interfaces of this entity.
	 */
	private List<JavaClass> superImplementedInterfaces;

	/**
	 * The map of super entities (matching the list of super interfaces). This
	 * may be null. key: qualified entity name
	 */
	private Map<String, PAMELAEntity> directSuperEntities;

	/**
	 * key: entity name
	 */
	private Map<String, PAMELAEntity> embeddedEntities;

	/**
	 * the entities with annotation @Import
	 */
	private Set<PAMELAEntity> importEntities;

	private int currentProcessLinenum = 0;

	/**
	 * create entity from class of source.java
	 * 
	 * @param implementedInterface
	 * @throws ModelDefinitionException
	 */
	public PAMELAEntity(JavaClass implementedInterface) throws ModelDefinitionException {
		super();
		this.implementedInterface = implementedInterface;
		this.name = implementedInterface.getFullyQualifiedName();
		this.declaredProperties = new HashMap<String, PAMELAProperty>();
		// this.properties = new HashMap<String, PAMELAProperty>();
		this.embeddedEntities = new HashMap<String, PAMELAEntity>();
		this.importEntities = new HashSet<PAMELAEntity>();
		// model super interface
		for (JavaClass superi : implementedInterface.getInterfaces()) {
			if (isModelEntity(superi)) {
				if (superImplementedInterfaces == null) {
					superImplementedInterfaces = new ArrayList<JavaClass>();
				}
				// use qdox loader to get the interface here, because if we use
				// directly implementedInterface.getInterfaces(), the interface
				// just have a name, the content is empty.
				JavaClass real_superi = UtilPAMELA.getClassByName(implementedInterface, superi.getFullyQualifiedName());
				superImplementedInterfaces.add(real_superi);
			}
		}

		// get fields
		List<JavaField> fields = this.implementedInterface.getFields();
		for (JavaField field : fields) {
			// TODO remember the filed location
			// this.declaredProperties.put(key, value)
			currentProcessLinenum = currentProcessLinenum < field.getLineNumber() ? field.getLineNumber()
					: currentProcessLinenum;
		}

		// we scan all the method
		List<JavaMethod> methods = implementedInterface.getMethods();

		for (JavaMethod method : methods) {

			// get the propertyIdentifier for the method
			String propertyIdentifier = getPropertyIdentifier(method);

			// create the properties
			if (propertyIdentifier != null && !declaredProperties.containsKey(propertyIdentifier)) {
				PAMELAProperty property = PAMELAProperty.loadPAMELAproperty(propertyIdentifier, this);
				declaredProperties.put(propertyIdentifier, property);
			}

			// TODO currentProcessLinenum => end of last method getEndLineNumber
			currentProcessLinenum = method.getLineNumber();
		}

		// TODO Init delegate implementations

		// init supperEntities

		System.out.println("create Entity " + this.name + "-->with property num" + declaredProperties.size());

	}

	/**
	 * create a new PAMELAEntity
	 * 
	 * @param qname
	 *            qualified name of this new entity
	 */
	public PAMELAEntity(String qname) {
		this.implementedInterface = null;
		this.name = qname;
		this.declaredProperties = new HashMap<String, PAMELAProperty>();
		// this.properties = new HashMap<String, PAMELAProperty>();
		this.embeddedEntities = new HashMap<String, PAMELAEntity>();
		this.importEntities = new HashSet<PAMELAEntity>();
	}

	/**
	 * get direct super entity by qualified name
	 * 
	 * @param qname
	 * @return null the entity not exist
	 */
	public PAMELAEntity getDirectSuperEntity(String qname) {
		return directSuperEntities.get(qname);
	}

	/**
	 * used for loading. Add the parent entity into directSuperEntities.
	 * 
	 * @return directSuperEntities
	 * @throws ModelDefinitionException
	 */
	public Map<String, PAMELAEntity> loadDirectSuperEntities() throws ModelDefinitionException {
		if (directSuperEntities == null && superImplementedInterfaces != null) {
			directSuperEntities = new HashMap<String, PAMELAEntity>();
			for (JavaClass superInterface : superImplementedInterfaces) {
				PAMELAEntity superEntity = PAMELAEntityLibrary.get(superInterface, true);
				directSuperEntities.put(superEntity.getName(), superEntity);
			}
		}
		return directSuperEntities;
	}

	/**
	 * get declared property with property name, ignore case
	 * 
	 * @param propertyName
	 *            the identifier of the property
	 * @return
	 */
	public PAMELAProperty getDeclaredProperty(String propertyName) {
		propertyName = propertyName.toUpperCase();
		if (!this.declaredProperties.containsKey(propertyName))
			return null;
		return this.declaredProperties.get(propertyName);
	}

	public Map<String, PAMELAProperty> getDeclaredProperty() {
		return this.declaredProperties;
	}

	public JavaClass getImplementedInterface() {
		return implementedInterface;
	}

	public String getName() {
		return name;
	}

	/**
	 * used in loading property from the source. Scan the annotation of
	 * getter/setter/adder/remover to find the identifier
	 * 
	 * @param m
	 * @return
	 */
	private String getPropertyIdentifier(JavaMethod m) {
		//
		String propertyIdentifier = null;
		JavaAnnotation aGetter = UtilPAMELA.getAnnotation(m, "Getter");
		if (aGetter != null) {
			propertyIdentifier = aGetter.getNamedParameter("value").toString();
		} else {
			JavaAnnotation aSetter = UtilPAMELA.getAnnotation(m, "Setter");
			if (aSetter != null) {
				propertyIdentifier = aSetter.getNamedParameter("value").toString();
			} else {
				JavaAnnotation anAdder = UtilPAMELA.getAnnotation(m, "Adder");
				if (anAdder != null) {
					propertyIdentifier = anAdder.getNamedParameter("value").toString();
				} else {
					JavaAnnotation aRemover = UtilPAMELA.getAnnotation(m, "Remover");
					if (aRemover != null) {
						propertyIdentifier = aRemover.getNamedParameter("value").toString();
					}
				}
			}
		}
		return propertyIdentifier;
	}

	@Override
	public String toString() {
		return " [name=" + name + "]:properties-size:" + declaredProperties.size() + " properties: "
				+ propertiesIdToString();
	}

	private String propertiesIdToString() {
		StringBuilder sb = new StringBuilder("|");
		for (String id : declaredProperties.keySet())
			sb.append(id).append("|");
		return sb.toString();
	}

	public void addsource(String url, String string) {
		this.sourceUrl = url;
		this.sourceString = string;
	}

	/**
	 * add a new property in entity
	 * 
	 * @param prop
	 * @throws PropertyException
	 *             if a property has same identify added in declaredProperties
	 *             map.
	 */
	public void addProperty(PAMELAProperty prop) throws PropertyException {
		if (declaredProperties.containsKey(prop.getIdentifier()))
			throw new PropertyException(prop.getIdentifier() + " is already exist.");
		else {
			prop.setPamelaEntity(this);
			declaredProperties.put(prop.getIdentifier(), prop);

		}
	}

	/**
	 * remove a property from the entity
	 * 
	 * @param pidentify
	 *            the identify of the property
	 * @throws PropertyException
	 */
	public void removeProperty(String pidentify) throws PropertyException {
		pidentify = pidentify.toUpperCase();
		// check property exist
		PAMELAProperty prop = getDeclaredProperty(pidentify);
		if (prop == null)
			throw new PropertyException(pidentify + " is not exist.");
		else {
			// if this property's type is not simple type, and the ignoreType is
			// true
			if (!TypeConverterLibrary.getInstance().hasConverter(prop.getType().getFullyQualifiedName())
					&& !prop.ignoreType()) {
				// try to find if this property is the last one who use the same
				// embedded entity
				boolean flag = false;
				for (PAMELAProperty p : declaredProperties.values()) {
					if (!p.getIdentifier().equals(pidentify)
							&& p.getType().getFullyQualifiedName().equals(prop.getType().getFullyQualifiedName()))
						flag = true;
				}
				// if not found, remove from embedded entity
				if (flag == false)
					removeEmbeddedEntity(prop.getType().getFullyQualifiedName());
			}
			// remove this property
			declaredProperties.remove(pidentify);
		}

	}

	/**
	 * verify if an java class has annotation @ModelEntity
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isModelEntity(JavaClass type) {
		if (UtilPAMELA.getAnnotation(type, "ModelEntity") != null)
			return true;
		return false;
	}

	/**
	 * used when loading a entity from sources.java by Qdox we don't load the
	 * entity if it has ignoreType=true or if it is a primitive type TODO need
	 * to distinguish Enum Type.
	 * 
	 * @throws ModelDefinitionException
	 */
	void init() throws ModelDefinitionException {
		// TODO init----ignore Type....
		// We now resolve our inherited entities and properties

		if (directSuperEntities == null) {
			loadDirectSuperEntities();
		}

		for (PAMELAProperty property : declaredProperties.values()) {

			if (property.getType() != null
					&& !TypeConverterLibrary.getInstance().hasConverter(property.getType().getFullyQualifiedName())
					/*
					 * TODO && !property.getType().isEnum() &&
					 */ && !property.ignoreType()) {
				try {
					// use qdox builder to get the coherent class
					PAMELAEntity addEntity = PAMELAEntityLibrary.get(
							EntityBuilder.builder.getClassByName(property.getType().getFullyQualifiedName()), true);
					embeddedEntities.put(addEntity.getName(), addEntity);
				} catch (ModelDefinitionException e) {
					throw new ModelDefinitionException(
							"Could not retrieve model entity for property " + property + " and entity " + this, e);
				}
			}
		}

		// resolve Annotation Imports
		JavaAnnotation imports = UtilPAMELA.getAnnotation(implementedInterface, "Imports");
		if (imports != null) {
			AnnotationValueList values = (AnnotationValueList) imports.getProperty("value");

			for (AnnotationValue imp : values.getValueList()) {
				String impclsname = (String) ((DefaultJavaAnnotation) imp).getNamedParameter("value");
				impclsname = UtilString.removeDotClassInClassname(impclsname);
				JavaClass impclas = UtilPAMELA.getClassByName(implementedInterface, impclsname);
				importEntities.add(PAMELAEntityLibrary.get(impclas, true));
			}

		}
	}

	public boolean singleInheritance() {
		return superImplementedInterfaces != null && superImplementedInterfaces.size() == 1;
	}

	public boolean multipleInheritance() {
		return superImplementedInterfaces != null && superImplementedInterfaces.size() > 1;
	}

	public Map<String, PAMELAEntity> getEmbeddedEntities() {
		return this.embeddedEntities;
	}

	/**
	 * remove embedded entity and change the corresponding property -> ignore
	 * type = true
	 * 
	 * @param qname
	 *            qualified name of the entity
	 * @return true-removed false-the embedded entity not exist in this entity
	 */
	public boolean removeEmbeddedEntity(String qname) {
		// find if the entity exist
		if (embeddedEntities.containsKey(qname)) {
			// change property's ignoreType
			for (PAMELAProperty prop : declaredProperties.values()) {
				// verify if the property's type is same with the removed Entity
				if (prop.getType().getFullyQualifiedName().equals(qname)) {
					prop.setIgnoreType(true);
				}
			}
			embeddedEntities.remove(qname);
			return true;
		}
		return false;
	}

	/**
	 * remove superEntity
	 * 
	 * @param qname
	 * @return true-removed ; false-the embedded entity not exist in this entity
	 */
	public boolean removeSuperEntity(String qname) {
		if (directSuperEntities != null && directSuperEntities.containsKey(qname)) {
			directSuperEntities.remove(qname);
			return true;
		}
		return false;
	}

	public boolean equals(PAMELAEntity e) {
		return this.getName().equals(e.getName());
	}

}
