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
	
	
	// Inner class postings to store postings information
	class Postings implements Serializable
	{
		private int noOfOccurences = 1;
		private int fileMappedID;
		
		public Postings(int fileID)
		{
			this.fileMappedID = fileID; 
		}
		
		public int getOccurences()
		{
			return this.noOfOccurences;
		}
		
		public void incrementOccurences()
		{
			noOfOccurences++;
		}
		
		public int getFileID()
		{
			return this.fileMappedID;
		}
		
		
	}
	
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
		termDictionary = new HashMap<String, Integer>();
		categoryDictionary = new HashMap<String, Integer>();
		placeDictionary = new HashMap<String, Integer>();
		authorDictionary = new HashMap<String, Integer>();
		
		// Allocating memory for the indexes (4 in total)
		termIndex = new HashMap<Integer, LinkedList<Postings>>();
		categoryIndex = new HashMap<Integer, LinkedList<Postings>>();
		authorIndex = new HashMap<Integer, LinkedList<Postings>>();
		placeIndex = new HashMap<Integer, LinkedList<Postings>>();
		
		// Allocating memory for the kTermMap
		this.kTermMap = new HashMap<String, Integer>();
		
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
		this.fileIDDictionary.put(fileID, this.fileIDAssigner);
		this.inverseFileIDDictionary.put(this.fileIDAssigner, fileID);
		this.fileIDAssigner++;
		
		int idCat;
		
		// putting the category id : postings into the category index
		
		int idFile = fileIDDictionary.get(fileID);
		
		if (d.getField(FieldNames.CATEGORY) != null)
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
		
		
		// DONE with the category index, now moving on to indexing TITLE, CONTENT, AUTHORORG 
		// together into the term index for the given DOCUMENT
		
		if (d.getField(FieldNames.TITLE) != null)
		{
			analyzeField(FieldNames.TITLE, d.getField(FieldNames.TITLE)[0], idFile);
		}
		
		if (d.getField(FieldNames.CONTENT) != null)
		{
			analyzeField(FieldNames.CONTENT, d.getField(FieldNames.CONTENT)[0], idFile);
		}
		
		if (d.getField(FieldNames.PLACE) != null)
		{
			analyzeField(FieldNames.PLACE, d.getField(FieldNames.PLACE)[0], idFile);
		}
		
		if (d.getField(FieldNames.NEWSDATE) != null)
		{
			analyzeField(FieldNames.NEWSDATE, d.getField(FieldNames.NEWSDATE)[0], idFile);
		}
		
		if (d.getField(FieldNames.AUTHORORG) != null)
		{
			analyzeField(FieldNames.AUTHORORG, d.getField(FieldNames.AUTHORORG)[0], idFile);
		}
		
		if (d.getField(FieldNames.AUTHOR) != null)
		{
			analyzeField(FieldNames.AUTHOR, d.getField(FieldNames.AUTHOR), idFile);
		}
		
		
		
		
		
		
	}
	
	/**
	 * Method that indicates that all open resources must be closed
	 * and cleaned and that the entire indexing operation has been completed.
	 * @throws IndexerException : In case any error occurs
	 */
	public void close() throws IndexerException {
		
		//TODO
		try{
			// writing term index and dictionary to file
			FileOutputStream fos = new FileOutputStream(this.indexDirectory+File.separator+"termIndex.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.termIndex);
	        oos.writeObject(this.termDictionary);
	        fos.close();
	        
	        
	        // writing author index and dictionary to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"authorIndex.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.authorIndex);
	        oos.writeObject(this.authorDictionary);
	        fos.close();
	        
	        // writing place index and dictionary to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"placeIndex.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.placeIndex);
	        oos.writeObject(this.placeDictionary);
	        fos.close();
	        
	        // writing category index and dictionary to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"categoryIndex.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.categoryIndex);
	        oos.writeObject(this.categoryDictionary);
	        fos.close();
	        
	        // writing KTermMap to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"kTermMap.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.kTermMap);
	        fos.close();
	        
	        
	        // writing fileIDDictionary to file
	        fos = new FileOutputStream(this.indexDirectory+File.separator+"fileIDDict.ser");
	        oos = new ObjectOutputStream(fos);
	        oos.writeObject(this.fileIDDictionary);
	        oos.writeObject(this.inverseFileIDDictionary);
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
						for (Postings posting : postingsListTerm) {
							if (posting.getFileID() == fileID) {
								posting.incrementOccurences();
								sameStream = true;
								break;
							} else {
								continue;
							}
						}
						if (sameStream == false) {
							postingsListTerm.add(new Postings(fileID));
							this.termIndex.put(tokenNo, postingsListTerm);
						}

					} else {
						postingsListTerm = new LinkedList<Postings>();
						Postings element = new Postings(fileID);
						postingsListTerm.add(element);
						this.termIndex.put(tokenNo, postingsListTerm);
					}

				}

			} catch (TokenizerException e) {
				e.printStackTrace();
			}

		} // END OF CASE TITLE / CONTENT / AUTHORORG / NEWSDATE
		
				
		if(fn == FieldNames.PLACE) {
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

					// if the category is already in the category index, then
					// add the new file to the keys
					// equivalent postings list
					if (this.placeIndex.containsKey(tokenNo)) {

						postingsListTerm = this.placeIndex.get(tokenNo);
						// scanning through postings list to find if file is
						// already there
						// if so, increment the number of occurences of it
						for (Postings posting : postingsListTerm) 
						{
							if (posting.getFileID() == fileID) {
								posting.incrementOccurences();
								sameStream = true;
								break;
							} 
							else 
							{
								continue;
							}
						}
						
						if (sameStream == false) {
							postingsListTerm.add(new Postings(fileID));
							this.placeIndex.put(tokenNo, postingsListTerm);
						}

					}
					
					else 
					{
						postingsListTerm = new LinkedList<Postings>();
						Postings element = new Postings(fileID);
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

					// if the category is already in the category index, then
					// add the new file to the keys
					// equivalent postings list
					if (this.authorIndex.containsKey(tokenNo)) {

						postingsListTerm = this.authorIndex.get(tokenNo);
						// scanning through postings list to find if file is
						// already there
						// if so, increment the number of occurences of it
						for (Postings posting : postingsListTerm) 
						{
							if (posting.getFileID() == fileID) {
								posting.incrementOccurences();
								sameStream = true;
								break;
							} 
							else 
							{
								continue;
							}
						}
						
						if (sameStream == false) {
							postingsListTerm.add(new Postings(fileID));
							this.authorIndex.put(tokenNo, postingsListTerm);
						}

					}
					
					else 
					{
						postingsListTerm = new LinkedList<Postings>();
						Postings element = new Postings(fileID);
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
