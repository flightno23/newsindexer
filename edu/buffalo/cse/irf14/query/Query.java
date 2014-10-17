package edu.buffalo.cse.irf14.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Stack;


/**
 * Class that represents a parsed query
 * @author nikhillo
 *
 */
public class Query {
	// this string is for the toString Method to print
	private String userQueryFinal;
	private String userQueryToTreat;
	
	// this LinkedList holds the postfix expression to be evaluated
	private LinkedList<String> postfixList = new LinkedList<String>();
	
	ArrayList<String> arrayOfTokens;
	
	
	
	/**
	 * Method to convert given parsed query into string
	 */
	public String toString() {
		//TODO: YOU MUST IMPLEMENT THIS
		return this.userQueryFinal;
	}
	
	/**
	 * Setter method for the Final User Query
	 */
	public Query(String userQuery)
	{
		this.userQueryToTreat = userQuery;
		
		// modify this later
		this.userQueryFinal = userQuery;
		
		// create the arrayOfTokens
		this.arrayOfTokens = new ArrayList<String>();
		
		char[] queryArray = userQuery.toCharArray();
		boolean startOfPhrase = false;
		StringBuilder sb = new StringBuilder(); 
		
		for (int i=0; i<queryArray.length; i++)
		{	
			// if ( or ) then directly append to arrayOfTokens
			// if we get a closing brackets ), then add the existing stringbuilder to the array
			if (queryArray[i] == '(' || queryArray[i] == ')')
			{
				if (queryArray[i] == ')'){
					if (sb.length() > 0)
					{
						this.arrayOfTokens.add(sb.toString());
					}
					sb.setLength(0);
				}
				this.arrayOfTokens.add(Character.toString(queryArray[i]));
				
				continue;
			} 
			
			// if the start of phrase occurs, then set startOfPhrase flag to true and append to StringBuilder
			if (startOfPhrase == true && queryArray[i] == '\"'){ // signifies endOfPhrase
				startOfPhrase = false;
				sb.append(queryArray[i]);
				if (sb.length() > 0)
				{
					this.arrayOfTokens.add(sb.toString());
				}
				sb.setLength(0);
				continue;
				
			} else if (startOfPhrase == false && queryArray[i] == '\"'){ //signifies startOfPhrase
				startOfPhrase = true;
				sb.append(queryArray[i]);
				continue;
			}
			
			if (startOfPhrase == true){ // this just goes on appending characters that are part of the phrase
				sb.append(queryArray[i]);
				continue;
			}
			
			// now, check the case where the string is not a phrase, but is a word that ends when a whitespace
			// is reached, or the end of char array is reached
			if (startOfPhrase == false && queryArray[i] != ' '){
				sb.append(queryArray[i]);
			}
			
			if (startOfPhrase == false 
					&& (Character.isWhitespace(queryArray[i]) || i == (queryArray.length -1))){
				if (sb.length() > 0)
				{
				this.arrayOfTokens.add(sb.toString());
				}
				sb.setLength(0);
				
			}
		}
		
		/*for(String element:this.arrayOfTokens){
			System.out.println(element);
		}
		System.out.println(this.arrayOfTokens.size());
		System.out.println("end of arraylist");
		System.out.println(); */
		
		this.postfixList = convert2Postfix(this.arrayOfTokens);
		
		/*System.out.println("start of linked list");
		for (String post:postfixList){
			System.out.println(post);
		}*/
			
		
		
	}
	
	/**
	 * Setter method for the postfix expression
	 * @param - LinkedList<String>
	 */
	public void setPostfixList(LinkedList<String> postfixList)
	{
		this.postfixList = postfixList;
	}
	
	
	/**
	 * Getter method for getting the postfix expression structured in a linked list
	 * @return
	 */
	public LinkedList<String> getPostfixList(){
		return this.postfixList;
	}
	
	
	
	public LinkedList<String> convert2Postfix(ArrayList<String> infixExpr) { // pass the arrayList instead - arrayOfTokens
		Stack<String> stack = new Stack<String>(); 
		LinkedList<String> postfixList = new LinkedList<String>();

		for (String current : infixExpr) {
			if (isOperator(current)) { // if the character is an operator , in my case they are "(", ")", "&" and "|"
				while (!stack.isEmpty() && stack.peek().equals("(") == false ) {	// if stack is non-empty and first operator is not '('
					if (operatorGreaterOrEqual(stack.peek(), current)) {	// if operator in stack is >= in precedance, then pop and append to postfix expression
						postfixList.add(stack.pop()); // in my case, add it to the linked list
					} else {	// else, do nothing
						break;
					}
				}
				stack.push(current); // push the new operator into the stack
			} else if (current.equals("(")) {	// if the current operator is a "(", then push into stack
				stack.push(current);	
			} else if (current.equals(")")) { // if the current operator is a ")", then
				while (!stack.isEmpty() && stack.peek().equals("(") == false) {	// keep popping out operators until a ")" is reached
					postfixList.add(stack.pop());	// pop and append to linked list
				}
				if (!stack.isEmpty()) {	// if stack is non-empty, then pop() the final "(" but don't do anything with it
					stack.pop();
				}
			} else  { // else if the string is an operand, then add to linked list
				postfixList.add(current);
			}
		}
		while (!stack.empty()) { // finally, empty out the stack and add all the remaining elements to the linked list
			postfixList.add(stack.pop());
		}
		
		return postfixList;
	}
	
	private boolean isOperator(String val) {
		if (val.equals("&") || val.equals("|"))
		{
			return true;
		}
		else return false;
	}
	
	private boolean operatorGreaterOrEqual(String op1, String op2) {
		return getPrecedence(op1) >= getPrecedence(op2);
	}
	
	private int getPrecedence(String operator) {
		int ret = 0;
		if (operator.equals("|")) {
			ret = 1;
		} else if (operator.equals("&")) {
			ret = 2;
		}
		return ret;
	}
	
}