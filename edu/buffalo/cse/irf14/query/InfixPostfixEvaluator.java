package edu.buffalo.cse.irf14.query;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

/**
 * Class to evaluate expressions.
 * 
 * @author Paul E. Davis (feedback@willcode4beer.com)
 */
public class InfixPostfixEvaluator {

	/**
	 * Operators in reverse order of precedence.
	 */
	private static final String operators = "-+/*";
	private static final String operands = "0123456789";

	private int getPrecedence(String operator) {
		int ret = 0;
		if (operator.equals("|")) {
			ret = 1;
		} else if (operator.equals("&")) {
			ret = 2;
		}
		return ret;
	}

	private boolean operatorGreaterOrEqual(String op1, String op2) {
		return getPrecedence(op1) >= getPrecedence(op2);
	}

	private boolean isOperator(String val) {
		if (val.equals("&") || val.equals("|"))
		{
			return true;
		}
		else return false;
	}

	private boolean isOperand(char val) {
		return operands.indexOf(val) >= 0;
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

	public int evaluatePostfix(String postfixExpr) {
		char[] chars = postfixExpr.toCharArray();
		Stack<Integer> stack = new Stack<Integer>();
		for (char c : chars) {
			if (isOperand(c)) {
				stack.push(c - '0'); // convert char to int val
			} else if (isOperator(c)) {
				int op1 = stack.pop();
				int op2 = stack.pop();
				int result;
				switch (c) {
				case '*':
					result = op1 * op2;
					stack.push(result);
					break;
				case '/':
					result = op2 / op1;
					stack.push(result);
					break;
				case '+':
					result = op1 + op2;
					stack.push(result);
					break;
				case '-':
					result = op2 - op1;
					stack.push(result);
					break;
				}
			}
		}
		return stack.pop();
	}

	public int evalInfix(String infix) {
		return evaluatePostfix(convert2Postfix(infix));
	}
	
	public static void main(String[] args){
		/*if(args.length!=1){
			System.out.println("Usage: InfixPostfixEvaluator expression");
			System.exit(2);
		}*/
		InfixPostfixEvaluator eval = new InfixPostfixEvaluator();
		// String expression = args[0];
		
		String expression = "1+2+3+4";
		String postfix = eval.convert2Postfix(expression);
		System.out.println("Postfix: " + postfix);
		System.out.println("Result: " + eval.evaluatePostfix(postfix));
	}
}