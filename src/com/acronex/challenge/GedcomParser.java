package com.acronex.challenge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * This class parses the flat file in a code challenge format to a
 * nested xml file. It also does validation on the data format and
 * if all required data is avaialble or not
 * @author Akhil.Gupta
 *
 */
public class GedcomParser {
	Logger logger = Logger.getLogger("GedcomParser");

	private int currentLevel;
	private int lastLevel;
	private Pattern idPattern = Pattern.compile("@\\w+@");
	private final static String ROOT_NODE_OPEN="<GEDCOM>";
	private final static String ROOT_NODE_CLOSE="</GEDCOM>";
	private final static String OPEN_TAG="<";
	private final static String CLOSE_TAG="</";
	private final static String END_TAG=">";
	private final static String WHITE_SPACE=" ";
	private final static String QUOTATION="\"";
	private final static String EQUAL_TO="=";
	private final static String TAB="\t";
	private Stack<String> tagStack = new Stack<String>();
	private Set<String> idReferenceSet = new HashSet<String>();
	private Set<String> idSet = new HashSet<String>();
	
	/**
	 * Enum representing the different xml attributes to be used
	 * @author Akhil.Gupta
	 *
	 */
	private enum XmlAttribute{
		ID,
		IDREF,
		VALUE;
	}

	public void parseFile() throws IOException {
		System.out.print("Enter path of the file to be read: ");
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		String inputFilePath = userInput.readLine();
		logger.info("path entered for the input file: "+ inputFilePath);
		
		System.out.print("Enter the path of the output file: ");
		String outputFilePath = userInput.readLine();
		logger.info("path entered for the input file: "+ outputFilePath);
		
		//if both user inputs are fine, processFile
		processFile(inputFilePath, outputFilePath);
	}
	
	protected void processFile(String inputFilePath, String outputFilePath) {

		BufferedReader reader = getReaderForFile(inputFilePath);
		BufferedWriter writer = getFileWriter(outputFilePath);
		
		try {
			logger.info("opening the root node");
			writer.write(ROOT_NODE_OPEN); //root node for the parser
			String line = null;
			while ((line = reader.readLine()) != null) {
				if(line.trim().length() != 0) //skip blank line processing
					processCurrentLine(writer,line);
			 }
			//close all open tags after last line of the file is read
			int i=0;
			logger.info("parsed file till end, closing all open tags");
			while(!tagStack.isEmpty()){
				String closeTagName =getClosingTagByName(tagStack.pop(),i);
				logger.info("closing tag: "+closeTagName);
				writer.write(closeTagName);
				writer.newLine();
				i++;
			}
		} catch (IOException e) {
			logger.severe("An error occured while reading/writting the/to file ");
			throw new RuntimeException("An error occured while processing the file");
		}
		// Verify if xml data is valid, if no FAM node with id "@1235@" is defined and a node line this (<FAM idref="@1245@"></FAM>) exists,
		//code will throw error 

		if(!idSet.containsAll(idReferenceSet))
			throw new RuntimeException("Data for few node(s) is missing, invalid xml");
				
		//if everything is fine, close the writer
		try {
			logger.info("closing the root node");
			writer.write(ROOT_NODE_CLOSE); //closing the root node
			writer.close();
		} catch (IOException e) {
			logger.severe("An error occured while close the file stream");
			throw new RuntimeException("An error occured while closing the file");
		}
		System.out.println("File parsed succefully!!");	  
	}

	/** Method parse the passed line from the file as per it current level
	 * and writes it into xml form to the output file.
	 * It will also close the xml tags as required.
	 * 
	 * Throws exception if any line is not as per the pre-defined format. It also
	 * throws exception in case any of the nested element data is not found. For e.g.
	 * @param line
	 * @throws IOException 
	 */
	private void processCurrentLine(BufferedWriter writer, String line) throws IOException{
		line=line.trim();
		logger.info("processing line: "+line);
		String []data = line.split("\\s",3); //split by white space into three parts
		List<String> dataList=new ArrayList(Arrays.asList(data));
		String level = dataList.remove(0);
		//check if level is integer or not
		try{
		 currentLevel = Integer.valueOf(level).intValue() +1; //add one as root node is opened	
		 logger.info("current level: "+currentLevel +" lastLevel: "+lastLevel);
		}catch (NumberFormatException nfe) {
			logger.severe("error reading current level of a line");
			throw new RuntimeException("File is not in defined format, level value not found at start of line");
		}

		//get the no. of xml tags that should be close before this line
		int countOfTagsToClose = numberOfElementsToBeClosed();
		logger.info("no. of open tags to close before writting current line: "+countOfTagsToClose);
		
		if(countOfTagsToClose==0) writer.newLine();
		
		if(!tagStack.isEmpty()){
			for(int i=0;i<countOfTagsToClose;i++){
				writer.write(getClosingTagByName(tagStack.pop(),i));
				writer.newLine();
			}	
		}
		
		String tagOrId = dataList.remove(0);
		String content = dataList.size()!=0?dataList.remove(0):null;
		
		//check if element after level is tag or id, if its is id, create element with id attribute with tag name as content
		if(isId(tagOrId)){
			logger.info(String.format("creating an element(%s) with attribute id(%s)",content,tagOrId));
			tagStack.push(content);
			idSet.add(tagOrId); //maintains a set of id of all elements in xml document
			writer.write(getElementWithAttribute(content, XmlAttribute.ID, tagOrId));
		}else{
			//as second element is tag, check if content is of type ID, in that case it would refer to another node in xml
			tagStack.push(tagOrId);

			if(content!=null && content.length()>0 && isId(content)){
				logger.info(String.format("creating an element(%s) with attribute idref(%s)",tagOrId,content));
				idReferenceSet.add(content); //maintains a set of all id's referred in the by other elements in xml document
				writer.write(getElementWithAttribute(tagOrId, XmlAttribute.IDREF, content));
			}else{
				logger.info(String.format("creating an element(%s) with attribute value(%s)",tagOrId,content));
				writer.write(getElementWithAttribute(tagOrId, XmlAttribute.VALUE, content));
			}
			
		}
		lastLevel = currentLevel;
	}
	
