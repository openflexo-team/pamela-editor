package org.openflexo.pamela.editor.build;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.editor.builder.EntityBuilder;
import org.openflexo.pamela.editor.editer.exceptions.DeclaredPropertyNullException;


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
		/*
		PAMELAEntity myLibriry =  EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model", "org.openflexo.pamela.editor.model.Liberary");
		System.out.println(myLibriry);
		
		PAMELAProperty book = myLibriry.getDeclaredProperty("BOOKS"); 
		System.out.println(book.getIdentifier());
		System.out.println(book.getGetter().getCodeBlock());
		System.out.println(book.getSetter().getCodeBlock());
		System.out.println(book.getAdder().getCodeBlock());
		System.out.println(book.getRemover().getCodeBlock());
		*/
	}
	
	@Test
	public void testBasicBook() throws DeclaredPropertyNullException{
		/*
		PAMELAEntity book =  EntityBuilder.load("src/test/example/openflexotest/model", "openflexotest.model.Book");
		System.out.println(book);
		
		JavaMethod mySetter = UtilPAMELA.buildMethod("@Setter(TITLE)"+"\n"+"void newMysetter(String title,java.util.Date date);");
		System.out.println(mySetter.getCodeBlock());
		book.getDeclaredProperty("TITLE").setSetter(mySetter);
		
		JavaMethod myGetter = UtilPAMELA.buildMethod("@Getter(TITLE)"+"\n"+"private void newMygetter();");
		book.getDeclaredProperty("TITLE").setGetter(myGetter);
		
		Map<String, PAMELAProperty> propertys = book.getDeclaredProperty();
		System.out.println(propertys);
		*/
	}
	
	@Test
	public void testSetGetter() throws DeclaredPropertyNullException{
		/*
		GetterA getter= new GetterA("TITLE", Cardinality.LIST, "", "", true, false,"java.lang.Object","java.lang.String");
		Map<String,AnnotationA> mapGetter = new HashMap<String,AnnotationA>();
		mapGetter.put("Getter", getter);
		
		
		PAMELAEntity book =  EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model", "");
		System.out.println(book);
		
		
		book.getDeclaredProperty("TITLE").setGetter(mapGetter);
		
		JavaMethod setter = book.getDeclaredProperty("TITLE").getSetter();
		System.out.println(setter.getCodeBlock());		
		*/
	}

	@Test
	public void testBuilder1(){
		//EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model","");
	}
	
	@Test
	public void testBuilder2SingleEntry(){
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model2","org.openflexo.pamela.editor.model.model2.Liberary");
	}
	
	@Test
	public void testBuilder3SingleEntry(){
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model/model1","org.openflexo.pamela.editor.model.model1.FlexoProcess");
	}
	
	@Test
	public void testBuilder4MuiltiEntry(){
		String[] classNames = {"org.openflexo.pamela.editor.model.model2.Liberary","org.openflexo.pamela.editor.model.model2.Company"};
		EntityBuilder.load("src/test/java/org/openflexo/pamela/editor/model",classNames);
	}
	
	@After
	public void testAfter(){
		EntityBuilder.entityLibrary.printAllEntities();
	}
	
}
