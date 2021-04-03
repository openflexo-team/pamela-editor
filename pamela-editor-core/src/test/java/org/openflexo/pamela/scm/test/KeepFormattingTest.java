package org.openflexo.pamela.scm.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.scm.PamelaEntity;
import org.openflexo.pamela.scm.PamelaSCMModel;
import org.openflexo.pamela.scm.PamelaSCMModelFactory;
import org.openflexo.toolbox.FileUtils;

/**
 * Created by adria on 24/01/2017.
 */
public class KeepFormattingTest {
	private static final String Resource1Path = "src/test/java/org/openflexo/pamela/scm/test/resources1";
	private PamelaEntity entity;

	private static PamelaSCMModel pamelaModel;
	private static PamelaEntity basicPamelaEntity1;

	@BeforeClass
	public static void initialize() throws Exception {

		File currentDir = new File(System.getProperty("user.dir"));
		File resource1dir = new File(currentDir, Resource1Path);
		System.out.println("Working on " + resource1dir);
		pamelaModel = PamelaSCMModelFactory.makePamelaModel(resource1dir, "org.openflexo.pamela.scm.test.resources1.BasicPamelaEntity1");
		basicPamelaEntity1 = pamelaModel.getEntityNamed("org.openflexo.pamela.scm.test.resources1.BasicPamelaEntity1");
	}

	@Test
	public void compareSerialization() throws IOException {

		String expected = FileUtils.fileContents(basicPamelaEntity1.getSourceFile());
		String actual = PamelaSCMModelFactory.serialize(entity);

		System.out.println("---------> Avant:");
		System.out.println(expected);

		System.out.println("---------> Apres:");
		System.out.println(actual);

		assertEquals(expected, actual);

	}

}
