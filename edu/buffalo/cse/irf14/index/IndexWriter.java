/**
 * 
 */
package edu.buffalo.cse.irf14.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import edu.buffalo.cse.irf14.document.Document;
import edu.buffalo.cse.irf14.document.FieldNames;
import edu.buffalo.cse.irf14.analysis.*;

/**
 * @author nikhillo
 * Class responsible for writing indexes to disk
 */
public class IndexWriter extends Token implements Serializable{
	
	
	// Class Variables needed
	private String indexDirectory;
	private int fileIDAssigner = 1;
	private int termIDAssigner = 1;
	private int categoryIDAssigner = 1;
	private int placeIDAssigner = 1;
	private int authorIDAssigner = 1;
	
	// Declaring the various lookup dictionaries needed for indexing process
	private HashMap<String, Integer> fileIDDictionary;
	private HashMap<Integer, String> inverseFileIDDictionary;
	private HashMap<String, Integer> termDictionary;
	private HashMap<String, Integer> categoryDictionary;
	private HashMap<String, Integer> placeDictionary;
	private HashMap<String, Integer> authorDictionary;
	private HashMap<Integer, Integer> docLengthDictionary;
	private HashMap<String, Double> normalizedIndex;
	
	// Declaring the various indexes needed
	private HashMap<Integer, LinkedList<Postings>> termIndex;
	private HashMap<Integer, LinkedList<Postings>> categoryIndex;
	private HashMap<Integer, LinkedList<Postings>> authorIndex;
	private HashMap<Integer, LinkedList<Postings>> placeIndex;
	
	// postings list linked list to be used
	LinkedList<Postings> postingsListCategory;
	LinkedList<Postings> postingsListTerm;
	LinkedList<Postings> postingsListPlace;
	LinkedList<Postings> postingsListAuthor; 
	
	// declaring hash map which will generate top k terms
	private HashMap<String, Integer> kTermMap;
	private HashMap<String, Integer> kCategoryMap;
	private HashMap<String, Integer> kPlaceMap;
	private HashMap<String, Integer> kAuthorMap;
	
	// Utility Variables
	int position = 0;
	int docLength = 0;
	int docsParsed = 0;
	HashMap<String, Integer> tempTfMap;
	
	// Inner class postings to store postings information
	
	
	/**
	 * Default constructor
	 * @param indexDir : The root directory to be sued for indexing
	 */
	
	public IndexWriter(String indexDir) {
		//TODO : YOU MUST IMPLEMENT THIS
		
		this.indexDirectory = indexDir;
		
		// Allocating memory on the heap for the Dictionaries
		inverseFileIDDictionary = new HashMap<Integer, String>();
		fileIDDictionary = new HashMap<String, Integer>();
		docLengthDictionary = new HashMap<Integer, Integer>();
		termDictionary = new HashMap<String, Integer>();
		categoryDictionary = new HashMap<String, Integer>();
		placeDictionary = new HashMap<String, Integer>();
		authorDictionary = new HashMap<String, Integer>();
		normalizedIndex = new HashMap<String, Double>();
		
		// Allocating memory for the indexes (4 in total)
		termIndex = new HashMap<Integer, LinkedList<Postings>>();
		categoryIndex = new HashMap<Integer, LinkedList<Postings>>();
		authorIndex = new HashMap<Integer, LinkedList<Postings>>();
		placeIndex = new HashMap<Integer, LinkedList<Postings>>();
		
		// Allocating memory for the kTermMap
		this.kTermMap = new HashMap<String, Integer>();
		this.kAuthorMap = new HashMap<String, Integer>();
		this.kPlaceMap = new HashMap<String, Integer>();
		this.kCategoryMap = new HashMap<String, Integer>();
		
		
	}
	
