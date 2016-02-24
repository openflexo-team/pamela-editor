package org.openflexo.pamela.editor.learnqdox;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaPackage;
import com.thoughtworks.qdox.model.JavaSource;
import com.thoughtworks.qdox.model.JavaType;

public class TestQdox {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		JavaProjectBuilder builder = new JavaProjectBuilder();
		
		//File f = new File(getResource("/").getPath()); 
		//System.out.println(f); 
		
		builder.addSource(new File("E:/proS5/pamela-editor-git/pamela-editor/src/test/java/org/openflexo/pamela/editor/learnqdox/MyfileInterface.java"));
		JavaClass cls = builder.getClassByName("org.openflexo.pamela.editor.learnqdox.MyfileInterface");
		
		
		JavaPackage pkg      = cls.getPackage();            // "com.blah.foo"
		String name     = cls.getName();               // "MyClass"
		String fullName = cls.getCanonicalName(); // "com.blah.foo.MyClass";
		String canonicalName = cls.getFullyQualifiedName(); // "com.blah.foo.MyClass";
		boolean isInterface = cls.isInterface();       // false

		boolean isPublic   = cls.isPublic();   // true
		boolean isAbstract = cls.isAbstract(); // true
		boolean isFinal    = cls.isFinal();    // false

		JavaType superClass = cls.getSuperClass(); // "com.base.SubClass";
		List<JavaType> imps     = cls.getImplements(); // {"java.io.Serializable",
		                                       //  "com.custom.CustomInterface"}

		//String author = ((JavaType) cls.getTagsByName("author")).getValue(); // "joe"
		
		List<JavaMethod> methods = cls.getMethods();
		for(JavaMethod method: methods){
			List<JavaAnnotation> annotations = method.getAnnotations();
			for(JavaAnnotation a:annotations){
				System.out.println("get a annotation");
				System.out.println("typevalue:" + a.getType().getValue());
				System.out.println(a.getProperty("ignoreType").getParameterValue().toString());
				System.out.println(a.toString());
				System.out.println(method.getLineNumber());
			}
			//JavaAnnotation javaAnnotation = annotations.get(0);
			System.out.println(method.getName());
		}
		

		//JavaField nameField = cls.getFields()[0];
		//JavaMethod doStuff = cls.getMethods()[0];
		//JavaMethod getNumber = cls.getMethods()[1];

		JavaSource javaSource = cls.getParentSource();
		System.out.println("End");
		
		
		
	}

}
