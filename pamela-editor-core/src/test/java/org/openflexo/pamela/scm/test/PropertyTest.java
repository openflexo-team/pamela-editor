package org.openflexo.pamela.scm.test;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.scm.PamelaEntity;
import org.openflexo.pamela.scm.PamelaSCMModelFactory;
import org.openflexo.pamela.scm.PamelaProperty;

import com.thoughtworks.qdox.model.impl.DefaultJavaClass;

/**
 * Created by adria on 14/12/2016.
 */
public class PropertyTest {
	private static final String BasicPamelaEntity1Path = "src/test/java/org/openflexo/pamela/scm/test/resources1/BasicPamelaEntity1.java";
	private PamelaEntity entity;

	@Before
	public void initialize() throws Exception {
		entity = PamelaSCMModelFactory.createEntity(new File(BasicPamelaEntity1Path));
	}

	@Test
	public void getPropertiesTest() {
		List<PamelaProperty> properties = entity.getProperties();

		Assert.assertNotNull(properties);
		Assert.assertFalse(properties.isEmpty());
		Assert.assertNotNull(properties.get(0).getGetter());
		Assert.assertNotNull(properties.get(0).getSetter());
	}

	@Test
	public void addPropertiesTest() {
		entity.addProperty("test", new DefaultJavaClass("int"));
		Assert.assertTrue(entity.getProperties().size() == 2);
		System.out.println(PamelaSCMModelFactory.serialize(entity));
	}

	@Test
	public void removePropertiesTest() {
		PamelaProperty propertyToRemove = entity.getProperty("MYATTRIBUTE");
		Assert.assertNotNull(propertyToRemove);
		entity.removeProperty(propertyToRemove.getIdentifier());
		Assert.assertTrue(entity.getProperties().isEmpty());
	}

	@Test
	public void modifyPropertyTest() {
		PamelaProperty property = entity.getProperty("MYATTRIBUTE");

		property.getGetter().setCardinality(Getter.Cardinality.LIST);

		Assert.assertTrue(property.getGetter().getCardinality().equals(Getter.Cardinality.LIST));
	}

	@Test
	public void addSetterToPropertyTest() {
		PamelaProperty property = entity.addProperty("testAdd", new DefaultJavaClass("int"));
		property.addSetter();

		Assert.assertNotNull(property.getSetter());
		Assert.assertTrue(entity.getProperties().size() == 2);
	}

	@Test
	public void addEmbeddedTest() {
		entity.getProperty("MYATTRIBUTE").addEmbedded();
		Assert.assertTrue(entity.getProperty("MYATTRIBUTE").isEmbedded());
	}
}
