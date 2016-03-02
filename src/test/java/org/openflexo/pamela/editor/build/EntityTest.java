package org.openflexo.pamela.editor.build;

import javax.xml.bind.PropertyException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.Cardinality;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.PAMELAProperty;
import org.openflexo.pamela.editor.editer.exceptions.EntityExistException;

public class EntityTest {

	@Before
	public void setUp() throws Exception {
		EntityBuilder.entityLibrary.clear();

	}

	@Test
	public void testAddEmptyEntity() throws EntityExistException {
		PAMELAEntity mytable = new PAMELAEntity("org.openflexo.pamela.editor.model.model2.Table");
		EntityBuilder.entityLibrary.add(mytable);
	}

	/**
	 * add a entity its qualified name is already exist in library
	 * 
	 * @throws EntityExistException
	 */
	@Test(expected = EntityExistException.class)
	public void testAddEntityHaveSameName() throws EntityExistException {
		// load a model
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model2",
				"org.openflexo.pamela.editor.model.model2.Library");
		// create a new entity
		PAMELAEntity mylibrary = new PAMELAEntity("org.openflexo.pamela.editor.model.model2.Library");
		EntityBuilder.entityLibrary.add(mylibrary);
	}

	@Test
	public void testAddEntityWithProperty() throws EntityExistException {
		PAMELAEntity mytable = new PAMELAEntity("org.openflexo.pamela.editor.model.model2.Table");
		//TODO how to resolve javatype in PAMELAProperty
		PAMELAProperty color = new PAMELAProperty("color", Cardinality.SINGLE);
		try {
			mytable.addProperty(color);
		} catch (PropertyException e) {
			e.printStackTrace();
		}
		// TODO add property in mytable
		EntityBuilder.entityLibrary.add(mytable);
	}
	

	@After
	public void testAfter() {
		EntityBuilder.entityLibrary.printAllEntities();
	}

}
