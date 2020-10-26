package org.openflexo.pamela.scm;

import java.util.List;

/**
 * Represents a PAMELA model as a source code
 * 
 * A {@link PamelaSCMModel} is defined from a collection of directories and a collection of head classes
 * 
 */
public class PamelaSCMModel {

	private List<PamelaEntity> entities;

	public PamelaSCMModel(List<PamelaEntity> entities) {
		this.entities = entities;
	}

	public List<PamelaEntity> getEntities() {
		return entities;
	}
}
