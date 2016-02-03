package org.openflexo.pamela.editor.build;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.PAMELAProperty;

public class PropertyTest {

	@Before
	public void setUp() throws Exception {
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model1","org.openflexo.pamela.editor.model.model1.FlexoProcess");
	}

	@Test
	public void testLocationProperty() {
		PAMELAEntity pe = EntityBuilder.getEntityByClassName("org.openflexo.pamela.editor.model.model1.FlexoProcess");
		Map<String, PAMELAProperty> properties = pe.getDeclaredProperty();
		for(Entry<String, PAMELAProperty> prop :properties.entrySet()){
			System.out.println("propname:"+prop.getKey()+" begin:"+prop.getValue().getBegin()+ " end:"+prop.getValue().getEnd());
		}
	}

}
