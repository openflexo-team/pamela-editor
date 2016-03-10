package org.openflexo.pamela.editor.editer;

import java.util.HashSet;
import java.util.Set;

/**
 * to resolve primitive type and some other type.. can be supplied in the future
 * 
 * @author jue
 *
 */
public class TypeConverterLibrary {

	private static final TypeConverterLibrary instance = new TypeConverterLibrary();

	public static TypeConverterLibrary getInstance() {
		return instance;
	}

	private final Set<String> converters;

	/**
	 * support some other simple type like: Data..
	 */
	public TypeConverterLibrary() {
		converters = new HashSet<String>();
		converters.add("byte");
		converters.add("short");
		converters.add("int");
		converters.add("long");
		converters.add("float");
		converters.add("double");
		converters.add("char");
		converters.add("java.lang.String");
		converters.add("boolean");
	}

	public boolean hasConverter(String fullyQualifiedName) {
		return converters.contains(fullyQualifiedName);
	}

}
