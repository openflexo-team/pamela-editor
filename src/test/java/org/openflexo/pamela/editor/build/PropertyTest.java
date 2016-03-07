package org.openflexo.pamela.editor.build;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.PAMELAEntityLibrary;
import org.openflexo.pamela.editor.editer.PAMELAProperty;

public class PropertyTest {

	@Before
	public void setUp() throws Exception {
		PAMELAEntityLibrary.clear();
	}

	@Test
	public void testLocationProperty() {
		//load source
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model1","org.openflexo.pamela.editor.model.model1.FlexoProcess");
		
		PAMELAEntity pe = EntityBuilder.getEntityByClassName("org.openflexo.pamela.editor.model.model1.FlexoProcess");
		Map<String, PAMELAProperty> properties = pe.getDeclaredProperty();
		
		PAMELAProperty pnodes = pe.getDeclaredProperty("NODES");
		PAMELAProperty pfoo = pe.getDeclaredProperty("FOO");
		
		/* ==== verify the location of begin and end of the property === */
		
		assertEquals(47,pnodes.getBegin());
		assertEquals(61,pnodes.getEnd());
		
		assertEquals(40,pfoo.getBegin());
		assertEquals(45,pfoo.getEnd());
		
		for(Entry<String, PAMELAProperty> prop :properties.entrySet()){
			System.out.println("propname:"+prop.getKey()+" begin:"+prop.getValue().getBegin()+ " end:"+prop.getValue().getEnd());
		}
	}
	
	@Test
	public void testSetIngoreType(){
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model2",
				"org.openflexo.pamela.editor.model.model2.Library");
		
		PAMELAEntity eLibrary = EntityBuilder.entityLibrary
				.get("org.openflexo.pamela.editor.model.model2.Library");
		PAMELAProperty pbooks = eLibrary.getDeclaredProperty("books");
		
		/* ==== verify set the ignore type  === */
		
		assertEquals(false,pbooks.ignoreType());
		pbooks.setIgnoreType(true);
		assertEquals(true,pbooks.ignoreType());
	}

}
