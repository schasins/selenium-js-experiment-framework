package javaScript;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.google.common.base.Joiner;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class JavaScriptTestingSerialTime {
	
	public static void main(String[] args) {
		//Input 1
		List<String[]> rows = new ArrayList<String[]>();
		try {
			CSVReader reader = new CSVReader(new FileReader("resources/input2.csv"), '\t');
		    rows = reader.readAll();
		}
		catch(Exception e){
			System.out.println("Failed to open input file.");
			return;
		}
		
		//Input 2
		String javaScriptFunction;
		try{
			javaScriptFunction = new Scanner(new File("resources/titleExtractor.js")).useDelimiter("\\Z").next();
		}
		catch(Exception e){System.out.println("Failed to open JavaScript input file."); return;}
		
		//Output
		PrintWriter writer;
		try{
			String csv = "resources/output-seq.csv";
			writer = new PrintWriter(csv);
		}
		catch(Exception e){
			System.out.println("Failed to open output file.");
			return;
		}
		
		//Execution
		long t0 = System.currentTimeMillis();
		WebDriver driver = new FirefoxDriver();
		long t1 = System.currentTimeMillis();

		String header = "title" + ";" + 
				"start-up" + ";" + 
				"load" + ";" + 
				"read-args" + ";" + 
				"execute" + ";";
		writer.println(header);
		header = "LOAD;" + String.valueOf(t1 - t0);
		writer.println(header);
		
		for (int i = 0; i<rows.size(); i++){
			if (driver instanceof JavascriptExecutor) {
				String[] row = rows.get(i);
				String url = row[0];
				if (!url.startsWith("http")){url = "http://"+url;}
				long t2 = System.currentTimeMillis();
		        driver.get(url);
				long t3 = System.currentTimeMillis();
				
		        for(int j = 1; j < row.length; j++){
		            row[j] = "'"+row[j]+"'";
		        }
				String argString = Joiner.on(",").join(Arrays.copyOfRange(row, 1, row.length));
				long t4 = System.currentTimeMillis();
				Object ans = ((JavascriptExecutor) driver).executeScript(javaScriptFunction+" return func("+argString+");");
				long t5 = System.currentTimeMillis();
				
				String ansStr = ans.toString() + ";0;" + 
						String.valueOf(t3 - t2) + ";" + 
						String.valueOf(t4 - t3) + ";" + 
						String.valueOf(t5 - t4) + ";";
				
				writer.println(ansStr);
			}
		}
		
		//Close output writer
		try{writer.close();}catch(Exception e){System.out.println("Failed to close output file.");}
        //Close the browser
        driver.quit();
		long stop = System.currentTimeMillis();
		System.out.println(stop - t0);
		System.out.println("milliseconds");
	}

}
