package org.openflexo.pamela.scm.test;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.scm.PamelaEntity;
import org.openflexo.pamela.scm.PamelaProperty;

import com.thoughtworks.qdox.model.impl.DefaultJavaClass;

import project.PamelaProject;

/**
 * Created by adria on 03/01/2017.
 */
public class SerializationTest {
	private static final String BasicPamelaEntity1Path = "src/test/java/org/openflexo/pamela/scm/test/resources1/BasicPamelaEntity1.java";
	private PamelaEntity entity;

	@Before
	public void initialize() throws Exception {
		entity = PamelaProject.createEntity(new File(BasicPamelaEntity1Path));
	}

	@Test
	public void serializationTest() throws IOException {
		PamelaProperty property = entity.addProperty("test", new DefaultJavaClass("int"));
		property.addGetter();
		System.out.println(PamelaProject.serialize(entity));
	}

	@Test
	public void serializationTest2() throws IOException {
		PamelaProperty property1 = entity.addProperty("myInt", new DefaultJavaClass("int"));

		PamelaProperty property2 = entity.addProperty("myIntList", new DefaultJavaClass("List"));
		property2.addGetter().setCardinality(Getter.Cardinality.LIST);
		property2.addAdder(new DefaultJavaClass("int"));
		property2.addRemover(new DefaultJavaClass("int"));

		PamelaProperty property3 = entity.addProperty("myIntSet", new DefaultJavaClass("Set"));
		property3.addGetter().setCardinality(Getter.Cardinality.LIST);
		property3.addAdder(new DefaultJavaClass("String"));
		property3.addSetter();

		System.out.println(PamelaProject.serialize(entity));
	}
}
