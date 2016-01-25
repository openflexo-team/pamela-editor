package org.openflexo.pamela.editor.editer;

import java.util.HashMap;
import java.util.HashSet;
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
	
	public PAMELAContext(@Nonnull JavaClass baseClass) throws ModelDefinitionException {
		this.baseClass = baseClass;
		modelEntities = new HashMap<JavaClass, PAMELAEntity>();
		PAMELAEntity modelEntity = PAMELAEntityLiberary.importEntity(baseClass);
		appendEntity(modelEntity, new HashSet<PAMELAEntity>());
	}

	private void appendEntity(PAMELAEntity modelEntity, Set<PAMELAEntity> visited) {
		visited.add(modelEntity);
		modelEntities.put(modelEntity.getInplementedInterface(), modelEntity);
		
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

	public void setBaseClass(JavaClass baseClass) {
		this.baseClass = baseClass;
	}
	
	

	
}
