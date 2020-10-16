package org.openflexo.pamela.scm.test.resources3;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;

/**
 * Created by adria on 12/02/2017.
 */
@ModelEntity
public interface MotherEntity {
    public String MOTHERATTRIBUTE = "motherattribute";

    @Getter(value = MOTHERATTRIBUTE, defaultValue = "true")
    boolean getMotherAttribute();
}
