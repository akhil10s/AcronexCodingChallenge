package com.acronex.challenge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;

public class TestGedcomParser extends TestCase{
	GedcomParser parser;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		parser = new GedcomParser();
	}
	
	
	public void testParser_errorOpeningFiles(){
		//case1: wrong input file path
		try{
			parser.processFile("invalidPath", "output.xml");
			fail("error");
		}catch (RuntimeException e){
			assertEquals("File not found at the specified path: invalidPath", e.getMessage());
		}
		
		//case2: wrong output file path
		try{
			parser.processFile("./src/com/acronex/challenge/sample/input_perfect.txt", "D:/random/output.xml");
			fail("error");
		}catch (RuntimeException e){
			assertEquals("Error opening file at path: D:/random/output.xml", e.getMessage());
		}
	}
	
	
	public void testParser_errorInFile(){
		//invalid level test
		try{
			parser.processFile("./src/com/acronex/challenge/sample/input_invalidLevel.txt", "output.xml");
			fail("error");
		}catch (RuntimeException e){
			assertEquals("Data in the file is corrput!", e.getMessage());
		}
		
		//missing data for referred nodes
		try{
			parser.processFile("./src/com/acronex/challenge/sample/input_missingData.txt", "output.xml");
			fail("error");
		}catch (RuntimeException e){
			assertEquals("Data for few node(s) is missing, invalid xml", e.getMessage());
		}
	}
	
	
	public void testParser_Sucess() throws IOException{
		//test for the output given by parser for sample input which as white space etc.
		parser.processFile("./src/com/acronex/challenge/sample/input_perfect.txt", "./src/com/acronex/challenge/sample/output_test.xml");
		//test the output of the parser against the expected output file
		String expectedOutput = FileUtils.readFileToString(new File("./src/com/acronex/challenge/sample/output_perfect.xml"));
		String actualOutput = FileUtils.readFileToString(new File("./src/com/acronex/challenge/sample/output_test.xml"));
		 assertEquals(expectedOutput, actualOutput);
	}

	
}
