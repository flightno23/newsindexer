/**
 * 
 */
package edu.buffalo.cse.irf14.analysis;

import java.util.Arrays;

/**
 * @author nikhillo
 * Class that converts a given string into a {@link TokenStream} instance
 */
public class Tokenizer {
	// class variable that holds the delim to be used for token creation
	private String delim;
	/**
	 * Default constructor. Assumes tokens are whitespace delimited
	 */
	public Tokenizer() {
		//TODO : YOU MUST IMPLEMENT THIS METHOD
		this.delim = "\\s+";
	}
	
	/**
	 * Overloaded constructor. Creates the tokenizer with the given delimiter
	 * @param delim : The delimiter to be used
	 */
	public Tokenizer(String delim) {
		//TODO : YOU MUST IMPLEMENT THIS METHOD
		if (delim.matches("[\\^\\*\\$\\.\\|\\?\\*\\+\\(\\)\\[\\{]"))
		{
			this.delim = "\\" + delim + "+";
		}
		else
		{
			this.delim = delim;
		}
		
	}
	
	/**
	 * Method to convert the given string into a TokenStream instance.
	 * This must only break it into tokens and initialize the stream.
	 * No other processing must be performed. Also the number of tokens
	 * would be determined by the string and the delimiter.
	 * So if the string were "hello world" with a whitespace delimited
	 * tokenizer, you would get two tokens in the stream. But for the same
	 * text used with lets say "~" as a delimiter would return just one
	 * token in the stream.
	 * @param str : The string to be consumed
	 * @return : The converted TokenStream as defined above
	 * @throws TokenizerException : In case any exception occurs during
	 * tokenization
	 */
	public TokenStream consume(String str) throws TokenizerException {
		//TODO : YOU MUST IMPLEMENT THIS METHOD
		// splitting the String str using the delimiter
		
		if (str == null || str.equals(""))
		{	
			throw new TokenizerException();
		}
		
		String[] tokens = str.split(delim);
		
		TokenStream stream = new TokenStream();
		for (String tempToken: tokens)
		{	
			if (tempToken.trim() != "") {
				stream.setToken(tempToken);
			}
			else
			{
				continue;
			}
			
		}
		
		stream.initIterator();
		return stream;
		
	}
}
