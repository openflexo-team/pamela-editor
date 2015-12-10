package org.openflexo.pamela.editor.build;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

public class TestRegex {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		String str = "bbb<a><b><c><d>(e>aa";
		String regex = "\\<.*\\>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		System.out.println("replace: "+str.replaceAll(regex, "O"));
		while (matcher.find()) {
		  System.out.println("matcher: " + matcher.group(0));
		}
	}

}
