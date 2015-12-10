package org.openflexo.pamela.editor.model;

import java.util.List;

import org.openflexo.model.annotations.Adder;
import org.openflexo.model.annotations.Getter;
import org.openflexo.model.annotations.Getter.Cardinality;
import org.openflexo.model.annotations.Remover;
import org.openflexo.model.annotations.Setter;


public interface Liberary {

	String BOOKS = "books";

	@Getter(value= BOOKS ,cardinality = Cardinality.LIST )
	public List<Book> getBooks();

	@Adder(BOOKS)
	public void addBook(Book book);

	@Setter(BOOKS)
	public void setBooks(List<Book> books);

	@Remover(BOOKS)
	public void removeFromBooks(Book book);


}
