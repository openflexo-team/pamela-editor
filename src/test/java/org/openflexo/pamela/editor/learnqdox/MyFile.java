package org.openflexo.pamela.editor.learnqdox;

import org.openflexo.model.annotations.Getter;

public class MyFile {
	private String name;
	private int age;
	private float height;
	public MyFile(String name, int age, float height) {
		super();
		this.name = name;
		this.age = age;
		this.height = height;
	}
	
	@Getter("ooooo1")
	public String getName() {
		return name;
	}
	@Getter("ooooo2")
	public void setName(String name) {
		this.name = name;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public float getHeight() {
		return height;
	}
	public void setHeight(float height) {
		this.height = height;
	}
	
	
}
