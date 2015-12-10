package org.openflexo.pamela.editor.editer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openflexo.model.exceptions.ModelDefinitionException;
import org.openflexo.pamela.editor.editer.exceptions.DeclaredPropertyNullException;
import org.openflexo.pamela.editor.editer.utils.UtilPAMELA;

import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;

public class PAMELAEntity {

	private String name;

	/**
	 * the inplementedInterface is the code source
	 */
	private JavaClass inplementedInterface;

	/**
	 * the properties declared in the Entity
	 */
	private Map<String, PAMELAProperty> declaredProperties;

	public PAMELAEntity(JavaClass inplementedInterface) throws ModelDefinitionException {
		super();
		this.inplementedInterface = inplementedInterface;
		this.name = inplementedInterface.getName();
		this.declaredProperties = new HashMap<String, PAMELAProperty>();

		// model super interface

		// get fields
		List<JavaField> fields = this.inplementedInterface.getFields();
		for (JavaField field : fields) {
			//TODO
			//System.out.println(field.getName());
			// this.declaredProperties.put(key, value)
		}

		// we scan all the method
		List<JavaMethod> methods = inplementedInterface.getMethods();
		for (JavaMethod method : methods) {

			// get the propertyIdentifier for the method
			String propertyIdentifier = getPropertyIdentifier(method);
		
			//create the properties
			if( propertyIdentifier!=null && !declaredProperties.containsKey(propertyIdentifier)){
				PAMELAProperty property = PAMELAProperty.getPAMELAproperty(propertyIdentifier,this);
				declaredProperties.put(propertyIdentifier, property);
			}
			
		}
		
		//add New Entity to Entityliberary
		//TODO
		
		System.out.println("End create Entity-->with property num"+ declaredProperties.size());
	}

	public PAMELAProperty getDeclaredProperty(String propertyName) throws DeclaredPropertyNullException{
		if(!this.declaredProperties.containsKey(propertyName))
				throw new DeclaredPropertyNullException(propertyName+" not exist");
		return this.declaredProperties.get(propertyName);	
	}
	
	public Map<String, PAMELAProperty> getDeclaredProperty(){
		return this.declaredProperties;	
	}

	public JavaClass getInplementedInterface() {
		return inplementedInterface;
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
		}else{
			JavaAnnotation aSetter = UtilPAMELA.getAnnotation(m, "Setter");
			if(aSetter!=null){
				propertyIdentifier = aSetter.getNamedParameter("value").toString();
			}else{
				JavaAnnotation anAdder = UtilPAMELA.getAnnotation(m, "Adder");
				if(anAdder!=null){
					propertyIdentifier = anAdder.getNamedParameter("value").toString();
				}else{
					JavaAnnotation aRemover = UtilPAMELA.getAnnotation(m, "Remover");
					if(aRemover!=null){
						propertyIdentifier = aRemover.getNamedParameter("value").toString();
					}
				}
			}
		}
		return propertyIdentifier;
	}

	@Override
	public String toString() {
		return "PAMELAEntity [name=" + name + "]:properties-size:"+ declaredProperties.size();
	}
	
	

}
