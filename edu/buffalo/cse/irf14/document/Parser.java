/**
 * 
 */
package edu.buffalo.cse.irf14.document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nikhillo
 * Class that parses a given file into a Document
 */
public class Parser {
	/**
	 * Static method to parse the given file into the Document object
	 * @param filename : The fully qualified filename to be parsed
	 * @return The parsed and fully loaded Document object
	 * @throws ParserException In case any error occurs during parsing
	 */
	// Variable that keeps track of the amount of parsing done for the text file
	
	
	
	public static Document parse(String filename) throws ParserException {
		// TODO YOU MUST IMPLEMENT THIS
		// girish - All the code below is mine
			
		// Creating the object for the new Document
		
		
		if (filename == null){
			throw new ParserException();
		}
		
		// Variable indexPos to track the current pointer location in the String Array
		int indexPos = 0;
		
		// Creating an instance of the document class to store the parsed content
		Document d = new Document();
		
		// to store the result sent from the regexAuthor method and regexPlaceDate method
		Object[] resultAuthor = {null, null, null};
		Object[] resultPlaceDate = {null, null, null};
		StringBuilder news = new StringBuilder();
		
		// Next 4 lines contains the code to get the fileID and Category metadata
		String[] fileCat = new String[2];
		
		fileCat = regexFileIDCat("/((?:[a-z]|-)+)/([0-9]{7})", filename);
		// System.out.println(filename);
		// throw an exception if the file is blank, junk or null
		if (fileCat[0] == null){
			throw new ParserException();
		}
		d.setField(FieldNames.CATEGORY, fileCat[0]);
		d.setField(FieldNames.FILEID, fileCat[1]);
		
		
		// Now , parsing the file
		File fileConnection = new File(filename);
		
		// newscollated - it will store the parsed file content on a line by line basis
		ArrayList<String> newscollated = new ArrayList<String>();
		
		// String that stores the content obtained from the . readline() operation
		String temp = null;
		
		try
		{
			
			BufferedReader getInfo = new BufferedReader(new FileReader(fileConnection));
			while ((temp = getInfo.readLine()) != null)
			{
				if(temp.equals(""))
				{
					continue;
				}
				else
				{	
					temp = temp + " ";
					newscollated.add(temp);
				}
				
				
			}
			getInfo.close();
			
			//-- deprecated (REMOVE THIS IF OTHER WORKS)
			// setting the title using the arraylist's 0th index element
			//d.setField(FieldNames.TITLE, newscollated.get(0));
			
			// Appending the lines into one big string using StringBuilder
			
			for (String n: newscollated)
			{
				news.append(n);
			}
			
			
			// Obtaining the TITLE of the file
			Object[] titleInfo = new Object[2];
			
			titleInfo = regexTITLE("([^a-z]+)\\s{2,}", news.toString());
			d.setField(FieldNames.TITLE, titleInfo[0].toString().trim());
			indexPos = (Integer) titleInfo[1];
			
			
			
			
			
			
			
			// Getting the Author and Author Org
			resultAuthor = regexAuthor("<AUTHOR>(.*)</AUTHOR>", news.toString());
			
			if (resultAuthor[0] != null)
			{
				d.setField(FieldNames.AUTHOR, resultAuthor[0].toString());
			}
			
			if (resultAuthor[1] != null)
			{
				d.setField(FieldNames.AUTHORORG, resultAuthor[1].toString());
			}
		    
		    
		    
		    if ((Integer) resultAuthor[2] != 0)
		    {
		    	indexPos = (Integer) resultAuthor[2];
		    }
			
		    
		    
		    // Getting the Place and Date
 			resultPlaceDate = regexPlaceDate("\\s{2,}(.+),\\s(?:([A-Z][a-z]+\\s[0-9]{1,})\\s{1,}-)", news.toString());
 			
 			if (resultPlaceDate[0] != null)
 			{
 				d.setField(FieldNames.PLACE, resultPlaceDate[0].toString().trim());
 			}
 		    
 			if (resultPlaceDate[1] != null)
 			{
 				d.setField(FieldNames.NEWSDATE, resultPlaceDate[1].toString().trim());
 			}
 			
 		    // getting the content
 		    
 		    if ((Integer) resultPlaceDate[2] != 0)
		    {
		    	indexPos = (Integer) resultPlaceDate[2];
		    }
 		    
 		    
 		    d.setField(FieldNames.CONTENT, news.substring(indexPos + 1));
 		    
 		    return d;
 		    
		    
		    
			
			
		}
		catch(FileNotFoundException e){
			throw new ParserException();
			
		}

		catch(IOException e){
			throw new ParserException();
		}
		
		catch(Exception e){
			e.printStackTrace();
			
		}
		return d;
		
		
		
		
		
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
	
	
	// Gets the fileID and category
	public static String[] regexFileIDCat(String theRegex, String str2Check)
	{
		String[] fileCatReturn = {null, null};
		String splitString;
    	String[] fileCat = {null, null};
		Pattern checkRegex = Pattern.compile(theRegex);
    	Matcher regexMatcher = checkRegex.matcher(str2Check);
    	
    	while (regexMatcher.find())
    	{
    		if(regexMatcher.group().length() != 0)
    		{
    			fileCatReturn[0] = regexMatcher.group(1).trim();
    			fileCatReturn[1] = regexMatcher.group(2).trim();
    			return fileCatReturn;
    		}
    		
    	}
    	return fileCatReturn;
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
	
}
