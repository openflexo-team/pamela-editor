package org.openflexo.pamela.editor.build;

import static org.junit.Assert.assertEquals;

import javax.xml.bind.PropertyException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.Cardinality;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.PAMELAProperty;
import org.openflexo.pamela.editor.editer.exceptions.EntityExistException;

import com.thoughtworks.qdox.model.JavaType;
import com.thoughtworks.qdox.model.impl.DefaultJavaType;

public class EntityTest {

	/**
	 * clean the entityLibrary for each test
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.out.println("===================Test Begin=========================");
		EntityBuilder.entityLibrary.clear();

	}

	/**
	 * add an entity without property
	 * 
	 * @throws EntityExistException
	 */
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

	/**
	 * add an entity with property, TODO 1.the constructor of property; 2.when
	 * set a method of property, the parameters of a annotation needs to be
	 * support
	 * 
	 * @throws EntityExistException
	 */
	@Test
	public void testAddEntityWithProperty() throws EntityExistException {
		PAMELAEntity mytable = new PAMELAEntity("org.openflexo.pamela.editor.model.model2.Table");
		JavaType javatypeColor = new DefaultJavaType("org.openflexo.pamela.editor.model.model2.Color");
		JavaType javatypePrice = new DefaultJavaType("int");
		// create property
		PAMELAProperty color = new PAMELAProperty("color", Cardinality.SINGLE, null, javatypeColor);
		PAMELAProperty price = new PAMELAProperty("price", Cardinality.SINGLE, null, javatypePrice);
		
		try {
			// add property in entity
			mytable.addProperty(color);
			mytable.addProperty(price);
		} catch (PropertyException e) {
			e.printStackTrace();
		}
		// add new entity in library
		EntityBuilder.entityLibrary.add(mytable);

		/* ==== verify properties === */
		PAMELAEntity entity = EntityBuilder.entityLibrary.get("org.openflexo.pamela.editor.model.model2.Table");
		PAMELAProperty pcolor = entity.getDeclaredProperty("color");
		PAMELAProperty pprice = entity.getDeclaredProperty("price");
		assertEquals("org.openflexo.pamela.editor.model.model2.Color", pcolor.getType().getFullyQualifiedName());
		assertEquals(true, pcolor.ignoreType());
		assertEquals("int", pprice.getType().getFullyQualifiedName());
		assertEquals(false, pprice.ignoreType());
	}

	/**
	 * remove an entity, the entities who have the relationship - embedded
	 * with removed entity, this relationship will be deleted (Change the @Getter ignoreType = true)
	 */
	@Test
	public void testRemoveEntityEmbedded() {
		// load a model
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model2",
				"org.openflexo.pamela.editor.model.model2.Library");

		// remove BOOK(qualified name) entity in model2
		EntityBuilder.entityLibrary.remove("org.openflexo.pamela.editor.model.model2.Book");

		/* ==== verify === */
		
		// verify the relationship between other entities and removed entity has been deleted
		PAMELAEntity eLib = EntityBuilder.entityLibrary.get("org.openflexo.pamela.editor.model.model2.Library");
		// i'm not sure: if we delete a entity from EntityLibrary, the
		// entity
		// who has the embedded property of this removed entity will be deleted
		// or not => in my test here, I delete this relationship by changing the ignoreType to "true"
		PAMELAProperty pbooks = eLib.getDeclaredProperty("books");
		assertEquals(true, pbooks.ignoreType());
	}

	/**
	 * remove an entity, the entities who have the relationship - inherit
	 * with removed entity, this relationship will be deleted
	 */
	@Test
	public void testRemoveEntityInherit() {
		// load a model
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model2",
				"org.openflexo.pamela.editor.model.model2.Library");

		// in model2, the Library Entity is inherited from the Company entity
		EntityBuilder.entityLibrary.remove("org.openflexo.pamela.editor.model.model2.Company");
		
		/* ==== verify === */
		
		//verify the inherit relationship between Company and Library has been removed
		PAMELAEntity eLib = EntityBuilder.entityLibrary.get("org.openflexo.pamela.editor.model.model2.Library");
		assertEquals(null, eLib.getDirectSuperEntity("org.openflexo.pamela.editor.model.model2.Company"));
	}

	@After
	public void testAfter() {
		EntityBuilder.entityLibrary.printAllEntities();
	}

}
