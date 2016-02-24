package org.openflexo.pamela.editor.build;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.Cardinality;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.PAMELAEntityLibrary;
import org.openflexo.pamela.editor.editer.PAMELAProperty;

public class EntityBuilderTest {

	@Before
	public void setUp() throws Exception {
		PAMELAEntityLibrary.clear();
	}

	@Test
	public void loadTest() {

	}

	/**
	 * Load a single entity from the java source In this test, use the qualified
	 * name of class Library as a entry point of the loader of EntityBuilder
	 * 
	 */
	@Test
	public void testBuilder2SingleEntry() {
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model2",
				"org.openflexo.pamela.editor.model.model2.Library");

		// get entities by implemented class name
		PAMELAEntity eLibrary = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Library");
		PAMELAEntity eBook = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Book");
		PAMELAEntity eCompany = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Company");
		PAMELAEntity ePerson = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Person");
		
		/* ==== verify entities === */

		// verify if the entities loaded are correct
		assertEquals("org.openflexo.pamela.editor.model.model2.Library", eLibrary.getName());
		// book is loaded as a Entity, book is an embedded entity of Library
		assertEquals("org.openflexo.pamela.editor.model.model2.Book", eBook.getName());
		// company is loaded as a Entity, book is an parent of Library
		assertEquals("org.openflexo.pamela.editor.model.model2.Company", eCompany.getName());
		// the person is null, because Person has no relationship with Library
		assertEquals(null, ePerson);

		/* ==== verify properties === */

		// property in Library entity
		PAMELAProperty pBook = eLibrary.getDeclaredProperty("BOOKS");
		assertEquals("BOOKS", pBook.getIdentifier());
		assertEquals(Cardinality.LIST, pBook.getCardinality());

		// properties in Book entity
		Map<String, PAMELAProperty> bookPMap = eBook.getDeclaredProperty();
		assertEquals(2, bookPMap.size());
		// the identifier of the property is write in upper case
		assertEquals("TITLE", eBook.getDeclaredProperty("title").getIdentifier());
		assertEquals("AUTHOR", eBook.getDeclaredProperty("author").getIdentifier());

		// properties in Company entity ( A property must have one of
		// @Getter, @Setter, @Adder, @Remover)
		Map<String, PAMELAProperty> companyPMap = eCompany.getDeclaredProperty();
		assertEquals(2, companyPMap.size());
		// employee has none Annotation, we dont consider employee is a property
		assertEquals(null, eCompany.getDeclaredProperty("employee"));

	}

	@Test
	public void testBuilder3SingleEntry() {
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model1",
				"org.openflexo.pamela.editor.model.model1.FlexoProcess");
	}

	/**
	 * Load multi entities from the java source. In this test, use the qualified
	 * name array for entry points the load path must contain these java source.
	 */
	@Test
	public void testBuilder4MuiltiEntry() {
		String[] classNames = { "org.openflexo.pamela.editor.model.model2.Library",
				"org.openflexo.pamela.editor.model.model2.Person" };
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model", classNames);

		// get entities by implemented class name
		PAMELAEntity eLibrary = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Library");
		PAMELAEntity eBook = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Book");
		PAMELAEntity eCompany = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Company");
		PAMELAEntity ePerson = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Person");

		/* ==== verify entities === */

		assertEquals("org.openflexo.pamela.editor.model.model2.Library", eLibrary.getName());
		assertEquals("org.openflexo.pamela.editor.model.model2.Book", eBook.getName());
		assertEquals("org.openflexo.pamela.editor.model.model2.Company", eCompany.getName());
		assertEquals("org.openflexo.pamela.editor.model.model2.Person", ePerson.getName());

		/* ==== verify properties === */

		// property in Library entity
		PAMELAProperty pBook = eLibrary.getDeclaredProperty("BOOKS");
		assertEquals("BOOKS", pBook.getIdentifier());
		assertEquals(Cardinality.LIST, pBook.getCardinality());

		// properties in Book entity
		Map<String, PAMELAProperty> bookPMap = eBook.getDeclaredProperty();
		assertEquals(2, bookPMap.size());
		// the identifier of the property is write in upper case
		assertEquals("TITLE", eBook.getDeclaredProperty("title").getIdentifier());
		assertEquals("AUTHOR", eBook.getDeclaredProperty("author").getIdentifier());

		// properties in Company entity
		Map<String, PAMELAProperty> companyPMap = eCompany.getDeclaredProperty();
		assertEquals(2, companyPMap.size());
		// employee has none Annotation, we dont consider employee is a property
		assertEquals(null, eCompany.getDeclaredProperty("employee"));

		// properties in Person entity
		assertEquals("NAME", ePerson.getDeclaredProperty("name").getIdentifier());

	}
	


	@After
	public void testAfter() {
		EntityBuilder.entityLibrary.printAllEntities();
	}

}
