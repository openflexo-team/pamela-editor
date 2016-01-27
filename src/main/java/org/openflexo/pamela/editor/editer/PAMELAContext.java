package org.openflexo.pamela.editor.editer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;

import com.thoughtworks.qdox.model.JavaClass;

public class PAMELAContext {
	
	
	public static class PAMELAPropertyXMLTag<I> {
		private String tag;
		private PAMELAProperty property;
		private PAMELAEntity accessedEntity;

		public PAMELAPropertyXMLTag(PAMELAProperty property) {
			super();
			this.property = property;
			this.accessedEntity = null;
			//TODO 
			//this.tag = property.getXMLContext() + property.getXMLElement().xmlTag();
		}

		public PAMELAPropertyXMLTag(PAMELAProperty property, PAMELAEntity accessedEntity) {
			super();
			this.property = property;
			this.accessedEntity = accessedEntity;
			//TODO
			//this.tag = property.getXMLContext() + accessedEntity.getXMLTag();
		}

		public String getTag() {
			return tag;
		}

		public PAMELAProperty getProperty() {
			return property;
		}

		public PAMELAEntity getAccessedEntity() {
			return accessedEntity;
		}

		@Override
		public String toString() {
			return "PAMELAPropertyXMLTag" + getAccessedEntity() + getProperty() + "/tag=" + getTag();
		}
	}	

	private Map<JavaClass,PAMELAEntity> modelEntities;
	private Map<String, PAMELAEntity> modelEntitiesByXmlTag;
	private Map<PAMELAEntity, Map<String, PAMELAPropertyXMLTag<?>>> modelPropertiesByXmlTag;
	private JavaClass baseClass;
	
	/**
	 * one entry base class
	 * 
	 * @param baseClass
	 * @throws ModelDefinitionException
	 */
	public PAMELAContext(@Nonnull JavaClass baseClass) throws ModelDefinitionException {
		this.baseClass = baseClass;
		modelEntities = new HashMap<JavaClass, PAMELAEntity>();
		PAMELAEntity modelEntity = PAMELAEntityLibrary.importEntity(baseClass);
		appendEntity(modelEntity, new HashSet<PAMELAEntity>());
	}
	
	public PAMELAContext(JavaClass baseClass, List<PAMELAContext> contexts) throws ModelDefinitionException{
		this.baseClass = baseClass;
		modelEntities = new HashMap<JavaClass, PAMELAEntity>();
		//modelEntitiesByXmlTag = new HashMap<String, PAMELAEntity>();
		//modelPropertiesByXmlTag = new HashMap<PAMELAEntity, Map<String, PAMELAPropertyXMLTag<?>>>();
		/*
		for (PAMELAContext context : contexts) {
			for (Entry<String, PAMELAEntity> e : context.modelEntitiesByXmlTag.entrySet()) {
				PAMELAEntity entity = modelEntitiesByXmlTag.put(e.getKey(), e.getValue());
				// TODO: handle properly namespaces. Different namespaces allows to have identical tags
				// See also importModelEntity(Class<T>)
				if (entity != null && !entity.getImplementedInterface().equals(e.getValue().getImplementedInterface())) {
					throw new ModelDefinitionException(
							entity + " and " + e.getValue() + " declare the same XML tag but not the same implemented interface");
				}
			}
			modelEntities.putAll(context.modelEntities);
		}
		*/
		if (baseClass != null) {
			PAMELAEntity modelEntity = PAMELAEntityLibrary.importEntity(baseClass);
			appendEntity(modelEntity, new HashSet<PAMELAEntity>());
		}
		//modelEntities = Collections.unmodifiableMap(modelEntities);
		//modelEntitiesByXmlTag = Collections.unmodifiableMap(modelEntitiesByXmlTag);
	}
	
	public PAMELAContext(JavaClass... baseClasses) throws ModelDefinitionException{
		this(null,makePAMELAContextList(baseClasses));
	}
	

	private static List<PAMELAContext> makePAMELAContextList(JavaClass[] baseClasses) throws ModelDefinitionException {
		List<PAMELAContext> returned = new ArrayList<PAMELAContext>();
		for(JavaClass c:baseClasses){
			returned.add(PAMELAContextLibrary.getModelContext(c));
		}
		return returned;
	}
	
	public PAMELAContext(PAMELAContext... contexts) throws ModelDefinitionException {
		this(null, contexts);
	}

	public PAMELAContext(JavaClass baseClass, PAMELAContext... contexts) throws ModelDefinitionException {
		this(baseClass, Arrays.asList(contexts));
	}

	private void appendEntity(PAMELAEntity modelEntity, Set<PAMELAEntity> visited) {
		visited.add(modelEntity);
		modelEntities.put(modelEntity.getImplementedInterface(), modelEntity);
		
		//TODO
		/*
		ModelEntity<?> put = modelEntitiesByXmlTag.put(modelEntity.getXMLTag(), modelEntity);
		if (put != null && put != modelEntity) {
			throw new ModelDefinitionException(
					"Two entities define the same XMLTag '" + modelEntity.getXMLTag() + "'. Implemented interfaces: "
							+ modelEntity.getImplementedInterface().getName() + " " + put.getImplementedInterface().getName());
		}
		*/
		for (PAMELAEntity e : modelEntity.getEmbeddedEntities()) {
			if (!visited.contains(e)) {
				appendEntity(e, visited);
			}
		}
		
	}

	public JavaClass getBaseClass() {
		return baseClass;
	}

	public PAMELAEntity getPAMELAEntity(String xmlElementName) {
		return modelEntitiesByXmlTag.get(xmlElementName);
	}

	public Iterator<PAMELAEntity> getEntities() {
		return modelEntities.values().iterator();
	}

	public int getEntityCount() {
		return modelEntities.size();
	}

	public PAMELAEntity getPAMELAEntity(JavaClass implementedInterface) {
		return modelEntities.get(implementedInterface);
	}
	

	
}
