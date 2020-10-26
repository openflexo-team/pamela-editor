package org.openflexo.pamela.scm.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.scm.PamelaEntity;
import org.openflexo.pamela.scm.PamelaSCMModelFactory;

/**
 * Created by adria on 24/01/2017.
 */
public class EntityTest {
	private static final String BasicPamelaEntity1Path = "src/test/java/org/openflexo/pamela/scm/test/resources1/BasicPamelaEntity1.java";
	private PamelaEntity entity;

	@Before
	public void initialize() throws Exception {
		entity = PamelaSCMModelFactory.createEntity(new File(BasicPamelaEntity1Path));
	}

	@Test
	public void implementationClassTest() {
		entity.addImplementationClass().setValue(BasicImpl.class.getCanonicalName());
		Assert.assertNotNull(entity.getImplementationClass());
		System.out.println(PamelaSCMModelFactory.serialize(entity));
	}

	@Test
	public void implementationClassTest2() {
		entity.addImplementationClass().setValue(BasicImpl.class.getSimpleName());
		entity.removeImplementationClass();
		Assert.assertNull(entity.getImplementationClass());
		System.out.println(PamelaSCMModelFactory.serialize(entity));
	}

	@Test
	public void importsTest() {
		List<String> imports = new ArrayList<>();
		imports.add(String.class.getCanonicalName());
		imports.add(Integer.class.getCanonicalName());
		imports.add(Map.class.getCanonicalName());
		entity.addImports().setValue(imports);
		Assert.assertNotNull(entity.getImportsAnnotation());
		System.out.println(PamelaSCMModelFactory.serialize(entity));
	}

	@Test
	public void nullImportsTest() {
		entity.addImports();
		entity.getImportsAnnotation();
		System.out.println(PamelaSCMModelFactory.serialize(entity));
	}

	@Test
	public void emptyImportsTest() {
		entity.addImports();
		entity.getImportsAnnotation();
		System.out.println(PamelaSCMModelFactory.serialize(entity));
	}
}
