package org.openflexo.pamela.scm.test;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.scm.PamelaEntity;
import org.openflexo.pamela.scm.PamelaSCMModelFactory;
import org.openflexo.pamela.scm.PamelaProperty;

/**
 * Created by adria on 23/01/2017.
 */
public class AdderTest {
	private static final String BasicPamelaEntity1Path = "src/test/java/org/openflexo/pamela/scm/test/resources1/BasicPamelaEntity1.java";
	private PamelaEntity entity;

	@Before
	public void initialize() throws Exception {
		entity = PamelaSCMModelFactory.createEntity(new File(BasicPamelaEntity1Path));
	}

	@Test
	public void valueTest() {
		PamelaProperty property = entity.getProperties().get(0);
	}
}
