package org.openflexo.pamela.editor.editer.utils;

public class UtilString {

	/**
	 * split classname and remove .class in xxx.xxx.class string 
	 * 
	 * @param clsnameWithDot
	 * @return class name without .class
	 */
	public static String removeDotClassInClassname(String clsnameWithDot){
		String[] impclsnamesplit = clsnameWithDot.split("\\.");
		StringBuilder sb = new StringBuilder();
		int offset = impclsnamesplit.length - 1;
		for(int i=0;i<offset;i++){
			if(i==offset-1)
				sb.append(impclsnamesplit[i]);
			else
				sb.append(impclsnamesplit[i]).append(".");
		}
		return sb.toString();
	}
	
}
