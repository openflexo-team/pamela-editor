package org.openflexo.pamela.scm.test;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openflexo.pamela.scm.PamelaEntity;
import org.openflexo.pamela.scm.PamelaProperty;

import project.PamelaProject;

/**
 * Created by adria on 24/01/2017.
 */
public class PamelaProjectTest {
	private static final String BasicPamelaEntity1Path = "src/test/java/org/openflexo/pamela/scm/test/resources1/BasicPamelaEntity1.java";

	@Test
	public void createEntityTest() throws Exception {
		PamelaEntity entity = PamelaProject.createEntity(new File(BasicPamelaEntity1Path));

		List<PamelaProperty> properties = entity.getProperties();

		Assert.assertNotNull(properties);
		Assert.assertFalse(properties.isEmpty());
		Assert.assertNotNull(properties.get(0).getGetter());
		Assert.assertNotNull(properties.get(0).getSetter());
	}
}
