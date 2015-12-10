package org.openflexo.pamela.editor.learnqdox;

import java.io.File;
import java.io.IOException;

import org.openflexo.pamela.editor.model.Book;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;


public class TestQdoxClassLoader {
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		JavaProjectBuilder builder = new JavaProjectBuilder();
		String a;
		int num;
		
		//File f = new File(getResource("/").getPath()); 
		//System.out.println(f); 
		Book b;
		
		//builder.addSource(new File("src/main/java/model/Book.java"));
		builder.addSourceFolder( new File( "src" ) );
		JavaClass cls = builder.getClassByName("openflexotest.model.Book");
		System.out.println(cls.getName());
		for(JavaMethod m :cls.getMethods()){
			System.out.println(m.getName());
		}
		

		System.out.println("End");
		
		
		
	}
}
