package org.openflexo.pamela.editor.model.model2;

import org.openflexo.model.annotations.Getter;
import org.openflexo.model.annotations.ModelEntity;
import org.openflexo.model.annotations.Setter;

@ModelEntity
public interface Company {

	String NAME="name";
	String SIZE = "size";
	String EMPLOYEE = "employee";
	
	@Getter(NAME)
	public String getName();
	
	@Setter(NAME)
	public void setName(String name);
	
	@Getter(SIZE)
	public int getSize();
	
	@Setter(SIZE)
	public void setSize(int size);
	
	
	
}
