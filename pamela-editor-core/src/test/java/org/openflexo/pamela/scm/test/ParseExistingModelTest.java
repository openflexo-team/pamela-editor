package org.openflexo.pamela.scm.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openflexo.pamela.scm.PamelaEntity;
import org.openflexo.pamela.scm.PamelaSCMModel;
import org.openflexo.pamela.scm.PamelaSCMModelFactory;
import org.openflexo.test.OrderedRunner;
import org.openflexo.test.TestOrder;

/**
 *
 */
@RunWith(OrderedRunner.class)
public class ParseExistingModelTest {
	private static final String Resource3Path = "src/test/java/org/openflexo/pamela/scm/test/resources3";

	private static PamelaSCMModel pamelaModel;

	@Test
	@TestOrder(1)
	public void initialize() throws Exception {

		File currentDir = new File(System.getProperty("user.dir"));
		File resource3dir = new File(currentDir, Resource3Path);
		System.out.println("currentdir=" + currentDir);
		System.out.println("resource3dir=" + resource3dir + " exist: " + resource3dir.exists());
		pamelaModel = PamelaSCMModelFactory.makePamelaModel(resource3dir, "org.openflexo.pamela.scm.test.resources3.ChildEntity2");
	}

	@Test
	@TestOrder(2)
	public void checkInitialModel() {
		assertEquals(6, pamelaModel.getEntities().size());
		PamelaEntity childEntity2 = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ChildEntity2");
		PamelaEntity motherEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.MotherEntity");
		PamelaEntity referencedEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ReferencedEntity");
		assertEquals(1, pamelaModel.getHeadEntities().size());
		assertTrue(pamelaModel.getHeadEntities().contains(childEntity2));
		assertEquals(3, pamelaModel.getAccessibleEntities().size());
		assertTrue(pamelaModel.getAccessibleEntities().contains(childEntity2));
		assertTrue(pamelaModel.getAccessibleEntities().contains(motherEntity));
		assertTrue(pamelaModel.getAccessibleEntities().contains(referencedEntity));
		PamelaEntity forEverAloneEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ForeverAloneEntity");
		assertNotNull(forEverAloneEntity);
	}

	@Test
	@TestOrder(3)
	public void changeHeadClassToMother() {
		pamelaModel.setHeadClassName("org.openflexo.pamela.scm.test.resources3.MotherEntity");
		PamelaEntity motherEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.MotherEntity");
		assertEquals(1, pamelaModel.getHeadEntities().size());
		assertTrue(pamelaModel.getHeadEntities().contains(motherEntity));
		assertEquals(6, pamelaModel.getEntities().size());
		assertEquals(1, pamelaModel.getAccessibleEntities().size());
		assertTrue(pamelaModel.getAccessibleEntities().contains(motherEntity));
	}

	@Test
	@TestOrder(4)
	public void changeHeadClassToChildEntity() {
		pamelaModel.setHeadClassName("org.openflexo.pamela.scm.test.resources3.ChildEntity");
		PamelaEntity motherEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.MotherEntity");
		PamelaEntity childEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ChildEntity");
		PamelaEntity importedEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ImportedEntity");
		PamelaEntity forEverAloneEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ForeverAloneEntity");
		assertNotNull(forEverAloneEntity);
		assertEquals(1, pamelaModel.getHeadEntities().size());
		assertTrue(pamelaModel.getHeadEntities().contains(childEntity));
		assertEquals(6, pamelaModel.getEntities().size());
		assertEquals(3, pamelaModel.getAccessibleEntities().size());
		assertTrue(pamelaModel.getAccessibleEntities().contains(childEntity));
		assertTrue(pamelaModel.getAccessibleEntities().contains(motherEntity));
		assertTrue(pamelaModel.getAccessibleEntities().contains(importedEntity));
	}

	@Test
	@TestOrder(5)
	public void changeHeadClassToBothEntities() {
		pamelaModel.setHeadClassNames("org.openflexo.pamela.scm.test.resources3.ChildEntity",
				"org.openflexo.pamela.scm.test.resources3.ChildEntity2");
		PamelaEntity childEntity2 = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ChildEntity2");
		PamelaEntity motherEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.MotherEntity");
		PamelaEntity referencedEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ReferencedEntity");
		PamelaEntity childEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ChildEntity");
		PamelaEntity importedEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ImportedEntity");
		PamelaEntity forEverAloneEntity = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources3.ForeverAloneEntity");
		assertNotNull(forEverAloneEntity);
		assertEquals(2, pamelaModel.getHeadEntities().size());
		assertTrue(pamelaModel.getHeadEntities().contains(childEntity));
		assertTrue(pamelaModel.getHeadEntities().contains(childEntity2));
		assertEquals(6, pamelaModel.getEntities().size());
		assertEquals(5, pamelaModel.getAccessibleEntities().size());
		assertTrue(pamelaModel.getAccessibleEntities().contains(childEntity));
		assertTrue(pamelaModel.getAccessibleEntities().contains(motherEntity));
		assertTrue(pamelaModel.getAccessibleEntities().contains(importedEntity));
		assertTrue(pamelaModel.getAccessibleEntities().contains(childEntity2));
		assertTrue(pamelaModel.getAccessibleEntities().contains(referencedEntity));
	}
}
