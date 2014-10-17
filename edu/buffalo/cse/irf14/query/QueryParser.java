/**
 * 
 */
package edu.buffalo.cse.irf14.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nikhillo
 * Static parser that converts raw text to Query objects
 */
public class QueryParser {
	/**
	 * MEthod to parse the given user query into a Query object
	 * @param userQuery : The query to parse
	 * @param defaultOperator : The default operator to use, one amongst (AND|OR)
	 * @return Query object if successfully parsed, null otherwise
	 */
	public static Query parse(String userQuery, String defaultOperator) {
		//TODO: YOU MUST IMPLEMENT THIS METHOD
		
		// CODE to check CASE 7 and 8 of specs
		Boolean isFacetedQuery = false;
		String[] splitString = userQuery.split(" AND ");
		
		// cycle through splitString array to check for faceted queries
		for (String token:splitString)
		{
			
			if (token.trim().matches("(((Author:)|(Category:)|(Place:)|(Term:))[0-9A-Za-z]+)")){
				isFacetedQuery = true;
				break;
			}
		}
		
		if (isFacetedQuery){	
			StringBuilder tempBuild = new StringBuilder();
			// check case 7 and edit
			for (int i=0; i<splitString.length; i++)
			{	
				if (isFacetedQuery == true && splitString[i].matches("[0-9A-Za-z\\s]+\\s[0-9A-Za-z\\s]+")){
					splitString[i] = "(" + splitString[i] + ")";
					tempBuild.append(splitString[i]);
					break;
				}
				tempBuild.append(splitString[i]);
				
				if (i != splitString.length-1)	// bug fix, only append when in middle of userQuery
				tempBuild.append(" AND ");
			}
			
			userQuery = tempBuild.toString();
		}
		
		// CHECKING CASE 8 TYPE QUERIES
		StringBuffer sbtemp = new StringBuffer();
		
		// to locate words of the form Category:(A AND B) -> (Category:A AND Category:B) (CASE 8)
		/*
		 * find any word of the form with regex Category:\((.+)\)
		 */
		Pattern pcase8 = Pattern.compile("(?:((Author:)|(Category:)|(Place:)|(Term:))\\((?:.+)\\))");
		Matcher mcase8 = pcase8.matcher(userQuery);
		
		while (mcase8.find()){
			String repString = null;
			String finalString = "";
			Pattern tempPattern = Pattern.compile("[0-9A-Za-z!|&]+");
			Matcher tempMatcher = tempPattern.matcher(mcase8.group());
			while (tempMatcher.find()){
				if (tempMatcher.group().matches("(Author)|(Category)|(Place)|(Term)|(AND)|(OR)|(NOT)") == false){
					repString = mcase8.group(1) + tempMatcher.group();
					finalString = finalString + repString + " ";
				} else if (tempMatcher.group().matches("(AND)|(OR)|(NOT)")){
					finalString = finalString + tempMatcher.group() + " ";
				}
			}
			finalString = "(" + finalString.trim() + ")";
			mcase8.appendReplacement(sbtemp, finalString);
			
		}
		
		mcase8.appendTail(sbtemp);
		userQuery = sbtemp.toString();
		
		
		sbtemp = null;
		
		// CODE to handle DEFAULT operators		
		String temp1, temp2;
		String[] tokens = userQuery.split("\\s");
		ArrayList<String> treatedTokens = new ArrayList<String>();
		Boolean startOfPhrase = false;
		
		
		// System.out.println(Arrays.toString(tokens));
		
		for(int i=0; i<tokens.length ; i++)
		{
			if (i == tokens.length - 1)
			{
				treatedTokens.add(tokens[i]);
				break;
			}
			
			
			temp1 = tokens[i];
			temp2 = tokens[i+1];
			
			// checks for start of phrase and end of phrase
			if(startOfPhrase == false && temp1.contains("\""))
			{
				startOfPhrase = true;
				treatedTokens.add(temp1);
				continue;
			}
			else if(startOfPhrase == true && temp1.charAt(temp1.length()-1) == '\"')
			{
				startOfPhrase = false;
			}
			
			
			if(startOfPhrase == true)
			{
				treatedTokens.add(temp1);
				continue;
			}
			
			
			
			
			
			// if any one of the tokens are "OR" or "AND" then do nothing
			if(temp1.equals("OR") || temp1.equals("AND") || temp2.equals("OR") || temp2.equals("AND")
					|| temp1.equals("NOT") || temp1.equals("(") || temp2.equals(")"))
			{
				treatedTokens.add(temp1);
			}
			else
			{	
				treatedTokens.add(temp1);
				treatedTokens.add(defaultOperator);
			}
		}
		
		
		StringBuilder s = new StringBuilder();
		for (String token:treatedTokens)
		{
			s.append(token);
			s.append(" ");
		}
		
		
		// overwrite userQuery to include default operators now
		userQuery = s.toString().trim();
		
		
		
		// converting AND -> & and OR -> | and NOT -> !
		userQuery = userQuery.replaceAll("AND", "&");
		userQuery = userQuery.replaceAll("OR", "|");
		userQuery = userQuery.replaceAll("NOT\\s", "!");
		
		// System.out.println(userQuery);
		
		String rx1 = "([0-9A-Za-z:~\"]+)";
		String rx2 = "(Author:)|(Category:)|(Place:)|(Term:)";
		String rx3 = "(\".+\")";
		
		StringBuffer sb = new StringBuffer();
		
		Pattern p3 = Pattern.compile(rx3);
		Pattern p1 = Pattern.compile(rx1);
		
		Matcher m3 = p3.matcher(userQuery);
		
		Pattern p2 = Pattern.compile(rx2);
		Matcher m2;
		
		// FIND PHRASES AND REPLACE THE SPACE IN B/W WITH ~
		while(m3.find()){
			String repString;
			repString = m3.group(1).replaceAll("\\s", "~");
			if(repString != null){
				m3.appendReplacement(sb, repString);
			}
		}
		
		m3.appendTail(sb);
		userQuery = sb.toString();
		
		sb = new StringBuffer();
		
		// to locate words of the form Category:(A AND B) -> (Category:A AND Category:B) (CASE 8)
		/*
		 * find any word of the form with regex Category:\((.+)\)
		 */
		Pattern p4 = Pattern.compile("(?:((Author:)|(Category:)|(Place:)|(Term:))\\((?:.+)\\))");
		Matcher m4 = p4.matcher(userQuery);
		
		while (m4.find()){
			String repString = null;
			String finalString = "";
			Pattern tempPattern = Pattern.compile("[0-9A-Za-z!|&]+");
			Matcher tempMatcher = tempPattern.matcher(m4.group());
			while (tempMatcher.find()){
				if (tempMatcher.group().matches("(Author)|(Category)|(Place)|(Term)") == false){
					repString = m4.group(1) + tempMatcher.group();
					finalString = finalString + repString + " ";
				} else if (tempMatcher.group().matches("(AND)|(OR)|(NOT)")){
					finalString = finalString + tempMatcher.group();
				}
			}
			finalString = "(" + finalString.trim() + ")";
			m4.appendReplacement(sb, finalString);
			
		}
		
		m4.appendTail(sb);
		userQuery = sb.toString();
		

		
		sb = new StringBuffer();
		// ADDING A DEFAULT Term: to all the terms that dont have a index before it
		Matcher m1 = p1.matcher(userQuery);
		
		while(m1.find())
		{	
			// if Author, Place, etc already found in word, then do nothing
			String repString = null;
			m2 = p2.matcher(m1.group(1));
			if(m2.find() == true)
			{
				
			} 
			else{
				// word encountered, add the default "Term:" to signify we need to search the term index
				repString = "Term:" + m1.group(1);
				if (repString.contains("\"")) repString = repString.replaceAll("~", " ");
			}
			
			if(repString != null) m1.appendReplacement(sb, repString);
			
		}
		m1.appendTail(sb);
		
		// Updating userquery with "Term:" added to words
		userQuery = sb.toString();
		// System.out.println(userQuery);
		
		// code to convert !Term (NOT) into <Term>
		rx1 = "!([0-9A-Za-z:]+)";
		
		sb = new StringBuffer();
		
		p1 = Pattern.compile(rx1);
		m1 = p1.matcher(userQuery);
		
		while(m1.find()){
			String repString = null;
			repString = "<" + m1.group(1) + ">";
			
			if(repString != null) m1.appendReplacement(sb, repString);
				
		}
		m1.appendTail(sb);
		
		userQuery = sb.toString();
		userQuery = userQuery.replaceAll("~", " ");
		System.out.println(userQuery);
		
		// setting the final user query after all the processing, Query will take over from here on
		Query queryObject = new Query(userQuery);
		
		// stub for now
		return queryObject;
		
	}
	
	
	
	
	
	
	
	
	
	
	
}