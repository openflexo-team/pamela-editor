package org.openflexo.pamela.scm.test.resources1;

import org.openflexo.pamela.annotations.Import;
import org.openflexo.pamela.annotations.Imports;
import org.openflexo.pamela.annotations.ModelEntity;

/**
 * Created by adria on 17/02/2017.
 */
@ModelEntity
@Imports({ @Import(BasicPamelaEntity1.class) })
public interface EntityWithImports {
}
