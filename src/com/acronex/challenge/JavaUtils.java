package com.acronex.challenge;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaUtils {

	private static final Pattern doubleQuotePattern = Pattern.compile("\"");
	private static final Pattern ampPattern = Pattern.compile("&");
	private static final Pattern brackOpenQuotePattern = Pattern.compile("<");
	private static final Pattern brackCloseQuotePattern = Pattern.compile(">");
	

	public static String escapeSpecialCharacters(String s) {
		if(s!=null && s.length()>0){
			s = doubleQuotePattern.matcher(s).replaceAll("&quot");
			s = ampPattern.matcher(s).replaceAll("&amp;");
			s = brackOpenQuotePattern.matcher(s).replaceAll("&lt;");
			s = brackCloseQuotePattern.matcher(s).replaceAll("&gt;");
		}
		return s;
	}

	    
}
