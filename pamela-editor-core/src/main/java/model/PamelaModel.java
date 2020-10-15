package model;

import java.util.List;

/**
 * Created by adria on 27/01/2017.
 */
public class PamelaModel {

    private List<PamelaEntity> entities;

    public PamelaModel(List<PamelaEntity> entities) {
        this.entities = entities;
    }

    public List<PamelaEntity> getEntities() {
        return entities;
    }
}
