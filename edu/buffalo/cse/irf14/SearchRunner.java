package edu.buffalo.cse.irf14;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import edu.buffalo.cse.irf14.analysis.*;
import edu.buffalo.cse.irf14.document.FieldNames;
import edu.buffalo.cse.irf14.index.*; 
import edu.buffalo.cse.irf14.query.QueryParser;
import edu.buffalo.cse.irf14.query.Query;

/**
 * Main class to run the searcher.
 * As before implement all TODO methods unless marked for bonus
 * @author nikhillo
 *
 */
public class SearchRunner {
	public enum ScoringModel {TFIDF, OKAPI};
	
	// Various Linked Lists for manipulation
	public LinkedList<Result> finalList;
	
	// Declaring the various lookup dictionaries needed for indexing process
	public HashMap<String, Integer> fileIDDictionary;
	public HashMap<Integer, String> inverseFileIDDictionary;
	public HashMap<String, Integer> termDictionary;
	public HashMap<String, Integer> categoryDictionary;
	public HashMap<String, Integer> placeDictionary;
	public HashMap<String, Integer> authorDictionary;
	public HashMap<Integer, Integer> docLengthDictionary;
	
	// Declaring the various indexes needed
	public HashMap<Integer, LinkedList<Postings>> termIndex;
	public HashMap<Integer, LinkedList<Postings>> categoryIndex;
	public HashMap<Integer, LinkedList<Postings>> authorIndex;
	public HashMap<Integer, LinkedList<Postings>> placeIndex;
	private String defaultOperator;
	private HashMap<String, Double> normalizedIndex;
	
	ArrayList<DocDetails> docsRelevancy;
	
