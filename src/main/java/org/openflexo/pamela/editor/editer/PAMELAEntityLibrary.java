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

	static synchronized PAMELAEntity importEntity(JavaClass implementedInterface) throws ModelDefinitionException {
		PAMELAEntity pamelaEntity = entities.get(implementedInterface.getFullyQualifiedName());
		if (pamelaEntity == null) {
			pamelaEntity = get(implementedInterface, true);
			newEntities.clear();
		}

		return pamelaEntity;
	}

	/**
	 * 
	 * 
	 * @param implementedInterface
	 * @param create:
	 *            true - if the entity is not in the entities Map, it will be
	 *            initialized and add into entities Map.
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

	public static PAMELAEntity get(JavaClass implementedInterface) {
		try {
			return get(implementedInterface, false);
		} catch (ModelDefinitionException e) {
			return null;
		}
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
			//TODO update related entities:
			// delete from embedded,import or super entities
			return entities.remove(qname);
		} else
			throw new EntityExistException(qname + " not exist in library");
	}

	/**
	 * get entity by qualified name
	 * 
	 * @param qname
	 * @return
	 */
	public static PAMELAEntity get(String qname) {
		return entities.get(qname);
	}

	static boolean has(String qname) {
		return entities.containsKey(qname);
	}

	public static void clear() {
		entities.clear();
	}

	public void printAllEntities() {
		for (Entry<String, PAMELAEntity> entry : entities.entrySet()) {
			System.out.println("LB-Entity--:" + entry.getValue().getName());
		}
	}
}
