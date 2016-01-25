package org.openflexo.pamela.editor.editer.exceptions;

public class ModelDefinitionException extends Exception {

	public ModelDefinitionException() {
	}

	public ModelDefinitionException(String message) {
		super(message);
	}

	public ModelDefinitionException(String string, ModelDefinitionException e) {
		super(string,e);
	}

}
