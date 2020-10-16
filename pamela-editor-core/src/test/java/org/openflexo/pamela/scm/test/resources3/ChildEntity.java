package org.openflexo.pamela.scm.test.resources3;

import org.openflexo.pamela.annotations.*;

import static org.openflexo.pamela.annotations.Getter.Cardinality.SINGLE;

/**
 * Created by adria on 14/12/2016.
 */
@ModelEntity
@Imports({@Import(ImportedEntity.class)})
interface ChildEntity extends MotherEntity {
    String MYATTRIBUTE = "attribute1";

    @Getter(value = MYATTRIBUTE, cardinality = SINGLE)
    int getMyAttribute();

    @Setter(MYATTRIBUTE)
    void setMyattribute(int myAtt);
}
