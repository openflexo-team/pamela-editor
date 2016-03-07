package org.openflexo.pamela.editor.editer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openflexo.pamela.editor.editer.exceptions.EntityExistException;
import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;

import com.thoughtworks.qdox.model.JavaClass;

/**
 * to store all the entity
 * 
 * @author ddcat1991
 *
 */
public class PAMELAEntityLibrary {

	/**
	 * entities key:qualified name of entities
	 */
	private static Map<String, PAMELAEntity> entities = new HashMap<String, PAMELAEntity>();

	private static List<PAMELAEntity> newEntities = new ArrayList<PAMELAEntity>();

	/**
	 * import an entity from implementedInterface(JavaClass), if this entity not
	 * exist in the library then create and add this entity in library
	 * 
	 * @param implementedInterface
	 * @return
	 * @throws ModelDefinitionException
	 */
	static synchronized PAMELAEntity importEntity(JavaClass implementedInterface) throws ModelDefinitionException {
		PAMELAEntity pamelaEntity = entities.get(implementedInterface.getFullyQualifiedName());
		if (pamelaEntity == null) {
			pamelaEntity = get(implementedInterface, true);
			newEntities.clear();
		}

		return pamelaEntity;
	}

	/**
	 * get an entity from entities
	 * 
	 * @param implementedInterface
	 * @param create
	 *            true - if the entity is not in the entities Map, it will be
	 *            initialized and add into entities Map, then return this new
	 *            entity. false - if the entity exist then return the entity,
	 *            otherwise return null
	 * @return
	 * @throws ModelDefinitionException
	 */
	static PAMELAEntity get(JavaClass implementedInterface, boolean create) throws ModelDefinitionException {
		PAMELAEntity pamelaEntity = entities.get(implementedInterface.getFullyQualifiedName());
		if (pamelaEntity == null && create) {
			if (!PAMELAEntity.isModelEntity(implementedInterface)) {
				throw new ModelDefinitionException("Class " + implementedInterface + " is not a ModelEntity.");
			}
			synchronized (PAMELAEntityLibrary.class) {
				entities.put(implementedInterface.getFullyQualifiedName(),
						pamelaEntity = new PAMELAEntity(implementedInterface));
				pamelaEntity.init();
				newEntities.add(pamelaEntity);
			}
		}

		return pamelaEntity;
	}

	/**
	 * get an entity class by implemented Interface
	 * 
	 * @param implementedInterface
	 * @return
	 */
	public static PAMELAEntity get(JavaClass implementedInterface) {
		try {
			return get(implementedInterface, false);
		} catch (ModelDefinitionException e) {
			return null;
		}
	}

	/**
	 * get entity by qualified name (from entity library)
	 * 
	 * @param qname
	 * @return
	 */
	public static PAMELAEntity get(String qname) {
		return entities.get(qname);
	}

	/**
	 * add an entity in library and verify if the qualified name is existed in
	 * library
	 * 
	 * @param addentity
	 * @return
	 * @throws EntityExistException
	 */
	public PAMELAEntity add(PAMELAEntity addentity) throws EntityExistException {
		if (!has(addentity.getName())) {
			entities.put(addentity.getName(), addentity);
			return addentity;
		} else
			throw new EntityExistException(addentity.getName() + " is already exist in library");
	}

	/**
	 * delete a entity from library, check if entity exist in the library
	 * 
	 * @param qname
	 *            qualified name of entity
	 * @return entity deleted
	 * @throws EntityExistException
	 *             if the entity not exist in the library
	 */
	public PAMELAEntity delete(String qname) throws EntityExistException {
		if (has(qname)) {
			// TODO update related entities:
			// delete from embedded,import or super entities
			return entities.remove(qname);
		} else
			throw new EntityExistException(qname + " not exist in library");
	}

	public static boolean has(String qname) {
		return entities.containsKey(qname);
	}

	/**
	 * clear entities library
	 */
	public static void clear() {
		entities.clear();
	}

	public void printAllEntities() {
		for (Entry<String, PAMELAEntity> entry : entities.entrySet()) {
			System.out.println("LB-Entity--:" + entry.getValue().toString());
		}
	}

	/**
	 * TODO remove import relationship
	 * remove an entity from library the relationship
	 * (inherit,embedded,import) with other entities need to be considered
	 * 
	 * @param string
	 */
	public void remove(String qname) {
		// find if entity existed
		PAMELAEntity rEntity = get(qname);
		// traverse all entities
		for (PAMELAEntity e : entities.values()) {
			// remove the embedded relationship(change
			// ignoreType->true)
			e.removeEmbeddedEntity(qname);
			// remove inherit relationship
			e.removeSuperEntity(qname);
			// TODO remove import relationship
		}

		// remove
		entities.remove(qname);

	}

}
