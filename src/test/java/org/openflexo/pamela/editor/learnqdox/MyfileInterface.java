package org.openflexo.pamela.editor.learnqdox;

import org.openflexo.model.annotations.Getter;

public interface MyfileInterface {
	String name="NAME1";

	//MyFile(String name, int age, float height);
	
	@Getter("ooooo1")
	public String getName();
	@Getter("ooooo2")
	public void setName(String name);
	public int getAge();
	public void setAge(int age);
	public float getHeight();
	public void setHeight(float height);
}