	/**
	 * checks if the passed string is of type id
	 * @param tagOrId
	 * @return
	 */
	private boolean isId(String tagOrId){
		Matcher m = idPattern.matcher(tagOrId);
		return m.matches();
	}
	
	/*
	 * prepares a closing xml tag for the passed tag name with the proper indentation
	 */
	private String getClosingTagByName(String tagName, int closeTagIndex){
		StringBuffer closeTag = new StringBuffer();
		//indent the element as per the current level of the closing node
		for(int i=0;closeTagIndex!=0&&i<lastLevel-closeTagIndex;i++){
			closeTag.append(TAB);
		}
		closeTag.append(CLOSE_TAG);
		closeTag.append(tagName);
		closeTag.append(END_TAG);
		return closeTag.toString();
	}
	
	/**
	 * Prepares an xml tag with the passed attribute name and its value and the indentation required
	 * @param tagName
	 * @param attr
	 * @param value
	 * @return
	 */
	private String getElementWithAttribute(String tagName, XmlAttribute attr, String value){
		StringBuffer element = new StringBuffer();
		//indent the element as per the current level of the node
		for(int i=0;i<currentLevel;i++){
			element.append(TAB);
		}
		
		element.append(OPEN_TAG);
		element.append(tagName);
		//add the attribute only if value is null
		
		if(value!=null && value.length()>0){
			element.append(WHITE_SPACE);
			element.append(attr.name().toLowerCase());
			element.append(EQUAL_TO);
			element.append(QUOTATION);
			element.append(JavaUtils.escapeSpecialCharacters(value));
			element.append(QUOTATION);
		}
		
		element.append(END_TAG);
		return element.toString();
	}
	
	/**
	 * checks if data read from the current line is valid as per level.
	 * a) Level for a line being read can be same or +1 than last level
	 * b) Level for the current line can be less than 
	 * @return
	 */
	private int numberOfElementsToBeClosed(){
		if((currentLevel==lastLevel)){
			return 1; //as current level of the xml node is same as previous node, we can close the previous node
		}else if(currentLevel == (lastLevel+1)){
			return 0; //node being written is child of previous node, nothing should be closed
		}else if(currentLevel <lastLevel && currentLevel>=0){
			return lastLevel-currentLevel +1; //case where 0-->1-->2-->3 where open and current node is -->1 (we should close, node 3, 2, 1)
		}
		//if none of the conditions above are true that means data is not in correct format, throw exception
		logger.severe("Data in the file is corrput!");
		throw new RuntimeException("Data in the file is corrput!");
	}
	
	private BufferedWriter getFileWriter(String fileName){
    	OutputStreamWriter fstream=null;
		try {
			fstream = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");
		} catch (IOException e) {
			logger.severe("Error opening file at path: "+fileName);
			throw new RuntimeException("Error opening file at path: "+fileName);
		}
    	BufferedWriter out = new BufferedWriter(fstream);
    	return out;
    }
	
    private BufferedReader getReaderForFile(String filePath){
		BufferedReader br  = null;
		try{
			File f = new File(filePath);
			br = new BufferedReader(new FileReader(f));
		}catch (Exception ex){
			logger.severe("File not found");
			throw new RuntimeException("File not found at the specified path: "+filePath);
		}
		return br;
	}

	public static void main(String args[]) throws IOException{
		GedcomParser g = new GedcomParser();
		g.logger.setLevel(Level.FINE);
		g.parseFile();
	}
}
