/**
 * 
 */
package edu.buffalo.cse.irf14.analysis;

import edu.buffalo.cse.irf14.document.FieldNames;

/**
 * @author nikhillo
 * This factory class is responsible for instantiating "chained" {@link Analyzer} instances
 */
public class AnalyzerFactory {
	/**
	 * Static method to return an instance of the factory class.
	 * Usually factory classes are defined as singletons, i.e. 
	 * only one instance of the class exists at any instance.
	 * This is usually achieved by defining a private static instance
	 * that is initialized by the "private" constructor.
	 * On the method being called, you return the static instance.
	 * This allows you to reuse expensive objects that you may create
	 * during instantiation
	 * @return An instance of the factory
	 */
	
	private static AnalyzerFactory firstInstance = null;
	
	private AnalyzerFactory(){}
	
	public static AnalyzerFactory getInstance() {
		//TODO: YOU NEED TO IMPLEMENT THIS METHOD
		
		if (firstInstance == null)
		{
			firstInstance = new AnalyzerFactory();
			
		}
		return firstInstance;
		
	}
	
	/**
	 * Returns a fully constructed and chained {@link Analyzer} instance
	 * for a given {@link FieldNames} field
	 * Note again that the singleton factory instance allows you to reuse
	 * {@link TokenFilter} instances if need be
	 * @param name: The {@link FieldNames} for which the {@link Analyzer}
	 * is requested
	 * @param TokenStream : Stream for which the Analyzer is requested
	 * @return The built {@link Analyzer} instance for an indexable {@link FieldNames}
	 * null otherwise
	 */
	public Analyzer getAnalyzerForField(FieldNames name, TokenStream stream) {
		//TODO : YOU NEED TO IMPLEMENT THIS METHOD
		
		// need the class DoNothing class to return an Analyzer class which does nothing when 
		// the increment() method is called
		try
		{
			TokenFilterFactory factory = TokenFilterFactory.getInstance();
			TokenFilter filter;
		if (name == FieldNames.CONTENT | name == FieldNames.TITLE)
		{
			

			
			// thru ACCENTS FILTER
			filter = factory.getFilterByType(TokenFilterType.ACCENT, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}
			stream.reset();
			
			
			// THRU SYMBOL FILTER
			filter = factory.getFilterByType(TokenFilterType.SYMBOL, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}
			stream.reset();
			
			// THRU CAPITALIZATION FILTER
			filter = factory.getFilterByType(TokenFilterType.CAPITALIZATION, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}
			stream.reset();
			
			// THRU SPECIAL CHARS FILTER
			filter = factory.getFilterByType(TokenFilterType.SPECIALCHARS, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}
			stream.reset();
			
			
			
			
			// THRU DATES FILTER
			filter = factory.getFilterByType(TokenFilterType.DATE, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}			
			stream.reset();
			
			// THRU STOPWORDS FILTER
			filter = factory.getFilterByType(TokenFilterType.STOPWORD, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}
			stream.reset();
			
			// THRU NUMBERS FILTER
			filter = factory.getFilterByType(TokenFilterType.NUMERIC, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}
			stream.reset();			
			
			// THRU STEMMER FILTER
			filter = factory.getFilterByType(TokenFilterType.STEMMER, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}
			stream.reset();			
			
						
			
			
			
			
		}
		
		if (name == FieldNames.NEWSDATE)
		{
			// thru dates filter
			filter = factory.getFilterByType(TokenFilterType.DATE, stream);
			
			while (filter.increment()) {
				//Do nothing :/
			}			
			stream.reset();
		}
		
		} catch (TokenizerException e)
		{
			e.printStackTrace();
		}
		
		DoNothing d = new DoNothing(stream);
		
		return d;
	}
}
