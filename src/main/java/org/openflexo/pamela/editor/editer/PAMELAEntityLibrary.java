package org.openflexo.pamela.editor.editer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openflexo.pamela.editor.editer.exceptions.ModelDefinitionException;

import com.thoughtworks.qdox.model.JavaClass;

/**
 * to save all the entity
 * 
 * @author ddcat1991
 *
 */
public class PAMELAEntityLibrary {

	private static Map<JavaClass, PAMELAEntity> entities = new HashMap<JavaClass, PAMELAEntity>();

	private static List<PAMELAEntity> newEntities = new ArrayList<PAMELAEntity>();

	static synchronized PAMELAEntity importEntity(JavaClass implementedInterface) throws ModelDefinitionException {
		PAMELAEntity pamelaEntity = entities.get(implementedInterface);
		if (pamelaEntity == null) {
			pamelaEntity = get(implementedInterface, true);
			for (PAMELAEntity e : newEntities) {
				e.mergeProperties();
			}
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
		PAMELAEntity pamelaEntity = entities.get(implementedInterface);
		if (pamelaEntity == null && create) {
			if (!PAMELAEntity.isModelEntity(implementedInterface)) {
				throw new ModelDefinitionException("Class " + implementedInterface + " is not a ModelEntity.");
			}
			synchronized (PAMELAEntityLibrary.class) {
				entities.put(implementedInterface, pamelaEntity = new PAMELAEntity(implementedInterface));
				pamelaEntity.init();
				newEntities.add(pamelaEntity);
			}
		}

		return pamelaEntity;
	}

	static PAMELAEntity get(JavaClass implementedInterface) {
		try {
			return get(implementedInterface, false);
		} catch (ModelDefinitionException e) {
			return null;
		}
	}
	
	static boolean has(JavaClass implementedInterface){
		return entities.containsKey(implementedInterface);
	}
	
	public static void clear(){
		entities.clear();
	}

	public void printAllEntities(){
		for(Entry<JavaClass, PAMELAEntity> entry:entities.entrySet()){
			System.out.println("LB-Entity--:"+entry.getValue().getName());
		}
	}
}