	/**
	 * Method to add the given Document to the index
	 * This method should take care of reading the filed values, passing
	 * them through corresponding analyzers and then indexing the results
	 * for each indexable field within the document. 
	 * @param d : The Document to be added
	 * @throws IndexerException : In case any error occurs
	 */
	public void addDocument(Document d) throws IndexerException {
		//TODO : YOU MUST IMPLEMENT THIS
		// Local Variables of method addDocument
		String fileID , categoryFile;
		
		
		// Linked List structure to generate postings
		
		
		// Getting the fileID and category of the file and storing these entries into 
		// respective dictionaries - fileIDDictionary and categoryDictionary
		
		
		fileID = d.getField(FieldNames.FILEID)[0];
		
		
		// store the new fileID : compressedfileID mapping into the dictionaries
		if (this.fileIDDictionary.containsKey(fileID)) {
			
		} else {
			this.fileIDDictionary.put(fileID, this.fileIDAssigner);
			this.inverseFileIDDictionary.put(this.fileIDAssigner, fileID);
			this.fileIDAssigner++;
		}
		
		
		int idCat;
		
		// putting the category id : postings into the category index
		
		int idFile = fileIDDictionary.get(fileID);
		
		// Storing the length of the document into the dictionary
		int docLen = d.getDocLength();
		this.docLengthDictionary.put(idFile, docLen);
		// updating the utility variables to calculate avg. doc length later
		this.docLength += docLen;
		this.docsParsed++;
		
		
		
		if (d.getField(FieldNames.CATEGORY) != null)
		{	
			if (d.getField(FieldNames.CATEGORY)[0].trim().equals("") == false)
			{
				
			categoryFile = d.getField(FieldNames.CATEGORY)[0];
		// if the category is already present, get the category ID
			if (categoryDictionary.containsKey(categoryFile))
			{
				idCat = this.categoryDictionary.get(categoryFile);
			}
			else // assign a new ID for the category and store into dictionary
			{
				idCat = this.categoryIDAssigner;
				this.categoryDictionary.put(categoryFile, idCat);
				this.categoryIDAssigner++;
			}
			
			// code to store term into kTermMap as (Term : Occurences)
			if (kCategoryMap.containsKey(categoryFile)) {	// if Category already present, do nothing
				int valueOccur = kCategoryMap.get(categoryFile);
				valueOccur++;
				kCategoryMap.put(categoryFile, valueOccur);
			} else {  // set new Category and map it to (Category : 1)
				kCategoryMap.put(categoryFile, 1);
			}
		
			
	
			
			// if the category is already in the category index, then add the new file to the keys
			// equivalent postings list
			if (this.categoryIndex.containsKey(idCat))
			{
				
				postingsListCategory = this.categoryIndex.get(idCat);
				postingsListCategory.add(new Postings(idFile));
				this.categoryIndex.put(idCat, postingsListCategory);
				
			}
			else
			{	
				postingsListCategory = new LinkedList<Postings>();
				Postings element = new Postings(idFile);
				postingsListCategory.add(element);
				this.categoryIndex.put(idCat, postingsListCategory);
			}
			
			}
		}
		
		
		// DONE with the category index, now moving on to indexing TITLE, CONTENT, AUTHORORG 
		// together into the term index for the given DOCUMENT
		
		// setting the value of position to 1
		this.position = 0;
		this.tempTfMap = new HashMap<String, Integer>();
		if (d.getField(FieldNames.TITLE) != null)
		{
			if (d.getField(FieldNames.TITLE)[0].trim().equals(""))
			{
				
			}
			else
			{
				analyzeField(FieldNames.TITLE, d.getField(FieldNames.TITLE)[0], idFile);
			}
			
		}
		
		if (d.getField(FieldNames.CONTENT) != null)
		{
			if (d.getField(FieldNames.CONTENT)[0].trim().equals(""))
			{
				
			}
			else
			{
				analyzeField(FieldNames.CONTENT, d.getField(FieldNames.CONTENT)[0], idFile);
			}
			
		}
		
		// resetting the value of position to 1 so that next document will have the pointer back to 1
		this.position = 0;
		
		
		if (d.getField(FieldNames.PLACE) != null)
		{	
			if (d.getField(FieldNames.PLACE)[0].trim().equals(""))
			{
				
			}
			else
			{
				analyzeField(FieldNames.PLACE, d.getField(FieldNames.PLACE)[0], idFile);
			}
			
		}
		
		if (d.getField(FieldNames.NEWSDATE) != null)
		{	
			if (d.getField(FieldNames.NEWSDATE)[0].trim().equals(""))
			{
				
			}
			else
			{
			analyzeField(FieldNames.NEWSDATE, d.getField(FieldNames.NEWSDATE)[0], idFile);
			}
		}
		
		if (d.getField(FieldNames.AUTHORORG) != null )
		{
			if (d.getField(FieldNames.AUTHORORG)[0].trim().equals(""))
			{
				
			}
			else
			{
			analyzeField(FieldNames.AUTHORORG, d.getField(FieldNames.AUTHORORG)[0], idFile);
			}
		}
		
		if (d.getField(FieldNames.AUTHOR) != null )
		{	
			if (d.getField(FieldNames.AUTHOR)[0].trim().equals(""))
			{
				
			}
			else
			{
			analyzeField(FieldNames.AUTHOR, d.getField(FieldNames.AUTHOR), idFile);
			}
		}
		
		// CODE TO CREATE THE FORWARD INDEX
		int weightNorm = 0;
		double weightFinal;
		
		for (Map.Entry<String, Integer> entry: this.tempTfMap.entrySet()) {
			weightNorm += Math.pow(entry.getValue(), 2);
		}
		
		weightFinal = Math.sqrt(weightNorm);
		
		
		if (weightFinal != 0){
			this.normalizedIndex.put(fileID, weightFinal);
		} else {
			this.normalizedIndex.put(fileID, 1d);
		}
		
		
		
		
		
	}
	
