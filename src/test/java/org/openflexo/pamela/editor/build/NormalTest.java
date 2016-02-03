package org.openflexo.pamela.editor.build;

import org.junit.Before;
import org.openflexo.pamela.editor.builder.EntityBuilder;

public class NormalTest {

	@Before
	public void setUp() throws Exception {
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model", "");
	}

}
