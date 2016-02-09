package org.openflexo.pamela.editor.model;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.model.ModelContext;
import org.openflexo.model.factory.ModelFactory;
import org.openflexo.pamela.editor.model.model2.Company;

/**
 * Verify if my own model is correct
 * @author lenovo
 *
 */
public class ModelTesterPAMELAcore {

	private ModelFactory factory;
	private ModelContext modelContext;
	
	@Before
	public void setUp() throws Exception {
		new File("/tmp").mkdirs();
		modelContext = new ModelContext(Company.class);
		factory = new ModelFactory(modelContext);
	}

	@Test
	public void test() throws Exception {
		System.out.println(modelContext.debug());

		//assertEquals(11, modelContext.getEntityCount());

	}

}
