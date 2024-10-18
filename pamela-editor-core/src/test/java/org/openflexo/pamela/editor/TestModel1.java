package org.openflexo.pamela.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class TestModel1 {

	@Test
	public void parsePamelaModel() {
		File f = new File(System.getProperty("user.dir") + "/src/test/java/test/model1");
		SourceMetaModel mm = new SourceMetaModel();
		mm.addInputResource(f);
		mm.buildMetaModel();

		assertEquals(3, mm.getAllPackages().size());
		assertNotNull(mm.getDefaultPackage());
		assertTrue(mm.getAllPackages().contains(mm.getDefaultPackage()));
		assertTrue(mm.getAllPackages().contains(mm.getPackage("test")));
		assertTrue(mm.getAllPackages().contains(mm.getPackage("test.model1")));

		SourcePackage defaultPackage = mm.getDefaultPackage();
		SourcePackage testPackage = mm.getPackage("test");
		SourcePackage model1Package = mm.getPackage("test.model1");

		assertEquals(0, defaultPackage.getEntities().size());
		assertEquals(0, testPackage.getEntities().size());
		assertEquals(2, model1Package.getEntities().size());

		/*for (SourcePackage sourcePackage : mm.getAllPackages()) {
			System.out.println("* " + sourcePackage.getQualifiedName());
			for (SourceModelEntity<?> sourceModelEntity : sourcePackage.getEntities()) {
				System.out.println(" *** " + sourceModelEntity);
			}
		}*/

	}
}
