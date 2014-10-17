package edu.buffalo.cse.irf14;

import java.io.File;
import java.io.FileInputStream;
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
		this.defaultOperator = "OR"; // change this later
		
		// Get the query object converted to postfix form
		Query q;
		q = QueryParser.parse(userQuery, this.defaultOperator);
		q.toString();
		
		// get the postfix expression and create a set of Result objects based on it
		LinkedList<String> postfixExpr = q.getPostfixList();
		treatQuery(postfixExpr);	// treats the query and produces LinkedList<Results> finalList ready for stack evaluation							
		
		// computes
		HashMap<String, ArrayList<Double>> queryMatrix = computeQueryMatrixVSM();
		
		for (Map.Entry<String, ArrayList<Double>> entry: queryMatrix.entrySet()){
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
		}
		
		// finally evaluate the query and generate the final merged postings list that contain terms in the 
		// query given
		evaluateQuery();
		Result finalResult = this.finalList.get(0);
		ArrayList<DocDetails> docsRelevancy = new ArrayList<DocDetails>();
		
		for (Postings documents: finalResult.postingsList) {
			Map<String, ArrayList<Integer>> termsMap = documents.getTfMap(); 	// get the term: tf matrix of the document
			double euclidNorm = 0d;	
			double finalEuclidNorm;
			for (Map.Entry<String, ArrayList<Integer>> entry: termsMap.entrySet()) {	
				euclidNorm += Math.pow(entry.getValue().size(), 2d);
			}
			
			finalEuclidNorm = Math.sqrt(euclidNorm);
			Double scoreDoc = 0d;
			// Now, to compare and multiply weights to compute VSM weight
			for (Map.Entry<String, ArrayList<Integer>> entry: termsMap.entrySet()) {	// iterating through the key set again
				ArrayList<Double> queryInfo = queryMatrix.get(entry.getKey());	// get the queryinfo by doing a lookup on term
				Double weightQuery = queryInfo.get(0);	// get the weight of query
				Double weightDoc = entry.getValue().size() / finalEuclidNorm;	// get the normalized weight of the document	
				scoreDoc += weightQuery * weightDoc;	// multiply the weights
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
				return score1.compareTo(score2);
			}
		});
		
		for (DocDetails d:docsRelevancy) {
			System.out.println(d.fileID + '\t' + d.score);
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
				
				/*// FOR TF TABLE TESTING PURPOSES
				for (Map.Entry<String, Double> entry: tfTable.entrySet()) {
					System.out.println(entry.getKey());
					System.out.println(entry.getValue());
				}*/
				
				
				HashMap<String, ArrayList<Double>> perTermMap = new HashMap<String, ArrayList<Double>>();
				ArrayList<Double> doubleList;
				
				
				
				for (Result r:this.finalList) {		// Scan through list again and generate other valuse of query
					if (r.isOperator == false) {	// if a TERM
						String term = r.term;
						// GETTING the df of the term and finally the idf
						int mapID;
						double df;
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
								doubleList.add(0d);	// 0 as it is the author index
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
								doubleList.add(1d);	// 0 as it is the category index
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
								doubleList.add(2d);	// 0 as it is the place index
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
								doubleList.add(3d);	// 3 as it is the term index
								perTermMap.put(term, doubleList);
								
							} 
							
						}
					}
					
					
				}	// END OF FOR LOOP TO CREATE QUERY MATRIX
				return perTermMap;
	}
	
	/**
	 * Method to execute queries in E mode
	 * @param queryFile : The file from which queries are to be read and executed
	 */
	public void query(File queryFile) {
		//TODO: IMPLEMENT THIS METHOD
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
		
		// CODE FOR TESTING ONLY
		/*Result r = this.finalList.get(0);
		System.out.println(r.postingsList.size());
		for (Postings p:r.postingsList) {
			System.out.println(p.getFileID());
			for (Map.Entry<String, ArrayList<Integer>> entry:p.getTfMap().entrySet()) {
				System.out.println(entry.getKey());
				System.out.println(entry.getValue());
			}
		}*/
		
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
			System.out.println(intersection);
			if (intersection == false) {	// if no intersection, then add the two Postings to firstPostings list
				tempPostings.add(two);
				
			}
		}
		
		// CODE BELOW FOR TESTING ONLY
		/* for (Postings per:firstPostings) {
			System.out.println(per.getFileID());
			for (Map.Entry<String, ArrayList<Integer>> entry: per.getTfMap().entrySet()){
				System.out.println(entry.getKey());
				System.out.println(entry.getValue());
			}
		}*/
		
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
		
		/* //CODE BELOW FOR TESTING
		for (Postings per:tempMap) {
			System.out.println(per.getFileID());
			for (Map.Entry<String, ArrayList<Integer>> entry: per.getTfMap().entrySet()){
				System.out.println(entry.getKey());
				System.out.println(entry.getValue());
			}
		}*/
		
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
		
		/*System.out.println("here"); 
		 //CODE BELOW FOR TESTING
		for (Postings per:resultOfNot) {
			System.out.println(per.getFileID());
			for (Map.Entry<String, ArrayList<Integer>> entry: per.getTfMap().entrySet()){
				System.out.println(entry.getKey());
				System.out.println(entry.getValue());
			}
		}*/
		
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
		return userQuery;
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
			
			// if even a single term is not in the dictionary then return no posting
			for (String term:analyzedList){
				if (termDictionary.containsKey(term)){
					continue;
				} else {
					return;
				}
			}
			
			// retrieve the postings list for all of the terms and store them in a array list
			// called arrayOfTerms
			arrayOfTerms = new ArrayList<LinkedList<Postings>>();
			for (String term:analyzedList)
			{
				termID = termDictionary.get(term);
				tempList = termIndex.get(termID);
				arrayOfTerms.add(tempList);
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
				
			// Moving on to finding phrase queries out of the result set
			// use the toConvertMap, iterate through the postings, and choose the postings that 
			// have proximity of 1
			
			/*for (Postings p:toConvertMap) {
				for (Map.Entry<String, ArrayList<Integer>> entry: p.getTfMap().entrySet()) {
					System.out.println(p.getFileID());
					System.out.println(entry.getKey());
					System.out.println(entry.getValue());
				}
			}*/
			
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
					check.setTfMapForPhrase(this.term, newTfMap);
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

