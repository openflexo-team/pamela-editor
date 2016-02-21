package org.openflexo.pamela.editor.editer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openflexo.model.ModelProperty;
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

public class PAMELAEntity {
	/**
	 * source.java path
	 */
	private String sourceUrl;
	/**
	 * source.java String
	 */
	private String sourceString;

	private boolean initialized;

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
	 * The properties of this entity. The key is the identifier of the property
	 */
	private Map<String, PAMELAProperty> properties;

	/**
	 * The list of super entities (matching the list of super interfaces). This
	 * may be null
	 */
	private List<PAMELAEntity> directSuperEntities;

	/**
	 * The list of super interfaces of this entity. This may be null.
	 */
	private List<JavaClass> superImplementedInterfaces;

	private Set<PAMELAEntity> embeddedEntities;

	private Set<PAMELAEntity> importEntities;

	private int currentProcessLinenum = 0;

	public PAMELAEntity(JavaClass implementedInterface) throws ModelDefinitionException {
		super();
		this.implementedInterface = implementedInterface;
		this.name = implementedInterface.getFullyQualifiedName();
		this.declaredProperties = new HashMap<String, PAMELAProperty>();
		this.properties = new HashMap<String, PAMELAProperty>();
		this.embeddedEntities = new HashSet<PAMELAEntity>();
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
				PAMELAProperty property = PAMELAProperty.getPAMELAproperty(propertyIdentifier, this);
				declaredProperties.put(propertyIdentifier, property);
			}

			// TODO currentProcessLinenum => end of last method getEndLineNumber
			currentProcessLinenum = method.getLineNumber();
		}

		// TODO Init delegate implementations

		System.out.println("create Entity " + this.name + "-->with property num" + declaredProperties.size());

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
		// throw new DeclaredPropertyNullException(propertyName+" not exist");
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

	private String getPropertyIdentifier(JavaMethod m) {
		// scan the annotation of getter/setter/adder/remover to find the
		// identifier
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
		return "PAMELAEntity [name=" + name + "]:properties-size:" + declaredProperties.size();
	}

	public void addsource(String url, String string) {
		this.sourceUrl = url;
		this.sourceString = string;
	}

	/*
	 * public PAMELAProperty createNewProperty(String identifier, Cardinality
	 * cardinality, String inverse, String defaultValue, boolean
	 * isStringConvertable, boolean ignoreType, String type, String keyType) {
	 * // identifier not in Properties Map if
	 * (declaredProperties.containsKey(identifier)) return null;
	 * 
	 * // create getter String getterStr = UtilPAMELA.getterCreator(identifier,
	 * cardinality, inverse, defaultValue, isStringConvertable, ignoreType,
	 * type, keyType); // UtilPAMELA.getterCreator(identifier, cardinality,
	 * null, null, // null, // ignoreType, type, keyType);
	 * 
	 * // find last line int lastline = getLastLineNum(); String[] lines =
	 * sourceString.split(System.getProperty("line.separator"));
	 * 
	 * lines[lastline - 1] += System.getProperty("line.separator") + getterStr;
	 * 
	 * StringBuilder sb = new StringBuilder(); for (String str : lines) {
	 * sb.append(str).append(System.getProperty("line.separator")); }
	 * 
	 * // TODO respect the /tab
	 * 
	 * System.out.println(sb + "\n" + lines.length);
	 * 
	 * // TODO return PAMELAProperty
	 * 
	 * return null;
	 * 
	 * }
	 */

	/*
	 * public int getLastLineNum() { // we scan all the method List<JavaMethod>
	 * methods = implementedInterface.getMethods(); List<JavaField> fields =
	 * implementedInterface.getFields(); int lastLine = 0;
	 * 
	 * for (JavaField jf : fields) { int jfline = jf.getLineNumber(); if
	 * (lastLine < jfline) lastLine = jfline; } for (JavaMethod m : methods) {
	 * Location mlocation = UtilPAMELA.getMethodLocation(m); int mEnd =
	 * mlocation.getEnd(); if (lastLine < mEnd) lastLine = mEnd; } return
	 * lastLine; }
	 */

	public List<PAMELAEntity> getDirectSuperEntities() throws ModelDefinitionException {
		if (directSuperEntities == null && superImplementedInterfaces != null) {
			directSuperEntities = new ArrayList<PAMELAEntity>(superImplementedInterfaces.size());
			for (JavaClass superInterface : superImplementedInterfaces) {
				PAMELAEntity superEntity = PAMELAEntityLibrary.get(superInterface, true);
				directSuperEntities.add(superEntity);
			}
		}
		return directSuperEntities;
	}

	public static boolean isModelEntity(JavaClass type) {
		if (UtilPAMELA.getAnnotation(type, "ModelEntity") != null)
			return true;
		return false;
	}

	void init() throws ModelDefinitionException {
		// TODO init----ignore Type....
		// We now resolve our inherited entities and properties

		if (directSuperEntities == null) {
			getDirectSuperEntities();
		}
		/*
		 * if (getDirectSuperEntities() != null) {
		 * embeddedEntities.addAll(getDirectSuperEntities()); }
		 */

		for (PAMELAProperty property : declaredProperties.values()) {

			// System.out.println("init property:" + property.getIdentifier()+"
			// type:"+property.getType().getFullyQualifiedName());

			if (property.getType() != null
					&& !TypeConverterLibrary.getInstance().hasConverter(property.getType().getFullyQualifiedName())
					/*
					 * && !property.getType().isEnum() &&
					 * !property.isStringConvertable()
					 */ && !property.ignoreType()) {
				try {
					// use qdox builder to get the coherent class
					embeddedEntities.add(PAMELAEntityLibrary.get(
							EntityBuilder.builder.getClassByName(property.getType().getFullyQualifiedName()), true));
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
				// TODO do we need a attribute for import Entities?
				// embeddedEntities.add(PAMELAEntityLibrary.get(impclas, true));
				importEntities.add(PAMELAEntityLibrary.get(impclas, true));
			}

		}
	}

	/**
	 * Returns the {@link ModelProperty} with the identifier
	 * <code>propertyIdentifier</code>.
	 * 
	 * @param propertyIdentifier
	 * @return
	 */
	private PAMELAProperty getPAMELAProperty(String propertyIdentifier) {
		return properties.get(propertyIdentifier);
	}

	public boolean singleInheritance() {
		return superImplementedInterfaces != null && superImplementedInterfaces.size() == 1;
	}

	public boolean multipleInheritance() {
		return superImplementedInterfaces != null && superImplementedInterfaces.size() > 1;
	}

	public boolean hasProperty(String propertyIdentifier) {
		return properties.containsKey(propertyIdentifier);
	}

	public Set<PAMELAEntity> getEmbeddedEntities() {
		return this.embeddedEntities;
	}
}
