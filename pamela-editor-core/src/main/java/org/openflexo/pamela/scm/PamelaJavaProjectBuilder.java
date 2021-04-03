package org.openflexo.pamela.scm;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.library.SortedClassLibraryBuilder;

public class PamelaJavaProjectBuilder extends JavaProjectBuilder {

	/**
	 * Default constructor, which will use the {@link SortedClassLibraryBuilder} implementation and add the default classloaders
	 */
	public PamelaJavaProjectBuilder() {
		super(new PamelaClassLibraryBuilder());
	}

}
