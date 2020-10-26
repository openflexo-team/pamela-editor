package org.openflexo.pamela.scm.test.resources4.package2;

import static org.openflexo.pamela.annotations.Getter.Cardinality.SINGLE;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.scm.test.resources4.package1.Entity1;

/**
 * Created by adria on 19/01/1017.
 */
@ModelEntity
public interface Entity2 {
	String ENTITY1 = "entity1";

	@Getter(value = ENTITY1, cardinality = SINGLE)
	Entity1 getEntity1();

	@Setter(ENTITY1)
	void setEntity1(Entity1 entity1);
}
