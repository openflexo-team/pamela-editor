package org.openflexo.pamela.scm.test;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.scm.PamelaEntity;
import org.openflexo.pamela.scm.PamelaSCMModelFactory;

/**
 * Created by adria on 27/02/2017.
 */
public class ParseAllTest extends UltimateEntityTestBase {
	private static final String UltimateModelEntity = "src/test/java/org/openflexo/pamela/scm/test/resources2/UltimateModelEntity.java";
	private PamelaEntity entity;

	@Before
	public void initialize() throws Exception {
		entity = PamelaSCMModelFactory.createEntity(new File(UltimateModelEntity));
	}

	@Test
	public void checkModel() {
		checkEntityAnnotations(entity);
		checkEmbeddedProperty(entity);
		checkEmbeddedListProperty(entity);
	}

}
