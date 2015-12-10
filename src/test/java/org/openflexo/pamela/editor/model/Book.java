package org.openflexo.pamela.editor.model;


import java.util.Date;

import org.openflexo.model.annotations.*;


public interface Book {

	String TITLE = "title";
	String AUTHOR = "author";
	String OUTDATE = "outdate";
	
	@Getter(TITLE)
	public String getTitle();
	
	@Setter(TITLE)
	public void setTitle(String title);
	
	@Setter(AUTHOR)
	public String setAuthor(String author);
	
	@Getter(AUTHOR)
	public String getAuthor();
	
	@Getter(OUTDATE)
	public Date getOutdate();
	
	@Setter(OUTDATE)
	public void setOutdate(Date date);
}
