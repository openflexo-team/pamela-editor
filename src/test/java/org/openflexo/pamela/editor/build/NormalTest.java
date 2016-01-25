package org.openflexo.pamela.editor.build;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.Cardinality;
import org.openflexo.pamela.editor.editer.PAMELAEntity;

public class NormalTest {

	@Before
	public void setUp() throws Exception {
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model", "");
	}

	@Test
	public void test() {
		PAMELAEntity entityBook = EntityBuilder.entities.get("Book");
		entityBook.createNewProperty("toto", Cardinality.SINGLE, null, null, false, false, "int", null);
	}

}