	String corpusDir;
	PrintStream stream;
	ArrayList<eModeRead> eModeInput;
	boolean eModeTrue = false;
	long startTime;
	long endTime;
	
	
	/**
	 * Default (and only public) constuctor
	 * @param indexDir : The directory where the index resides
	 * @param corpusDir : Directory where the (flattened) corpus resides
	 * @param mode : Mode, one of Q or E
	 * @param stream: Stream to write output to
	 */
	public SearchRunner(String indexDir, String corpusDir, 
			char mode, PrintStream stream) {
		//TODO: IMPLEMENT THIS METHOD
		this.startTime = System.currentTimeMillis();
		this.corpusDir = corpusDir;
		this.stream = stream;
		// CODE TO READ STUFF FROM THE FILE TO BUILD THE INDEXES
		try
		{
			// READING THE TERM INDEX AND SUPPORTING DICTIONARY
			FileInputStream fis = new FileInputStream(indexDir + File.separator + "termIndex.ser");
	        ObjectInputStream ois = new ObjectInputStream(fis);
	        this.termIndex = (HashMap<Integer, LinkedList<Postings>>) ois.readObject();
	        this.termDictionary = (HashMap<String, Integer>) ois.readObject();
	        
	        fis.close();
	        ois.close();
	        
	        // READING THE CATEGORY INDEX AND SUPPORTING DICTIONARY
	        fis = new FileInputStream(indexDir + File.separator + "categoryIndex.ser");
	        ois = new ObjectInputStream(fis);
	        this.categoryIndex = (HashMap<Integer, LinkedList<Postings>>) ois.readObject();
	        this.categoryDictionary = (HashMap<String, Integer>) ois.readObject();
	        
	        fis.close();
	        ois.close();

	        // READING THE place INDEX AND SUPPORTING DICTIONARY
	        fis = new FileInputStream(indexDir + File.separator + "placeIndex.ser");
	        ois = new ObjectInputStream(fis);
	        this.placeIndex = (HashMap<Integer, LinkedList<Postings>>) ois.readObject();
	        this.placeDictionary = (HashMap<String, Integer>) ois.readObject();
	        
	        fis.close();
	        ois.close();

	        // READING THE author INDEX AND SUPPORTING DICTIONARY
	        fis = new FileInputStream(indexDir + File.separator + "authorIndex.ser");
	        ois = new ObjectInputStream(fis);
	        this.authorIndex = (HashMap<Integer, LinkedList<Postings>>) ois.readObject();
	        this.authorDictionary = (HashMap<String, Integer>) ois.readObject();
	  
	        fis.close();
	        ois.close();
	        
	        // FINALLY READING THE FILEID, INVERSEFILEID, DOCLENGTH 
	        fis = new FileInputStream(indexDir + File.separator + "fileIDDict.ser");
	        ois = new ObjectInputStream(fis);
	        this.fileIDDictionary = (HashMap<String, Integer>) ois.readObject();
	        this.inverseFileIDDictionary = (HashMap<Integer, String>) ois.readObject();
	        this.docLengthDictionary = (HashMap<Integer, Integer>) ois.readObject();
	        this.normalizedIndex = (HashMap<String, Double>) ois.readObject();
	        
	        fis.close();
	        ois.close();
	        
	        
	        
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to execute given query in the Q mode
	 * @param userQuery : Query to be parsed and executed
	 * @param model : Scoring Model to use for ranking results
	 */
	public void query(String userQuery, ScoringModel model) {
		//TODO: IMPLEMENT THIS METHOD
		// Function that chooses the default Operator based on the query
		this.eModeTrue = false;
		this.defaultOperator = "OR"; // change this later
		this.defaultOperator = chooseDefault(userQuery);
		
		
		// Get the query object converted to postfix form
		Query q;
		q = QueryParser.parse(userQuery, this.defaultOperator);
		q.toString();
		
		// get the postfix expression and create a set of Result objects based on it
		LinkedList<String> postfixExpr = q.getPostfixList();
		treatQuery(postfixExpr);	// treats the query and produces LinkedList<Results> finalList ready for stack evaluation							
		
		if (model == ScoringModel.TFIDF) {
			evaluateWithTFIDF();
			// evaluate and print to stream
			snippetsGeneration(userQuery);
		} else if (model == ScoringModel.OKAPI) {
			evaluateWithOkapi();
			// evaluate and print to stream
			snippetsGeneration(userQuery);
		}
		
		
		
		
		
		
	}
	/**
	 * METHOD TO COMPUTE THE QUERY MATRIX FOR VSM
	 * @return hashmap of form term:[weight, index]
	 */
	public HashMap<String, ArrayList<Double>> computeQueryMatrixVSM() {
		// before evaluating the query create the query matrix and finally create the document matrix in the next step
				double numberOfDocs = this.fileIDDictionary.size();
				
				HashMap<String, Double> tfTable = new HashMap<String, Double>();
				
				// first creating the term:tf hashmap
				for (Result r:this.finalList) {
					if (r.isOperator == false) {	// if the Result Object is a term
						String term = r.term;
						if (tfTable.containsKey(term)) {
							tfTable.put(term, tfTable.get(term)+1); // increment the tf
						} else {
							tfTable.put(term, 1d);	// set tf as 1
						}
					}
				}
				
				
				
				
				HashMap<String, ArrayList<Double>> perTermMap = new HashMap<String, ArrayList<Double>>();
				ArrayList<Double> doubleList;
				
				
				
				for (Result r:this.finalList) {		// Scan through list again and generate other valuse of query
					if (r.isOperator == false) {	// if a TERM
						String term = r.term;
						// GETTING the df of the term and finally the idf
						int mapID;
						double df = 1;
						double idf;
						double termFreq;
						double weight;
						
						// skip if term already read
						if (perTermMap.containsKey(term)){
							continue;
						}
						
						// in case key wasn't already processed calculate the df,idf and weight
						if (r.index == ResultType.IndexType.AUTHOR) {
							
							if (this.authorDictionary.containsKey(term)) {
								mapID = this.authorDictionary.get(term);	// get the term ID
								df = this.authorIndex.get(mapID).size();	// get the df of the term
								idf = Math.log(numberOfDocs / df) / Math.log(2);	// calculating the idf as log (N/df)
								termFreq = (1 + (Math.log(tfTable.get(term)) / Math.log(2)));	// calculating the tf as (1 + log(tf))
								weight = termFreq * idf;
								doubleList = new ArrayList<Double>();;
								doubleList.add(weight);
								doubleList.add(tfTable.get(term));
								doubleList.add(idf);
								perTermMap.put(term, doubleList);
							} 
							
						} else if (r.index == ResultType.IndexType.CATEGORY) {
							
							if (this.categoryDictionary.containsKey(term)) {
								mapID = this.categoryDictionary.get(term);
								df = this.categoryIndex.get(mapID).size();
								idf = Math.log(numberOfDocs / df) / Math.log(2);
								termFreq = (1 + ( Math.log(tfTable.get(term)) / Math.log(2) ));
								weight = termFreq * idf;
								doubleList = new ArrayList<Double>();;
								doubleList.add(weight);
								doubleList.add(tfTable.get(term));
								doubleList.add(idf);
								perTermMap.put(term, doubleList);
								
							} 
							
						} else if (r.index == ResultType.IndexType.PLACE) {
							
							if (this.placeDictionary.containsKey(term)) {
								mapID = this.placeDictionary.get(term);
								df = this.placeIndex.get(mapID).size();
								idf = Math.log(numberOfDocs / df) / Math.log(2);
								termFreq = (1 + ( Math.log(tfTable.get(term)) / Math.log(2) ));
								weight = termFreq * idf;
								doubleList = new ArrayList<Double>();;
								doubleList.add(weight);
								doubleList.add(tfTable.get(term));
								doubleList.add(idf);
								perTermMap.put(term, doubleList);
								
							} 
							
						} else if (r.index == ResultType.IndexType.TERM) {
							
							if (this.termDictionary.containsKey(term)) {
								mapID = this.termDictionary.get(term);
								df = this.termIndex.get(mapID).size();
								idf = Math.log(numberOfDocs / df) / Math.log(2);
								termFreq = (1 + ( Math.log(tfTable.get(term)) / Math.log(2) ));
								weight = termFreq * idf;
								doubleList = new ArrayList<Double>();;
								doubleList.add(weight);
								doubleList.add(tfTable.get(term));
								doubleList.add(idf);
								perTermMap.put(term, doubleList);
								
							} 
							
						}
					}
					
					
				}	// END OF FOR LOOP TO CREATE QUERY MATRIX
				return perTermMap;
	}
	
	public void snippetsGeneration(String userQuery){
		BufferedReader getInfo;
		DocDetails doc;
		StringBuffer sb;
		StringBuffer store = new StringBuffer();
		try{
			
		store.append("Query: " + userQuery);
		
		for (int i=0; i < this.docsRelevancy.size(); i++){	// for loop that iterates through the first 10 elements
			if (i == 10){	// get at the max only 10 results
				break;
			}
			store.append('\n');
			int rank = i + 1;
			store.append("RANK: " + rank);
			store.append('\n');
			
			sb = new StringBuffer();
			doc = this.docsRelevancy.get(i);
			store.append("FILE ID: " + doc.fileID);
			store.append('\n');
			
			getInfo = new BufferedReader(new FileReader(corpusDir+File.separator+doc.fileID));
			String temp;
			while ((temp = getInfo.readLine()) != null)
			{
				if(temp.equals(""))
				{
					continue;
				}
				else
				{	
					temp = temp + " ";
					sb.append(temp);
				}
			}
			getInfo.close();
			// Obtaining the TITLE of the file
			int indexPos = 0;
			Object[] titleInfo = new Object[2];
			titleInfo = regexTITLE("([^a-z]+)\\s{2,}", sb.toString());
			
			// NEW CODE CHANGE -- REMOVE IF CAUSES PROBLEMS
			if (titleInfo[0].toString().equals("")  & sb.toString().toLowerCase().contains("blah blah"))
			{	
				Pattern checkRegex = Pattern.compile("[^a-z]+");
		    	Matcher regexMatcher = checkRegex.matcher(sb.toString());
		    	
		    	if (regexMatcher.find() == true)
		    	{
		    		titleInfo[0] = regexMatcher.group(); 
		    		titleInfo[1] = regexMatcher.end();
		    		
		    	}
		    	
			}
			
	    	
			String snippetTitle = titleInfo[0].toString().trim();
			indexPos = (Integer) titleInfo[1];
			
			// Getting the Author and Author Org
			String[] authorList;
			Object[] resultAuthor = {null, null, null};
			resultAuthor = regexAuthor("<AUTHOR>(.*)</AUTHOR>", sb.toString());
		    
		    if ((Integer) resultAuthor[2] != 0)
		    {
		    	indexPos = (Integer) resultAuthor[2];
		    }
		    
		 // Getting the Place and Date
 			//resultPlaceDate = regexPlaceDate("\\s{2,}(.+),\\s(?:([A-Z][a-z]+\\s[0-9]{1,})\\s{1,}-)", news.toString());
		    Object[] resultPlaceDate = {null, null, null};
		    
		    resultPlaceDate = regexPlaceDate("\\s{2,}(.+),\\s{1,}(?:((?i)(jan|january|feb|february|mar|march|apr|april|may|june|july|aug|august|sep|september|oct|october|nov|november|dec|december)\\s{1,}[0-9]{1,})\\s{1,}-)", sb.toString());
 			
 			String snippetContent = "";
 		    // getting the content
 		    
 		    if ((Integer) resultPlaceDate[2] != 0)
		    {
		    	indexPos = (Integer) resultPlaceDate[2];
		    }
 		    
 		    if (indexPos >= sb.length() - 1)
 		    {
 		    	// defensive programming
 		    }
 		    else
 		    {
 		    snippetContent = sb.substring(indexPos + 1);
 		    }
 		    
 		    store.append("TITLE: " + snippetTitle);
			store.append('\n');
			
 		    if (snippetContent.length() > 280){
 		    	store.append("CONTENT: ..." + snippetContent.substring(0, 280) + "...");
 				store.append('\n');
 		    } else {
 		    	store.append("CONTENT: ..." + snippetContent);
 				store.append('\n');
 		    }
 		   store.append("RELEVANCE SCORE: " + doc.score);
 		   store.append('\n');
 		   
 		   
			
		}
		this.endTime = System.currentTimeMillis();
		
	   long queryTime = this.endTime - this.startTime;
	   
		   
	   stream.println("Query Time: " + queryTime + " ms");
	   stream.println(store.toString());
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// METHOD that returns the result of treating string with regex and index position
	public static Object[] regexAuthor(String theRegex, String str2Check)
	{
		
		Object[] authorInfo = {null, null, 0};
    	String[] authorData = {null,null};
		Pattern checkRegex = Pattern.compile(theRegex);
    	Matcher regexMatcher = checkRegex.matcher(str2Check);
    	
    	if (regexMatcher.find() == true)
    	{
    		if (regexMatcher.group().length() != 0)
	    	{
	    		if (regexMatcher.group(1).contains(","))
				{
					authorData = regexMatcher.group(1).trim().split(",");
					authorData[1] = authorData[1].trim();
				}
				else
				{
					authorData[0] = regexMatcher.group(1);
				}
	    		authorInfo[2] = regexMatcher.end();
	    		
	    	
	    		authorInfo[0] = authorData[0].replaceFirst("(?i)by","").trim();
	    		authorInfo[1] = authorData[1];
	    	
	    	}
    	}
    	
    	return authorInfo;
	}
	
	// Get the TITLE and the index of the expression
	public static Object[] regexTITLE(String theRegex, String str2Check)
	{
		Pattern checkRegex = Pattern.compile(theRegex);
		Matcher regexMatcher = checkRegex.matcher(str2Check);
		int titleTemp;
		
		// declaring and initializing the return type
		Object[] regexInfo = new Object[2];
		regexInfo[0] = "";
		regexInfo[1] = 0;
		
		if (regexMatcher.find() == true)
		{
			if (regexMatcher.group().length() != 0)
	    	{	
	    		if (regexMatcher.group(1).contains("<AUTHOR>"))
	    		{
	    			titleTemp = regexMatcher.group(1).indexOf("<AUTHOR>");
	    			regexInfo[0] = regexMatcher.group(1).substring(0, titleTemp).trim();
	        		regexInfo[1] = regexMatcher.end();	
	    		}
	    		else
	    		{
	    			regexInfo[0] = regexMatcher.group(1).trim();
	        		regexInfo[1] = regexMatcher.end();
	    		}
	    		
	    	}
		}
		
		
		return regexInfo;
		
	}
	
	// regex output for PLACE and NEWS DATE
	public static Object[] regexPlaceDate(String theRegex, String str2Check)
	{
		
		Object[] placeDateInfo = {null, null, 0};
		Pattern checkRegex = Pattern.compile(theRegex);
    	Matcher regexMatcher = checkRegex.matcher(str2Check);
    	int placeTemp;
    	
    	if (regexMatcher.find() == true)
		{
    		if (regexMatcher.group().length() != 0)
        	{
    			if (regexMatcher.group(1).contains("</AUTHOR>"))
    			{
    				placeTemp = regexMatcher.group(1).indexOf("</AUTHOR>");
        			placeDateInfo[0] = regexMatcher.group(1).substring(placeTemp+10).trim();
    			}
    			else
    			{
    				placeDateInfo[0] = regexMatcher.group(1).trim();
    			}
        		
        		placeDateInfo[1] = regexMatcher.group(2).trim();
        		placeDateInfo[2] = regexMatcher.end();
        	
        	}
		}
    	
    	
    	return placeDateInfo;
	}
	
	/**
	 * Evaluating the given boolean expression with TFIDF
	 */
	public void evaluateWithTFIDF() {
				// computes
				HashMap<String, ArrayList<Double>> queryMatrix = computeQueryMatrixVSM();
				
				
				
				// finally evaluate the query and generate the final merged postings list that contain terms in the 
				// query given
				evaluateQuery();
				Result finalResult = this.finalList.get(0);
				this.docsRelevancy = new ArrayList<DocDetails>();
				
				
				for (Postings documents: finalResult.postingsList) {
					Map<String, ArrayList<Integer>> termsMap = documents.getTfMap(); 	// get the term: tf matrix of the document
					double euclidNorm = 0d;	
					double finalEuclidNorm;
					for (Map.Entry<String, ArrayList<Integer>> entry: termsMap.entrySet()) {	
						euclidNorm += Math.pow(entry.getValue().size(), 2d);
					}
					
					String fileID = this.inverseFileIDDictionary.get(documents.getFileID());	// get the fileID of the document
					double weight = this.normalizedIndex.get(fileID);
					double lengthDoc = this.docLengthDictionary.get(documents.getFileID());
					
					finalEuclidNorm = Math.sqrt(euclidNorm);
					Double scoreDoc = 0d;
					Double finalScore;
					Double normalQuery = 0d;
					for (Map.Entry<String, ArrayList<Integer>> entry: termsMap.entrySet()) {	// iterating through the key set again
						ArrayList<Double> queryInfo = queryMatrix.get(entry.getKey());	// get the queryinfo by doing a lookup on term
						Double weightQuery = queryInfo.get(0);	// get the weight of query
						normalQuery += Math.pow(weightQuery, 2);
					}
					normalQuery = Math.sqrt(normalQuery);
					
					// Now, to compare and multiply weights to compute VSM weight
					for (Map.Entry<String, ArrayList<Integer>> entry: termsMap.entrySet()) {	// iterating through the key set again
						ArrayList<Double> queryInfo = queryMatrix.get(entry.getKey());	// get the queryinfo by doing a lookup on term
						Double weightQuery = queryInfo.get(0); /// normalQuery;	// get the weight of query
						Double weightDoc = (double) entry.getValue().size()/ weight;	// get the normalized weight of the document	
						scoreDoc += (weightQuery * weightDoc); 
					}
					
					finalScore = (scoreDoc * 3) / lengthDoc;
					
					
					
					
					DocDetails doc = new DocDetails(fileID, finalScore, documents.getTfMap());	// call the DocDetails class constructor
					docsRelevancy.add(doc);
					
				}
				
				// sorting the ArrayList based on the score
				Collections.sort(docsRelevancy, new Comparator<DocDetails>() {
					public int compare(DocDetails d1, DocDetails d2) {
						Double score1 = d1.score;
						Double score2 = d2.score;
						return score2.compareTo(score1);
					}
				});
				
				
	}
	
	/**
	 * Evaluating the given boolean expression with OKAPI
	 */
	public void evaluateWithOkapi() {
		// computes
		HashMap<String, ArrayList<Double>> queryMatrix = computeQueryMatrixVSM();
		
		
		// finally evaluate the query and generate the final merged postings list that contain terms in the 
		// query given
		evaluateQuery();
		Result finalResult = this.finalList.get(0);
		this.docsRelevancy = new ArrayList<DocDetails>();
		double averageDocLength = (double) this.docLengthDictionary.get(999999);
		double k1const = 1.2d;
		double k3const = 1.2d;
		double bconst = 0.75d;
		
		for (Postings documents: finalResult.postingsList) {
			Map<String, ArrayList<Integer>> termsMap = documents.getTfMap(); 	// get the term: tf matrix of the document
			
			double lengthDoc = this.docLengthDictionary.get(documents.getFileID());
			Double scoreDoc = 0d;
			// Now, to compare and multiply weights to compute VSM weight
			for (Map.Entry<String, ArrayList<Integer>> entry: termsMap.entrySet()) {	// iterating through the key set again
				
				ArrayList<Double> queryInfo = queryMatrix.get(entry.getKey());	// get the queryinfo by doing a lookup on term
				Double tfQuery = queryInfo.get(1);	// get the weight of query
				Double tfDoc = (double) entry.getValue().size();	// tf of the document
				Double idf = (double) queryInfo.get(2);
				scoreDoc += ((idf * (k1const + 1) * tfDoc * (k3const + 1) * tfQuery)
						/((k1const * ((1-bconst) + bconst * (lengthDoc/averageDocLength)) + tfDoc) * (k3const +tfQuery)));
				
			}
			
			String fileID = this.inverseFileIDDictionary.get(documents.getFileID());	// get the fileID of the document
			DocDetails doc = new DocDetails(fileID, scoreDoc, documents.getTfMap());	// call the DocDetails class constructor
			docsRelevancy.add(doc);
		}
		
		// sorting the ArrayList based on the score
		Collections.sort(docsRelevancy, new Comparator<DocDetails>() {
			public int compare(DocDetails d1, DocDetails d2) {
				Double score1 = d1.score;
				Double score2 = d2.score;
				return score2.compareTo(score1);
			}
		});
		
		
	}
	
	/**
	 * Method to execute queries in E mode
	 * @param queryFile : The file from which queries are to be read and executed
	 */
	public void query(File queryFile) {
		//TODO: IMPLEMENT THIS METHOD
		try {
			this.eModeTrue = true;
			BufferedReader getInfo;
			eModeInput = new ArrayList<eModeRead>();
			int numQueries = 0;
			getInfo = new BufferedReader(new FileReader(queryFile));
			String temp;
			temp = getInfo.readLine(); // reading the first line
			Pattern p = Pattern.compile("(.+)=([0-9]+)");
			Matcher m = p.matcher(temp);
			// getting the number of queries
			if (m.find() == true) {
				numQueries = Integer.parseInt(m.group(2));
			}
			
			for (int i=0; i < numQueries; i++) {
				temp = getInfo.readLine();
				p = Pattern.compile("(.+):\\{(.+)\\}");
				m = p.matcher(temp);
				String queryID = "";
				String query = "";
				
				if (m.find() == true) {
					queryID = m.group(1);
					query = m.group(2);
					System.out.println(queryID);
					System.out.println(query);
				}
				
				if (queryID.equals("") || query.equals("")){
					
				} else {
					eModeRead queryToPut = new eModeRead(queryID, query);
					eModeInput.add(queryToPut);
				}	
			}
			
			getInfo.close();
			StringBuffer sb = new StringBuffer();
			int numResults = 0;
			
			
			for (eModeRead queryOne: eModeInput) {
				String userQuery = queryOne.query;
				String queryID = queryOne.queryID;
				Query q;
				this.defaultOperator = "OR";
				this.defaultOperator = chooseDefault(userQuery);
				q = QueryParser.parse(userQuery, this.defaultOperator);
				q.toString();
				System.out.println(q.toString());
				// get the postfix expression and create a set of Result objects based on it
				LinkedList<String> postfixExpr = q.getPostfixList();
				treatQuery(postfixExpr);	// treats the query and produces LinkedList<Results> finalList ready for stack evaluation							
				
				evaluateWithTFIDF();
				if (this.docsRelevancy.size() == 0) continue;
				numResults++;
				sb.append(queryID+":{");
				int i=0;
				for (DocDetails d: this.docsRelevancy) {
					if (i==10) break;
					if (i!=0) {
						sb.append(",");
					}
					String fileID = d.fileID;
					String score = String.format("%.5f", d.score);
					sb.append(" " + fileID +"#" +score);
					i++;
				}
				sb.append("}");
				sb.append('\n');
				//stream.print(sb.toString());
				
				
				
			}
			// Get the query object converted to postfix form
			stream.print("numResults =" + numResults);
			stream.println();
			stream.print(sb.toString());
			stream.close();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * General cleanup method
	 */
	public void close() {
		//TODO : IMPLEMENT THIS METHOD
	}
	
	/**
	 * Method to indicate if wildcard queries are supported
	 * @return true if supported, false otherwise
	 */
	public static boolean wildcardSupported() {
		//TODO: CHANGE THIS TO TRUE ONLY IF WILDCARD BONUS ATTEMPTED
		return false;
	}
	
	/**
	 * Method to get substituted query terms for a given term with wildcards
	 * @return A Map containing the original query term as key and list of
	 * possible expansions as values if exist, null otherwise
	 */
	public Map<String, List<String>> getQueryTerms() {
		//TODO:IMPLEMENT THIS METHOD IFF WILDCARD BONUS ATTEMPTED
		return null;
		
	}
	
	/**
	 * Method to indicate if speel correct queries are supported
	 * @return true if supported, false otherwise
	 */
	public static boolean spellCorrectSupported() {
		//TODO: CHANGE THIS TO TRUE ONLY IF SPELLCHECK BONUS ATTEMPTED
		return false;
	}
	
	/**
	 * Method to get ordered "full query" substitutions for a given misspelt query
	 * @return : Ordered list of full corrections (null if none present) for the given query
	 */
	public List<String> getCorrections() {
		//TODO: IMPLEMENT THIS METHOD IFF SPELLCHECK EXECUTED
		return null;
	}
	
	/**
	 * METHOD that evaluates the LinkedList<Result> to generate the final merged postings list for 
	 * further evaluation by one of the ranking models
	 */
	public void evaluateQuery(){
		ListIterator<Result> li = finalList.listIterator();
		Result temp;
		Result first;
		Result second;
		Result replace;
		
		
		while (li.hasNext()) {
			temp = li.next();
			
			if (temp.isOperator == true) {
				li.previous();
				second = li.previous();
				first = li.previous();
				
				li.next();		
				li.remove();	// removing operand 1
				li.next();
				li.remove();	// removing operand 2
				li.next();
				li.remove();	// removing operator
				
				switch (temp.oper) {	// What type is the operator?
					case OR:	// OR operator
						if (second.isnot == ResultType.ISNot.YES) {
							temp = simpleNOTEvaluator(first, second);
							li.add(temp);
						} else {
							temp = simpleOREvaluator(first, second);
							li.add(temp);
						}
						break;
					case AND:	// AND operator
						if (second.isnot == ResultType.ISNot.YES) {
							temp = simpleNOTEvaluator(first, second);
							li.add(temp);
						} else {
							temp = simpleANDEvaluator(first, second);
							li.add(temp);
						}
						
						break;
				}
				li = finalList.listIterator();	// resetting the list iterator
				
			} // END OF IF WHERE ELEMENT IS AN OPERATOR
			
		}	// END OF WHILE LOOP THAT ITERATES THROUGH RESULT SET
		
		
		
	}	// END OF METHOD
	
	/**
	 * METHOD TO EVALUATE THE UNION OF TWO POSTINGS LISTS
	 * @param first
	 * @param second
	 * @return
	 */
	public Result simpleOREvaluator(Result first, Result second) {
		
		LinkedList<Postings> firstPostings = first.postingsList;	// get the FIRST elements postingsList
		LinkedList<Postings> secondPostings = second.postingsList;	// SECOND elements postings list
		LinkedList<Postings> tempPostings = new LinkedList<Postings>();
		Boolean intersection = false;	// flag to track intersection
		
		if (secondPostings.size() == 0 && firstPostings.size() == 0){
			first.postingsList = tempPostings;
			return first;
		} else if (secondPostings.size() == 0) {
			return first;
		} else if (firstPostings.size() == 0) {
			return second;
		}
		
		tempPostings.addAll(first.postingsList); 
		
		for (Postings two:secondPostings) {	// iterate through each element from second postings list
			intersection = false;	// assume intersection is FALSE in the beginning
			for (Postings one:firstPostings) {	// iterate through each element from first list while second is fixed
				if (two.getFileID() == one.getFileID()) {	// if both the file ID's have a match
					for (Postings p:tempPostings) {
						if (p.getFileID() == one.getFileID()) {
							p.setTfMap(two.getTfMap());
						}
					}
					intersection = true;	// set intersection to true
				} 
			}
			if (intersection == false) {	// if no intersection, then add the two Postings to firstPostings list
				tempPostings.add(two);	
			}
		}
		
		
		
		first.postingsList = tempPostings;
		
		return first;
	}
	
	/**
	 * METHOD TO EVALUATE THE INTERSECTION OF TWO POSTINGS LIST
	 * @param first
	 * @param second
	 * @return
	 */
	public Result simpleANDEvaluator(Result first, Result second) {
		LinkedList<Postings> firstPostings = first.postingsList;	// get the FIRST elements postingsList
		LinkedList<Postings> secondPostings = second.postingsList;	// SECOND elements postings list
		LinkedList<Postings> tempMap = new LinkedList<Postings>();
		
		
		if (secondPostings.size() == 0 || firstPostings.size() == 0){
			first.postingsList = tempMap;
			return first;
		} 
		
		for (Postings two:secondPostings) {	// iterate through each element from second postings list
			for (Postings one:firstPostings) {	// iterate through each element from first list while second is fixed
				if (two.getFileID() == one.getFileID()) {	// if both the file ID's have a match
					one.setTfMap(two.getTfMap());	// update the tfMap
					tempMap.add(one);
				} 
			}
		}
		
		
		
		first.postingsList = tempMap;	// return the Result Object
		
		return first;
	}
	
	/**
	 * METHOD TO EVALUATE THE NOT OF TWO POSTINGS LIST
	 * @param first
	 * @param second
	 * @return
	 */
	public Result simpleNOTEvaluator(Result first, Result second) {
		LinkedList<Postings> firstPostings = first.postingsList;	// get the FIRST elements postingsList
		LinkedList<Postings> secondPostings = second.postingsList;	// SECOND elements postings list
		LinkedList<Postings> resultOfNot = new LinkedList<Postings>();
		Boolean dontAdd = false;
		
		for (Postings one:firstPostings) {
			dontAdd = false;
			for (Postings two:secondPostings) {
				if (one.getFileID() == two.getFileID()) {
					dontAdd = true;	// in case the file is present in the second postings List, then should be removed
				}
			}
			if (dontAdd == false) {
				resultOfNot.add(one);
			}
		}
		
		
		
		first.postingsList = resultOfNot;
		
		return first;
	}
	
	
	/**
	 * This function treats the query and returns a linked list with postings for all the query terms
	 * FUNCTION TESTED AND WORKING
	 * @param pList
	 * @return
	 */
	public void treatQuery(LinkedList<String> pList){
		
		Result r;
		Pattern p;
		Matcher m;
		Pattern pnew;
		Matcher mnew;
		String index;
		String term;
		
		this.finalList = new LinkedList<Result>();
		
		// look at each element of the linkedlist pList in isolation
		for(String current:pList){
			// First, check if the current element is an operator
			// If so, create an appropriate Result object
			if (current.trim().equals("&")){
				r = new Result(ResultType.OperatorType.AND);
				this.finalList.add(r);
				continue;
				
			} else if (current.trim().equals("|")){
				r = new Result(ResultType.OperatorType.OR);
				this.finalList.add(r);
				continue;
			}
			
			// Second, check for not
			p = Pattern.compile("<(.+):(.+)>");		// PATTERN  for query with NOT
			m = p.matcher(current.trim());
			
			pnew = Pattern.compile("(.+):(.+)");	// PATTERN for simple query without NOT
			mnew = pnew.matcher(current.trim());
			
			
			// NOT present in term
			if (m.find() == true){
				
				index = m.group(1);
				term = m.group(2);
				
				p = Pattern.compile("\"(.+)\"");
				m = p.matcher(term);
				
				if (m.matches()) {		// IF PHRASE
					term = m.group(1);
					if (index.toLowerCase().equals("term")){	// if TERM INDEX
						this.finalList.add(new Result(ResultType.IndexType.TERM, ResultType.QueryType.PHRASE, ResultType.ISNot.YES, term));
						continue;
					} else if (index.toLowerCase().equals("place")){	// if PLACE INDEX
						this.finalList.add(new Result(ResultType.IndexType.PLACE, ResultType.QueryType.PHRASE, ResultType.ISNot.YES, term));
						continue;
					} else if (index.toLowerCase().equals("category")){	// if CATEGORY INDEX
						this.finalList.add(new Result(ResultType.IndexType.CATEGORY, ResultType.QueryType.PHRASE, ResultType.ISNot.YES, term));
						continue;
					} else if (index.toLowerCase().equals("author")){	// if AUTHOR INDEX
						this.finalList.add(new Result(ResultType.IndexType.AUTHOR, ResultType.QueryType.PHRASE, ResultType.ISNot.YES, term));
						continue;
					}
					
				} else {	// IF SINGLE QUERY
					
					if (index.toLowerCase().equals("term")){	// if TERM INDEX
						this.finalList.add(new Result(ResultType.IndexType.TERM, ResultType.QueryType.SINGLE, ResultType.ISNot.YES, term));
						continue;
					} else if (index.toLowerCase().equals("place")){	// if PLACE INDEX
						this.finalList.add(new Result(ResultType.IndexType.PLACE, ResultType.QueryType.SINGLE, ResultType.ISNot.YES, term));
						continue;
					} else if (index.toLowerCase().equals("category")){	// if CATEGORY INDEX
						this.finalList.add(new Result(ResultType.IndexType.CATEGORY, ResultType.QueryType.SINGLE, ResultType.ISNot.YES, term));
						continue;
					} else if (index.toLowerCase().equals("author")){	// if AUTHOR INDEX
						this.finalList.add(new Result(ResultType.IndexType.AUTHOR, ResultType.QueryType.SINGLE, ResultType.ISNot.YES, term));
						continue;
					}
				}
				
				
			} else if (mnew.matches()) {	// NOT doesnt occur in term
				index = mnew.group(1);
				term = mnew.group(2);
				
				pnew = Pattern.compile("\"(.+)\"");
				mnew = pnew.matcher(term);
				
				if (mnew.matches()) {		// IF PHRASE
					term = mnew.group(1);
					if (index.toLowerCase().equals("term")){	// if TERM INDEX
						this.finalList.add(new Result(ResultType.IndexType.TERM, ResultType.QueryType.PHRASE, ResultType.ISNot.NO, term));
						continue;
					} else if (index.toLowerCase().equals("place")){	// if PLACE INDEX
						this.finalList.add(new Result(ResultType.IndexType.PLACE, ResultType.QueryType.PHRASE, ResultType.ISNot.NO, term));
						continue;
					} else if (index.toLowerCase().equals("category")){	// if CATEGORY INDEX
						this.finalList.add(new Result(ResultType.IndexType.CATEGORY, ResultType.QueryType.PHRASE, ResultType.ISNot.NO, term));
						continue;
					} else if (index.toLowerCase().equals("author")){	// if AUTHOR INDEX
						this.finalList.add(new Result(ResultType.IndexType.AUTHOR, ResultType.QueryType.PHRASE, ResultType.ISNot.NO, term));
						continue;
					}
					
				} else {	// IF SINGLE QUERY
					
					if (index.toLowerCase().equals("term")){	// if TERM INDEX
						this.finalList.add(new Result(ResultType.IndexType.TERM, ResultType.QueryType.SINGLE, ResultType.ISNot.NO, term));
						continue;
					} else if (index.toLowerCase().equals("place")){	// if PLACE INDEX
						this.finalList.add(new Result(ResultType.IndexType.PLACE, ResultType.QueryType.SINGLE, ResultType.ISNot.NO, term));
						continue;
					} else if (index.toLowerCase().equals("category")){	// if CATEGORY INDEX
						this.finalList.add(new Result(ResultType.IndexType.CATEGORY, ResultType.QueryType.SINGLE, ResultType.ISNot.NO, term));
						continue;
					} else if (index.toLowerCase().equals("author")){	// if AUTHOR INDEX
						this.finalList.add(new Result(ResultType.IndexType.AUTHOR, ResultType.QueryType.SINGLE, ResultType.ISNot.NO, term));
						continue;
					}
				}
			}
		
		}	// end OF FOR
		
		
		
		return;	// come out of method, job done
		
	}	// END OF CLASS
	
	
	/**
	 * METHOD to choose the default operator based on the query sent
	 * @param userQuery
	 * @return
	 */
	public String chooseDefault(String userQuery){
		// METHOD STUB FILL THIS
		// first, split userQuery on spaces
		// DONT THINK I WILL NEED THIS METHOD
		String temp1, temp2;
		String[] tokens = userQuery.split("\\s");
		String defaultOper = "OR";
		
		
		
		
		
		
		for(int i=0; i<tokens.length ; i++)
		{
			if (i == tokens.length - 1)
			{
				break;
			}
			
			
			temp1 = tokens[i];
			temp2 = tokens[i+1];
			
			// if any one of the tokens are "OR" or "AND" then do nothing
			if(temp1.equals("OR") || temp1.equals("AND") || temp2.equals("OR") || temp2.equals("AND")
					|| temp1.equals("NOT") || temp1.equals("(") || temp2.equals(")"))
			{
				//DO NOTHING
			}
			else
			{	
				if (temp2.equals("NOT")){
					defaultOper = "AND";
				} 
			}
		}
		return defaultOper;
	}
	
	/**
	 * Inner class to store various information about a term (Like postingsList, term frequency, etc)
	 * @author Girish
	 *
	 */
	class Result{
		String term;
		Boolean isOperator = false; // false by default, set to true if operator is seen
		LinkedList<Postings> postingsList;	
		Boolean isNot = false; // false by default, set to true if the term is NOT
		
		// Declaring the various enums nested in inner classes
		ResultType.IndexType index;
		ResultType.QueryType query;
		ResultType.OperatorType oper;
		ResultType.ISNot isnot;
		
		
		
		public Result(ResultType.OperatorType oper){	// Constructor when an Operator is encountered
			this.oper = oper;
			this.isOperator = true;
			postingsList = new LinkedList<Postings>();
		}
		
		public Result(ResultType.IndexType index, ResultType.QueryType query, ResultType.ISNot isnot, String term){	// Constructor when an Operand is encountered
			this.index = index;
			this.query = query;
			this.isnot = isnot;
			this.term = term;
			getPostingsForTerm();
			
		}
		
		/**
		 * Allocator method to see the type of query - single or phrase and call respective functions
		 */
		public void getPostingsForTerm(){
			
			
			if (query == ResultType.QueryType.SINGLE) {	// IF SINGLE QUERY
				getPostingsForQuery(term);
			} else if (query == ResultType.QueryType.PHRASE) {	// IF PHRASE QUERY
				getPostingsForPhrase(term);
			}
			
		}
		
		/**
		 * Method to get the postings list for a phrase query
		 * @param phrase
		 * @return
		 */
		public void getPostingsForPhrase(String phrase){
			this.postingsList = new LinkedList<Postings>();
			LinkedList<Postings> tempList = new LinkedList<Postings>();
			ArrayList<LinkedList<Postings>> arrayOfTerms;
			String[] analyzedList;
			int termID;
			LinkedList<Postings> toConvertMap ;
			LinkedList<Postings> elementToCompare;
			LinkedList<Postings> tempMap;
			Postings tempPostings;
			HashMap<String, ArrayList<Integer>> tempHashMap;
			
			// send the phrase to the AnalyzeString method for analysis and split on space
			// this analyzer eats up STOPWORDS as well
			phrase = AnalyzeString(phrase, FieldNames.CONTENT);
			this.term = phrase;	// set the term as stemmed one
			
			// if phrase already in the index, then return its posting list
			if (termDictionary.containsKey(phrase)){
				int key = termDictionary.get(phrase);
				this.postingsList = termIndex.get(key);
				return;
			} 
			// else we need to AND the phrase and return the resulting postings list
			analyzedList = phrase.split("\\s+");
			this.term = analyzedList[0];
			
			// if even a single term is not in the dictionary then return no posting
			for (String term:analyzedList){
				if (index == ResultType.IndexType.TERM) {
					if (termDictionary.containsKey(term)){
						continue;
					} else {
						return;
					} 
				} else if (index == ResultType.IndexType.AUTHOR) {
					if (authorDictionary.containsKey(term)){
						continue;
					} else {
						return;
					}
				} else if (index == ResultType.IndexType.PLACE) {
					if (placeDictionary.containsKey(term)){
						continue;
					} else {
						return;
					}
				} else if (index == ResultType.IndexType.CATEGORY) {
					if (categoryDictionary.containsKey(term)){
						continue;
					} else {
						return;
					}
				}
			}
			
			
			arrayOfTerms = new ArrayList<LinkedList<Postings>>();
			// retrieve the postings list for all of the terms and store them in a array list
			// called arrayOfTerms
			if (index == ResultType.IndexType.TERM){
				
				for (String term:analyzedList)
				{
					termID = termDictionary.get(term);
					tempList = termIndex.get(termID);
					arrayOfTerms.add(tempList);
				}
			} else if (index == ResultType.IndexType.AUTHOR) {

				for (String term:analyzedList)
				{	
					termID = authorDictionary.get(term);
					tempList = authorIndex.get(termID);
					arrayOfTerms.add(tempList);
				}	
			}  else if (index == ResultType.IndexType.PLACE) {
				
				for (String term:analyzedList)
				{
					termID = placeDictionary.get(term);
					tempList = placeIndex.get(termID);
					arrayOfTerms.add(tempList);
				}	
			}  else if (index == ResultType.IndexType.CATEGORY) {
				
				for (String term:analyzedList)
				{
					termID = categoryDictionary.get(term);
					tempList = categoryIndex.get(termID);
					arrayOfTerms.add(tempList);
				}	
			} 
			
			// if the analyzed phrase has only one element, then set the postings list as the postings
			// list of that one element
			
			if (arrayOfTerms.size() == 1) {
				this.postingsList = arrayOfTerms.get(0);
				return;
			} else {	// if more than one element exists
				toConvertMap = arrayOfTerms.get(0);
				for (int i=1; i<arrayOfTerms.size(); i++) {
					elementToCompare = arrayOfTerms.get(i);
					
					tempMap = new LinkedList<Postings>();
					for (Postings xyz:elementToCompare)
					{
						for (Postings temp:toConvertMap)
						{
							if (temp.getFileID() == xyz.getFileID())
							{	
								tempPostings = xyz;
								tempHashMap = temp.getTfMap();
								tempPostings.setTfMap(tempHashMap);
								tempMap.add(tempPostings);
								break;
							}	// END OF IF THAT MATCHES COMMON FILES IN A POSTINGS LIST
						}
						
					}	// END OF FOR LOOP THAT COMPARES INDIVIDUAL POSTINGS OF PIVOT AND NEW POSTINGS LIST
					toConvertMap = tempMap;
				
				}	// END OF FOR LOOP WHICH SCANS THE FILE
				
			}	// END OF ELSE CASE WHERE MORE THAN ONE TERM EXISTS
				
			
			
			HashMap<String, ArrayList<Integer>> mapToManipulate;
			Boolean passTest = true;
			
			for (Postings check:toConvertMap) {
				mapToManipulate = check.getTfMap();	// getting the tfMap
				passTest = false;
				ArrayList<Integer> firstArray = mapToManipulate.get(analyzedList[0]);
				ArrayList<Integer> newTfMap = new ArrayList<Integer>();
				int difference = 1;
				
				for (int i=1; i<analyzedList.length; i++) {
					ArrayList<Integer> nthArray = mapToManipulate.get(analyzedList[i]);
					passTest = false;
					
					for (int element:firstArray){	// for each element in firstArray
						
						for (int nthElement: nthArray) {	// for each element in secondArrray
							
							if (element + difference == nthElement) {	// if position is consecutive
								newTfMap.add(element);
								passTest = true;	// if element passes test break out of this loop
								break;
							} 
						}	// END OF FOR LOOP TO ITERATE OVER EACH ELEMENT IN NTH ARRAY 
						
						/*if (passTest == true){
							break;	// break out of this loop also
						}*/
					}	// END OF FOR TO ITERATE OVER EACH ELEMENT IN THE PIVOT ARRAY
				
					difference++;	// value of difference keeps growing by 1
					
				}	// END OF FOR TO ITERATE OVER OTHER TERMS OF LIST
				
				if (passTest == true) {
					check.setTfMapForPhrase(analyzedList[0], newTfMap);
					this.postingsList.add(check);
				}
				
			}	// END OF FOR LOOP THAT CHECKS EACH POSTING IN LIST
			
		}
		
		/**
		 * Get postings List for a SINGLE query and return
		 * @param phrase
		 * @return
		 */
		public void getPostingsForQuery(String phrase){
			LinkedList<Postings> postingsList = new LinkedList<Postings>();
			int ID;
			
			
			if (index == ResultType.IndexType.TERM){	// if from TERM
				phrase = AnalyzeString(phrase, FieldNames.CONTENT);
				this.term = phrase;
				if (termDictionary.containsKey(phrase)){	// if found in TERM index	
					ID = termDictionary.get(phrase);
					this.postingsList = termIndex.get(ID);
				} else {	// if NOT found in TERM index
					this.postingsList = new LinkedList<Postings>();	// INITIALIZE EMPTY POSTINGS LIST
				}
				
			} else if (index == ResultType.IndexType.AUTHOR){	// if from AUTHOR
				phrase = AnalyzeString(phrase, FieldNames.AUTHOR);
				this.term = phrase;
				if (authorDictionary.containsKey(phrase)){	// if found in AUTHOR index	
					ID = authorDictionary.get(phrase);
					this.postingsList = authorIndex.get(ID);	
				} else {	// if NOT found in AUTHOR index
					this.postingsList = new LinkedList<Postings>();
				}
				
			} else if (index == ResultType.IndexType.CATEGORY){	// if from CATEGORY
				phrase = AnalyzeString(phrase, FieldNames.CATEGORY);
				this.term = phrase;
				if (categoryDictionary.containsKey(phrase)){	// if found in CATEGORY index	
					ID = categoryDictionary.get(phrase);
					this.postingsList = categoryIndex.get(ID);	
				} else {	// if NOT found in CATEGORY index
					this.postingsList = new LinkedList<Postings>();
				}
				
			} else if (index == ResultType.IndexType.PLACE){	// if from PLACE
				phrase = AnalyzeString(phrase, FieldNames.PLACE);
				this.term = phrase;
				if (placeDictionary.containsKey(phrase)){	// if found in PLACE index	
					ID = placeDictionary.get(phrase);
					this.postingsList = placeIndex.get(ID);	
				} else {	// if NOT found in PLACE index
					this.postingsList = new LinkedList<Postings>();
				}
				
			}
			
		}
		
		
		/**
		 * To analyze the query string and return
		 * @param phrase - The phrase to analyze
		 * @return
		 */
		public String AnalyzeString(String phrase, FieldNames fn) {
			ArrayList<String> analyzedList = new ArrayList<String>();
			
			Tokenizer tknizer = new Tokenizer();
			AnalyzerFactory fact = AnalyzerFactory.getInstance();
			try {
				TokenStream stream = tknizer.consume(phrase);
				Analyzer analyzer = fact.getAnalyzerForField(fn, stream);
				
				while (analyzer.increment()) {
					
				}
				stream = analyzer.getStream();
				stream.reset();
				
				while (stream.hasNext()){
					Token t = stream.next();
					analyzedList.add(t.getTermText());
				}
				
			} catch (TokenizerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// joining back the arrayList to make a String
			StringBuilder sb = new StringBuilder();
			for (String temp:analyzedList){
				sb.append(temp);
				sb.append(" ");
			}
			
			
			return sb.toString().trim();	
		}
		
	}
}


class DocDetails {
	String fileID;
	Double score;
	HashMap<String, ArrayList<Integer>> termMap;
	
	
	public DocDetails(String fileID, Double score, HashMap<String, ArrayList<Integer>> termMap) {
		this.fileID = fileID;
		this.score = score;
		this.termMap = termMap;
	}
	
}

