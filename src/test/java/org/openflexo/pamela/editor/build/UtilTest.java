package org.openflexo.pamela.editor.build;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.Cardinality;
import org.openflexo.pamela.editor.editer.utils.Location;
import org.openflexo.pamela.editor.editer.utils.UtilPAMELA;

public class UtilTest {

	@Before
	public void setUp() throws Exception {
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model", "");
	}

	@Test
	public void testMethodLocation() {
		
		Location loc = UtilPAMELA.getMethodLocation(EntityBuilder.entities.get("Book").getDeclaredProperty("AUTHOR").getGetter());
		System.out.println(loc);
	}
	
	@Test
	public void testGetterCreator(){
		UtilPAMELA.getterCreator("toto", Cardinality.SINGLE, null, null, false, false, "int", null);
	}

}
