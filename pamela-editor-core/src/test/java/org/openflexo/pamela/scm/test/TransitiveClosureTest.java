package org.openflexo.pamela.scm.test;

import java.io.File;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.openflexo.pamela.scm.PamelaSCMModel;
import org.openflexo.pamela.scm.PamelaSCMModelFactory;

/**
 * Created by adria on 12/02/2017.
 */
public class TransitiveClosureTest {
	private static final String directoryPath = "src/test/java/org/openflexo/pamela/scm/test/resources3";

	@Test
	public void transitiveClosureWithEntityOnReturn() throws Exception {
		PamelaSCMModel model = PamelaSCMModelFactory.createModel("org.openflexo.pamela.scm.test.resources3.ChildEntity2", new File(directoryPath));
		Assert.assertEquals(5, model.getEntities().size());
		Assert.assertFalse(model.getEntities().stream().anyMatch(
				entity -> Objects.equals(entity.getCanonicalName(), "org.openflexo.pamela.scm.test.resources3.ForeverAloneEntity")));
		Assert.assertTrue(model.getEntities().stream().anyMatch(
				entity -> Objects.equals(entity.getCanonicalName(), "org.openflexo.pamela.scm.test.resources3.ReferencedEntity")));
		Assert.assertTrue(model.getEntities().stream()
				.anyMatch(entity -> Objects.equals(entity.getCanonicalName(), "org.openflexo.pamela.scm.test.resources3.ImportedEntity")));
	}
}
