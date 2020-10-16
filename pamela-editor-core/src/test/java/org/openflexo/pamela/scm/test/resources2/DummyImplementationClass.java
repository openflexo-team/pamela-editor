package org.openflexo.pamela.scm.test.resources2;

import java.util.List;

/**
 * Created by adria on 27/02/2017.
 */
public class DummyImplementationClass implements UltimateModelEntity {
    @Override
    public EmbeddedModelEntity getEmbedded() {
        return null;
    }

    @Override
    public void setEmbedded(EmbeddedModelEntity entity) {

    }

    @Override
    public List<EmbeddedModelEntity> getEmbeddedList() {
        return null;
    }

    @Override
    public void addEmbedded(EmbeddedModelEntity entity) {

    }

    @Override
    public void removeEmbedded(EmbeddedModelEntity entity) {

    }
}
