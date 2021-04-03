package org.openflexo.pamela.scm;

import com.thoughtworks.qdox.builder.ModelBuilderFactory;
import com.thoughtworks.qdox.builder.impl.ModelBuilder;
import com.thoughtworks.qdox.library.ClassLibrary;
import com.thoughtworks.qdox.library.SortedClassLibraryBuilder;
import com.thoughtworks.qdox.model.impl.DefaultDocletTagFactory;
import com.thoughtworks.qdox.parser.structs.MethodDef;

public class PamelaClassLibraryBuilder extends SortedClassLibraryBuilder implements ModelBuilderFactory {

	public PamelaClassLibraryBuilder() {
		appendDefaultClassLoaders();
		setModelBuilderFactory(this);
	}

	@Override
	public ModelBuilder newInstance(ClassLibrary library) {
		System.out.println("-------> Tiens on passe la !!!");
		return new ModelBuilder(library, new DefaultDocletTagFactory()) {
			@Override
			public void endMethod(MethodDef def) {
				System.out.println("Pour la methode " + def);
				System.out.println("Line: " + def.getLineNumber() + " Col: " + def.getColumnNumber() + " Dim: " + def.getDimensions());
				super.endMethod(def);
			}
		};
	}
}
