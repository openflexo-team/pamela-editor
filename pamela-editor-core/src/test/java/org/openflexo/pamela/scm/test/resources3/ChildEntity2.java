package org.openflexo.pamela.scm.test.resources3;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/**
 * Created by adria on 19/02/2017.
 */
@ModelEntity
public interface ChildEntity2 extends MotherEntity {
    String SOMEATTRIBUTE = "someattribute";

    @Getter(SOMEATTRIBUTE)
    ReferencedEntity getSomeAttribute();

    @Setter(SOMEATTRIBUTE)
    void setSomeAttribute(ReferencedEntity value);
}
