package org.openflexo.pamela.scm.test.resources4.package1;

import static org.openflexo.pamela.annotations.Getter.Cardinality.SINGLE;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.scm.test.resources4.package2.Entity2;

/**
 * 
 */
@ModelEntity
public interface Entity1 {
	String ENTITY2 = "entity2";

	@Getter(value = ENTITY2, cardinality = SINGLE)
	Entity2 getEntity2();

	@Setter(ENTITY2)
	void setEntity2(Entity2 entity2);
}
