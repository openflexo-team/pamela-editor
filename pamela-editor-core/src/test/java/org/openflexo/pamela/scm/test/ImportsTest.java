package org.openflexo.pamela.scm.test;

import java.io.File;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.scm.PamelaEntity;

import project.PamelaProject;

/**
 * Created by adria on 17/02/2017.
 */
public class ImportsTest {
	private static final String EntityPath = "src/test/java/org/openflexo/pamela/scm/test/resources1/EntityWithImports.java";
	private PamelaEntity entity;

	@Before
	public void initialize() throws Exception {
		entity = PamelaProject.createEntity(new File(EntityPath));

		System.out.println(PamelaProject.serialize(entity));

	}

	@Test
	public void importsTest() {
		String importedEntity = entity.getImportsAnnotation().getValue().get(0);
		Assert.assertNotNull(importedEntity);
		System.out.println(importedEntity);
	}

	@Test
	public void setImportsTest() {
		String newImport = "basictest.NewEntity";
		entity.getImportsAnnotation().setValue(Collections.singletonList(newImport));
		Assert.assertEquals(newImport, entity.getImportsAnnotation().getValue().get(0));
		System.out.println(entity.getContainingInterface());
	}

	@Test
	public void addImportTest() {
		String newImport = "basictest.NewEntity";
		entity.getImportsAnnotation().addImport(newImport);
		Assert.assertTrue(entity.getImportsAnnotation().getValue().size() == 2);
		System.out.println(entity.getContainingInterface());
	}

	@Test
	public void removeImportTest() {
		entity.getImportsAnnotation().removeImport("org.openflexo.pamela.scm.test.resources1.BasicPamelaEntity1");
		Assert.assertTrue(entity.getImportsAnnotation().getValue().size() == 0);
	}
}
