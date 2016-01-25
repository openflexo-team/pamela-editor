package org.openflexo.pamela.editor.editer.utils;

public class Location {
	
	private int begin;
	private int end;
	public Location(int begin, int end) {
		super();
		this.begin = begin;
		this.end = end;
	}
	public int getBegin() {
		return begin;
	}
	public void setBegin(int begin) {
		this.begin = begin;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	@Override
	public String toString() {
		return "Location [begin=" + begin + ", end=" + end + "]";
	}
	
	
}
