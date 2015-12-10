package org.openflexo.pamela.editor.build;



import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.annotations.AnnotationA;
import org.openflexo.pamela.editor.annotations.GetterA;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.PAMELAEntity;
import org.openflexo.pamela.editor.editer.PAMELAProperty;
import org.openflexo.pamela.editor.editer.PAMELAProperty.Cardinality;
import org.openflexo.pamela.editor.editer.exceptions.DeclaredPropertyNullException;
import org.openflexo.pamela.editor.editer.utils.UtilPAMELA;

import com.thoughtworks.qdox.model.JavaMethod;


public class EntityBuilderTest {

	@Before
	public void setUp() throws Exception {
		
	}

	@Test
	public void loadTest() {
		



	}
	
	//Test for model library
	@Test
	public void testBasicLibrary() throws Exception{
		PAMELAEntity myLibriry =  EntityBuilder.load("src/test/example/openflexotest/model", "openflexotest.model.Liberary");
		System.out.println(myLibriry);
		
		PAMELAProperty book = myLibriry.getDeclaredProperty("BOOKS"); 
		System.out.println(book.getIdentifier());
		System.out.println(book.getGetter().getCodeBlock());
		System.out.println(book.getSetter().getCodeBlock());
		System.out.println(book.getAdder().getCodeBlock());
		System.out.println(book.getRemover().getCodeBlock());

	}
	
	@Test
	public void testBasicBook() throws DeclaredPropertyNullException{
		PAMELAEntity book =  EntityBuilder.load("src/test/example/openflexotest/model", "openflexotest.model.Book");
		System.out.println(book);
		
		JavaMethod mySetter = UtilPAMELA.buildMethod("@Setter(TITLE)"+"\n"+"void newMysetter(String title,java.util.Date date);");
		System.out.println(mySetter.getCodeBlock());
		book.getDeclaredProperty("TITLE").setSetter(mySetter);
		
		JavaMethod myGetter = UtilPAMELA.buildMethod("@Getter(TITLE)"+"\n"+"private void newMygetter();");
		book.getDeclaredProperty("TITLE").setGetter(myGetter);
		
		Map<String, PAMELAProperty> propertys = book.getDeclaredProperty();
		System.out.println(propertys);
		
	}
	
	@Test
	public void testSetGetter() throws DeclaredPropertyNullException{
		GetterA getter= new GetterA("TITLE", Cardinality.LIST, "", "", true, false,"java.lang.Object","java.lang.String");
		Map<String,AnnotationA> mapGetter = new HashMap<String,AnnotationA>();
		mapGetter.put("Getter", getter);
		
		
		PAMELAEntity book =  EntityBuilder.load("src/test/example/openflexotest/model", "openflexotest.model.Book");
		System.out.println(book);
		
		
		book.getDeclaredProperty("TITLE").setGetter(mapGetter);
		
		JavaMethod setter = book.getDeclaredProperty("TITLE").getSetter();
		System.out.println(setter.getCodeBlock());
		
		
	}

}
