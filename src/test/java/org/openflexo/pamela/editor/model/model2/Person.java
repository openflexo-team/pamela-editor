package org.openflexo.pamela.editor.model.model2;

import org.openflexo.model.annotations.Getter;
import org.openflexo.model.annotations.ModelEntity;
import org.openflexo.model.annotations.Setter;

@ModelEntity
public interface Person {

	String NAME="name";
	
	@Getter(NAME)
	public String getName();
	
	
	@Setter(NAME)
	public void setName(String name);
}
