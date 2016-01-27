package org.openflexo.pamela.editor.editer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openflexo.model.ModelEntity;
import org.openflexo.model.ModelProperty;
import org.openflexo.model.annotations.Getter;
import org.openflexo.model.exceptions.PropertyClashException;
import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;
import org.openflexo.pamela.editor.editer.utils.Location;
import org.openflexo.pamela.editor.editer.utils.UtilPAMELA;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;

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
	 * The list of super entities (matching the list of super interfaces). This may be null
	 */	
	private List<PAMELAEntity> directSuperEntities;
	
	/**
	 * The list of super interfaces of this entity. This may be null.
	 */
	private List<JavaClass> superImplementedInterfaces;
	
	private Set<PAMELAEntity> embeddedEntities;

	public PAMELAEntity(JavaClass inplementedInterface) throws ModelDefinitionException {
		super();
		this.implementedInterface = inplementedInterface;
		this.name = inplementedInterface.getFullyQualifiedName();
		System.out.println("full-qualified-name"+this.name);
		this.declaredProperties = new HashMap<String, PAMELAProperty>();
		this.properties = new HashMap<String, PAMELAProperty>();
		this.embeddedEntities = new HashSet<PAMELAEntity>();
		// model super interface

		// get fields
		List<JavaField> fields = this.implementedInterface.getFields();
		for (JavaField field : fields) {
			// TODO
			// System.out.println(field.getName());
			// this.declaredProperties.put(key, value)
		}

		// we scan all the method
		List<JavaMethod> methods = inplementedInterface.getMethods();
		for (JavaMethod method : methods) {

			// get the propertyIdentifier for the method
			String propertyIdentifier = getPropertyIdentifier(method);

			// create the properties
			if (propertyIdentifier != null && !declaredProperties.containsKey(propertyIdentifier)) {
				PAMELAProperty property = PAMELAProperty.getPAMELAproperty(propertyIdentifier, this);
				declaredProperties.put(propertyIdentifier, property);
			}

		}

		//TODO Init delegate implementations
		
		System.out.println("End create Entity "+ this.name +"-->with property num" + declaredProperties.size());
		
	}

	public PAMELAProperty getDeclaredProperty(String propertyName) {
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

	public PAMELAProperty createNewProperty(String identifier, Cardinality cardinality, String inverse,
			String defaultValue, boolean isStringConvertable, boolean ignoreType, String type, String keyType) {
		// identifier not in Properties Map
		if (declaredProperties.containsKey(identifier))
			return null;

		// create getter
		String getterStr = UtilPAMELA.getterCreator(identifier, cardinality, inverse, defaultValue, isStringConvertable,
				ignoreType, type, keyType);
				// UtilPAMELA.getterCreator(identifier, cardinality, null, null,
				// null,
				// ignoreType, type, keyType);

		// find last line
		int lastline = getLastLineNum();
		String[] lines = sourceString.split(System.getProperty("line.separator"));

		lines[lastline - 1] += System.getProperty("line.separator") + getterStr;

		StringBuilder sb = new StringBuilder();
		for (String str : lines) {
			sb.append(str).append(System.getProperty("line.separator"));
		}

		// TODO respect the /tab

		System.out.println(sb + "\n" + lines.length);

		// TODO return PAMELAProperty

		return null;

	}

	public int getLastLineNum() {
		// we scan all the method
		List<JavaMethod> methods = implementedInterface.getMethods();
		List<JavaField> fields = implementedInterface.getFields();
		int lastLine = 0;

		for (JavaField jf : fields) {
			int jfline = jf.getLineNumber();
			if (lastLine < jfline)
				lastLine = jfline;
		}
		for (JavaMethod m : methods) {
			Location mlocation = UtilPAMELA.getMethodLocation(m);
			int mEnd = mlocation.getEnd();
			if (lastLine < mEnd)
				lastLine = mEnd;
		}
		return lastLine;
	}

	void mergeProperties() throws ModelDefinitionException {
		if (initialized) {
			return;
		}
		properties.putAll(declaredProperties);

		// Resolve inherited properties (we only scan direct parent properties,
		// since themselves will scan for their inherited parents)
		if(getDirectSuperEntities() != null){
			for(PAMELAEntity parentEntity : getDirectSuperEntities()){
				parentEntity.mergeProperties();
				for(PAMELAProperty property:parentEntity.properties.values()){
					//TODO createMergedProperty - line 449 in ModelEntity
				}
			}
		}
		
		//TODO validate properties(they all have a getter and a return type etc??)
		initialized = true;
		//TODO propertyMethod, initializers
	}

	public List<PAMELAEntity> getDirectSuperEntities() throws ModelDefinitionException {
		if(directSuperEntities == null && superImplementedInterfaces != null){
			directSuperEntities = new ArrayList<PAMELAEntity>(superImplementedInterfaces.size());
			for (JavaClass superInterface : superImplementedInterfaces) {
				PAMELAEntity superEntity = PAMELAEntityLibrary.get(superInterface, true);
				directSuperEntities.add(superEntity);
			}
		}
		return directSuperEntities;
	}

	public static boolean isModelEntity(JavaClass type) {
		if(UtilPAMELA.getAnnotation(type, "ModelEntity")!=null)
			return true;
		return false;
	}

	void init() throws ModelDefinitionException {
		// TODO init----ignore Type....
		// We now resolve our inherited entities and properties
		/*
		if (getDirectSuperEntities() != null) {
			embeddedEntities.addAll(getDirectSuperEntities());
		}
		*/
		for (PAMELAProperty property : declaredProperties.values()) {
			
			System.out.println(property.getIdentifier()+"===property.getType.FullQualidiedName:"+property.getType().getFullyQualifiedName());
			
			if (property.getType() != null   && !TypeConverterLibrary.getInstance().hasConverter(property.getType().getFullyQualifiedName())
					/*&& !property.getType().isEnum() && !property.isStringConvertable()*/ && !property.ignoreType()) {
				try {		
						embeddedEntities.add(PAMELAEntityLibrary.get((JavaClass) property.getType(), true));	
				} catch (ModelDefinitionException e) {
					throw new ModelDefinitionException("Could not retrieve model entity for property " + property + " and entity " + this,
							e);
				}
			}
		}

		//TODO We also resolve our imports
		/*
		Imports imports = implementedInterface.getAnnotation(Imports.class);
		if (imports != null) {
			for (Import imp : imports.value()) {
				embeddedEntities.add(ModelEntityLibrary.get(imp.value(), true));
			}
		}
		*/
	}
	
	
	/**
	 * Creates the {@link ModelProperty} with the identifier <code>propertyIdentifier</code>.
	 * 
	 * @param propertyIdentifier
	 *            the identifier of the property
	 * @param create
	 *            whether the property should be create or not, if not found
	 * @return the property with the identifier <code>propertyIdentifier</code>.
	 * @throws ModelDefinitionException
	 */
	void createMergedProperty(String propertyIdentifier, boolean create) throws ModelDefinitionException {
		PAMELAProperty returned = buildPAMELAProperty(propertyIdentifier);
		properties.put(propertyIdentifier, returned);
	}

	/**
	 * Builds the {@link ModelProperty} with identifier <code>propertyIdentifier</code>, if it is declared at least once in the hierarchy
	 * (i.e., at least one method is annotated with the {@link Getter} annotation and the given identifier, <code>propertyIdentifier</code>
	 * ). In case of inheritance, the property is combined with all its ancestors. In case of multiple inheritance of the same property,
	 * conflicts are resolved to the possible extent. In case of contradiction, a {@link PropertyClashException} is thrown.
	 * 
	 * @param propertyIdentifier
	 *            the identifier of the property
	 * @return the new, possibly combined, property.
	 * @throws ModelDefinitionException
	 *             in case of an inconsistency in the model of a clash of property inheritance.
	 */
	private PAMELAProperty buildPAMELAProperty(String propertyIdentifier) throws ModelDefinitionException {
		PAMELAProperty property = PAMELAProperty.getPAMELAproperty(propertyIdentifier, this);
		if (singleInheritance() || multipleInheritance()) {
			PAMELAProperty parentProperty = buildModelPropertyUsingParentProperties(propertyIdentifier, property);
			//TODO
			//return combine(property, parentProperty);
		}
		return property;
	}
	
	/**
	 * Returns a model property with the identifier <code>propertyIdentifier</code> which is a combination of all the model properties with
	 * the identifier <code>propertyIdentifier</code> of the parent entities. This method may return <code>null</code> in case amongst all
	 * parents, non of them declare a property with identifier <code>propertyIdentifier</code>.
	 * 
	 * @param propertyIdentifier
	 *            the identifier of the property
	 * @param property
	 *            the model property with the identifier defined for <code>this</code> {@link ModelEntity}.
	 * @return
	 * @throws ModelDefinitionException
	 */
	private PAMELAProperty buildModelPropertyUsingParentProperties(String propertyIdentifier, PAMELAProperty property)
			throws ModelDefinitionException {
		PAMELAProperty returned = null;
		for (PAMELAEntity parent : getDirectSuperEntities()) {
			if (!parent.hasProperty(propertyIdentifier)) {
				continue;
			}
			if (returned == null) {
				returned = parent.getPAMELAProperty(propertyIdentifier);
			}
			else {
				//TODO
				//returned = combineAsAncestors(parent.getPAMELAProperty(propertyIdentifier), returned, property);
			}
		}
		return returned;
	}

	
	/**
	 * Returns the {@link ModelProperty} with the identifier <code>propertyIdentifier</code>.
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
