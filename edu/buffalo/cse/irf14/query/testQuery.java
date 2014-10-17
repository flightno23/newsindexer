package edu.buffalo.cse.irf14.query;
/**
 * custom class created by me to test out the Query and QueryParser class
 * @author Girish
 *
 */

public class testQuery{
	
	public static void main(String[] args) {
		
		Query queryObject = QueryParser.parse("(Love NOT War) AND Category:(movies NOT crime)", "OR");
		queryObject = QueryParser.parse("hello", "OR");
		queryObject = QueryParser.parse("hello world", "OR");
		queryObject = QueryParser.parse("\"hello world\"", "OR");
		queryObject = QueryParser.parse("orange AND yellow", "OR");
		queryObject = QueryParser.parse("(black OR blue) AND bruises", "OR");
		queryObject = QueryParser.parse("Author:rushdie NOT jihad", "AND");
		queryObject = QueryParser.parse("Category:War AND Author:Dutt AND Place:Baghdad AND prisoners detainees rebels", "OR");
		// System.out.println(queryObject.toString());
		
		
		
		
		
		
	}
	
}