	/**
	 * Method that indicates that all open resources must be closed
	 * and cleaned and that the entire indexing operation has been completed.
	 * @throws IndexerException : In case any error occurs
	 */
	public void close() throws IndexerException {
		
		
		// adding the average doc length entry into the hashmap 
		this.docLengthDictionary.put(999999, this.docLength/this.docsParsed);
		/*System.out.println(this.termDictionary.keySet());
		System.out.println(this.placeDictionary.keySet());
		System.out.println(this.authorDictionary.keySet());
		System.out.println(this.categoryDictionary.keySet());
		for (Map.Entry<String, Integer> entry: this.termDictionary.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		for (Map.Entry<Integer, LinkedList<Postings>> entry: this.termIndex.entrySet()){
			System.out.println(entry.getKey() + " term");
			for (Postings p: entry.getValue()) {
				System.out.println(this.inverseFileIDDictionary.get(p.getFileID()));
			}
		}*/
		
		//TODO
		try{
			
			
			// writing term index and dictionary to file
			FileOutputStream fos = new FileOutputStream(this.indexDirectory+File.separator+"termIndex.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.termIndex);
	        oos.writeObject(this.termDictionary);
	        oos.writeObject(this.kTermMap);
	        fos.close();
	        
	        
	        // writing author index and dictionary to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"authorIndex.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.authorIndex);
	        oos.writeObject(this.authorDictionary);
	        oos.writeObject(this.kAuthorMap);
	        fos.close();
	        
	        // writing place index and dictionary to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"placeIndex.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.placeIndex);
	        oos.writeObject(this.placeDictionary);
	        oos.writeObject(this.kPlaceMap);
	        fos.close();
	        
	        // writing category index and dictionary to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"categoryIndex.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.categoryIndex);
	        oos.writeObject(this.categoryDictionary);
	        oos.writeObject(this.kCategoryMap);
	        fos.close();
	        
	        
	        
	        
	        // writing fileIDDictionary to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"fileIDDict.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.fileIDDictionary);
	        oos.writeObject(this.inverseFileIDDictionary);
	        oos.writeObject(this.docLengthDictionary);	// added this dictionary for doc length recently
	        oos.writeObject(this.normalizedIndex);
	        fos.close();
	        
	        System.out.println("All writes done!!");
	        
	        
	        
	        
	        
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
		
		
	}
	
	// analyze method for the fields TITLE, CONTENT, NEWSDATE, AUTHORORG, PLACE
	private void analyzeField(FieldNames fn, String content, int fileID)
	{	
		Token t;
		String tokenText;
		boolean sameStream = false;
		this.position++;
		

		if (fn == FieldNames.TITLE || fn == FieldNames.CONTENT
				|| fn == FieldNames.NEWSDATE || fn == FieldNames.AUTHORORG) {
			try {
				// create a new token stream with the given content
				Tokenizer tkizer = new Tokenizer();
				TokenStream tstream = tkizer.consume(content);
				sameStream = false;
				

				AnalyzerFactory a = AnalyzerFactory.getInstance();
				Analyzer anal;
				anal = a.getAnalyzerForField(fn, tstream);
				tstream = anal.getStream();
				

				// iterate through the processed tokens and start the indexing
				// process
				tstream.reset();
				while (tstream.hasNext()) {
					t = tstream.next();
					tokenText = t.getTermText();
					// creating the forward index
					if (this.tempTfMap.containsKey(tokenText)){
						this.tempTfMap.put(tokenText, this.tempTfMap.get(tokenText) + 1);	// increment tf by 1
					} else {
						this.tempTfMap.put(tokenText, 1);	// set tf as 1
					}
					
					
					int tokenNo;
					// create a mapping in the term dictionary , if it already exists get the mapping
					if (this.termDictionary.containsKey(tokenText)) {
						tokenNo = this.termDictionary.get(tokenText);

					} else {
						this.termDictionary.put(tokenText, this.termIDAssigner);
						tokenNo = this.termIDAssigner;
						this.termIDAssigner++;
					}

					// code to store term into kTermMap as (Term : Occurences)
					if (kTermMap.containsKey(tokenText)) {	// if term already present, increment the value
						int valueOccur = kTermMap.get(tokenText);
						valueOccur++;
						kTermMap.put(tokenText, valueOccur);
					} else {  // set new term and map it to (term : 1)
						kTermMap.put(tokenText, 1);
					}

					// if the category is already in the category index, then
					// add the new file to the keys
					// equivalent postings list
					if (this.termIndex.containsKey(tokenNo)) {
						postingsListTerm = this.termIndex.get(tokenNo);
						// scanning through postings list to find if file is
						// already there
						// if so, increment the number of occurences of it
						sameStream = false;
						for (Postings posting : postingsListTerm) {
							if (posting.getFileID() == fileID) {
								posting.incrementOccurences();
								posting.setTfMap(tokenText, position);	// this adds the position of the term
								sameStream = true;
								break;
							} else {
								continue;
							}
						}
						
						if (sameStream == false) {
							Postings p = new Postings(fileID);
							p.setTfMap(tokenText, position);
							postingsListTerm.add(p);
							this.termIndex.put(tokenNo, postingsListTerm);
						}

					} else {
						postingsListTerm = new LinkedList<Postings>();
						Postings element = new Postings(fileID);
						element.setTfMap(tokenText, position);
						postingsListTerm.add(element);
						this.termIndex.put(tokenNo, postingsListTerm);
					}
					this.position++;
				}

			} catch (TokenizerException e) {
				e.printStackTrace();
			}

		} // END OF CASE TITLE / CONTENT / AUTHORORG / NEWSDATE
		
				
		if(fn == FieldNames.PLACE) {
			try {
				// create a new token stream with the given content
				// tokenize based on , character and not whitespace
				Tokenizer tkizer = new Tokenizer();
				TokenStream tstream = tkizer.consume(content);
				sameStream = false;
				
				AnalyzerFactory a = AnalyzerFactory.getInstance();
				Analyzer anal;
				anal = a.getAnalyzerForField(fn, tstream);
				tstream = anal.getStream();

				// iterate through the processed tokens and start the indexing
				// process
				tstream.reset();
				while (tstream.hasNext()) {
					t = tstream.next();
					tokenText = t.getTermText();
					
					// creating the forward index
					if (this.tempTfMap.containsKey(tokenText)){
						this.tempTfMap.put(tokenText, this.tempTfMap.get(tokenText) + 1);	// increment tf by 1
					} else {
						this.tempTfMap.put(tokenText, 1);	// set tf as 1
					}
					
					int tokenNo;
					// create a mapping in the term dictionary
					if (this.placeDictionary.containsKey(tokenText)) {
						tokenNo = this.placeDictionary.get(tokenText);

					} else {
						this.placeDictionary.put(tokenText,
								this.placeIDAssigner);
						tokenNo = this.placeIDAssigner;
						this.placeIDAssigner++;
					}
					
					// code to store term into kTermMap as (Term : Occurences)
					if (kPlaceMap.containsKey(tokenText)) {	// if Place already present, do nothing
						int valueOccur = kPlaceMap.get(tokenText);
						valueOccur++;
						kPlaceMap.put(tokenText, valueOccur);
					} else {  // set new Place and map it to (Place : 1)
						kPlaceMap.put(tokenText, 1);
					}

					// if the category is already in the category index, then
					// add the new file to the keys
					// equivalent postings list
					if (this.placeIndex.containsKey(tokenNo)) {

						postingsListTerm = this.placeIndex.get(tokenNo);
						// scanning through postings list to find if file is
						// already there
						// if so, increment the number of occurences of it
						sameStream = false;
						for (Postings posting : postingsListTerm) 
						{
							if (posting.getFileID() == fileID) {
								posting.incrementOccurences();
								posting.setTfMap(tokenText, 1);
								sameStream = true;
								break;
							} 
							else 
							{
								continue;
							}
						}
						
						if (sameStream == false) {
							Postings p = new Postings(fileID);
							p.setTfMap(tokenText, 1);
							postingsListTerm.add(p);
							this.placeIndex.put(tokenNo, postingsListTerm);
						}

					}
					
					else 
					{
						postingsListTerm = new LinkedList<Postings>();
						Postings element = new Postings(fileID);
						element.setTfMap(tokenText, 1);
						postingsListTerm.add(element);
						this.placeIndex.put(tokenNo, postingsListTerm);
					}

				}

			} catch (TokenizerException e) {
				e.printStackTrace();
			}

		} // END OF CASE PLACE
		
		
		
		
		
		
		
	} // END of ANALYZE method
	
	// OVERLOADED ANALYZE FIELD METHOD FOR AUTHOR FIELD - takes string[] content
	// author field can have multiple authors
	private void analyzeField(FieldNames fn, String[] contents, int fileID)
	{	
		Token t;
		String tokenText;
		boolean sameStream = false;
		
		for (String content: contents)
		{
			
		
		
				
		if(fn == FieldNames.AUTHOR) {
			try {
				// create a new token stream with the given content
				Tokenizer tkizer = new Tokenizer();
				TokenStream tstream = tkizer.consume(content);
				sameStream = false;
				
				AnalyzerFactory a = AnalyzerFactory.getInstance();
				Analyzer anal;
				anal = a.getAnalyzerForField(fn, tstream);
				tstream = anal.getStream();

				// iterate through the processed tokens and start the indexing
				// process
				tstream.reset();
				while (tstream.hasNext()) {
					t = tstream.next();
					tokenText = t.getTermText();
					
					
					// creating the forward index
					if (this.tempTfMap.containsKey(tokenText)){
						this.tempTfMap.put(tokenText, this.tempTfMap.get(tokenText) + 1);	// increment tf by 1
					} else {
						this.tempTfMap.put(tokenText, 1);	// set tf as 1
					}
					
					int tokenNo;
					// create a mapping in the term dictionary
					if (this.authorDictionary.containsKey(tokenText)) {
						tokenNo = this.authorDictionary.get(tokenText);

					} else {
						this.authorDictionary.put(tokenText,
								this.authorIDAssigner);
						tokenNo = this.authorIDAssigner;
						this.authorIDAssigner++;
					}
					
					// code to store term into kTermMap as (Term : Occurences)
					if (kAuthorMap.containsKey(tokenText)) {	// if Author already present, increment value
						int valueOccur = kAuthorMap.get(tokenText);
						valueOccur++;
						kAuthorMap.put(tokenText, valueOccur);
					} else {  // set new Author and map it to (Author : 1)
						kAuthorMap.put(tokenText, 1);
					}

					// if the category is already in the category index, then
					// add the new file to the keys
					// equivalent postings list
					if (this.authorIndex.containsKey(tokenNo)) {

						postingsListTerm = this.authorIndex.get(tokenNo);
						// scanning through postings list to find if file is
						// already there
						// if so, increment the number of occurences of it
						sameStream = false;
						for (Postings posting : postingsListTerm) 
						{
							if (posting.getFileID() == fileID) {
								posting.incrementOccurences();
								posting.setTfMap(tokenText, 1);
								sameStream = true;
								break;
							} 
							else 
							{
								continue;
							}
						}
						
						if (sameStream == false) {
							Postings p = new Postings(fileID);
							p.setTfMap(tokenText, 1);
							postingsListTerm.add(p);
							this.authorIndex.put(tokenNo, postingsListTerm);
						}

					}
					
					else 
					{
						postingsListTerm = new LinkedList<Postings>();
						Postings element = new Postings(fileID);
						element.setTfMap(tokenText, 1);
						postingsListTerm.add(element);
						this.authorIndex.put(tokenNo, postingsListTerm);
					}

				}

			} catch (TokenizerException e) {
				e.printStackTrace();
			}

		} 
		
		}	// END OF CASE AUTHOR
		

		
		
		
	} // END of ANALYZE method for AUTHOR
	
}


