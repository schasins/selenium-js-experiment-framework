package javaScript;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class JavaScriptTestingParallelWorkStealing {
	List<String[]> rows;
	TaskQueue queue;
	String javaScriptFunctions;
	int algorithms;
	List<Integer> subalgorithms;
	CSVWriter writer;
	Boolean jquery;
	Boolean screenshot;
	String screenshotDir;
	int stages;
	int secondsLimit;
	static int rowCounter;
	
	// JavaScript for DOM Modification
	static String DOMModifierFunctions;
	static int DOMChange;
	
	//String path_to_proxyserver = "/home/mangpo/work/262a/httpmessage/";
	//String path_to_proxyserver = "/home/sarah/Dropbox/Berkeley/research/similarityAlgorithms/cacheall-proxy-server/";
	//String path_to_proxyserver = "~/research/cacheall-proxy-server/";
	//String path_to_proxyserver = "/home/eecs/schasins/research/cacheall-proxy-server/";
	String path_to_proxyserver = "/scratch/schasins-cache/cacheall-proxy-server/";

	// Number of done jobs
	static int finishedJobs;
	
	JavaScriptTestingParallelWorkStealing() {
		stages = 0;
		this.DOMChange = 0;
	}
	
	/*
	JavaScriptTestingParallelWorkStealing(int DOMChange) {
		stages = 0;
		this.DOMChange = DOMChange;
		try {
			this.DOMModifierFunctions = new Scanner(new File("src/javaScript/DOMModifier.js")).useDelimiter("\\Z").next();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to open input src/javaScript/DOMModifier.js.");
			System.exit(1);
		}
	}
	*/
	
	public static void clearTmpFiles(){
		//let's clear out the tmp directory where profiles can save and accumulate too many and cause crashes
		System.out.println("Clearing tmp directory of profiles.");
		
		File folder = new File("/tmp");
		File[] listOfFiles = folder.listFiles();
		int delCounter = 0;
	    for (int i = 0; i < listOfFiles.length; i++) {
	      if (listOfFiles[i].isDirectory() && (listOfFiles[i].getName().contains("webdriver-profile") || listOfFiles[i].getName().contains("userprofile")))  {
	        delCounter++;
	        try {
				FileUtils.deleteDirectory(listOfFiles[i]);
			} catch (IOException e) {
				System.out.println("Couldn't delete the directory: "+listOfFiles[i].getName());
			}
	      }
	    }
	    System.out.println("Cleared "+delCounter+" files.");
	}
	
	public static void clearTmpScreenshots(){
		System.out.println("Clearing tmp directory of screenshots.");
		File folder = new File("/tmp");
		File[] listOfFiles = folder.listFiles();
		int delCounter = 0;
	    for (int i = 0; i < listOfFiles.length; i++) {
	      if (listOfFiles[i].getName().contains("screenshot"))  {
	        delCounter++;
	        try {
				FileUtils.forceDelete(listOfFiles[i]);
			} catch (IOException e) {
				System.out.println("Couldn't delete the file: "+listOfFiles[i].getName());
			}
	      }
	    }
	    System.out.println("Cleared "+delCounter+" files.");
	}
	
	public void stage(String inputFile, String javaScriptFile, String outputFile, Boolean jquery, int threads, int secondsLimit, Boolean screenshot, String screenshotDir){
		clearTmpFiles();
		
		this.stages ++;
		System.out.println("STAGE "+this.stages);
		
		this.algorithms = 0;
		this.subalgorithms = new ArrayList<Integer>();
		
		this.secondsLimit = secondsLimit;
		
		//Input 1
		List<String[]> rows = new ArrayList<String[]>();
		try {
			//CSVReader reader = new CSVReader(new FileReader(inputFile),',','\'','\0');
			CSVReader reader = new CSVReader(new FileReader(inputFile),',','\'','\\');
		    rows = reader.readAll();
		}
		catch(Exception e){
			System.out.println("Failed to open input file.");
			return;
		}
		this.rows = rows;
		this.queue = new TaskQueue(this.rows);

		//Input 2
		try{
			this.javaScriptFunctions = new Scanner(new File(javaScriptFile)).useDelimiter("\\Z").next();
		}
		catch(Exception e){System.out.println("Failed to open JavaScript input file."); return;}
		this.algorithms = 0;
		//while loop for number of algorithms
		while(true){
			char letter = ((char) ((int) 'a' + this.algorithms));
			if(this.javaScriptFunctions.contains("func_"+letter)){
				this.algorithms++;
				this.subalgorithms.add(0);
				//while loop for number of subalgorithms for each algorithm
				while(true){
					int count = this.subalgorithms.get(this.algorithms-1);
					//System.out.println("func_"+letter+(count+1));
					//System.out.println(this.subalgorithms);
					if(this.javaScriptFunctions.contains("func_"+letter+(count+1))){
						//System.out.println("present");
						//System.out.println(this.subalgorithms);
						this.subalgorithms.set(this.algorithms-1, count+1);
					}
					else{
						//System.out.println("not present");
						//System.out.println(this.subalgorithms);
						break;
					}
				}
			}
			else{
				break;
			}
		}
		System.out.println("algorithms: "+this.algorithms);
		System.out.println("subalgorithms: "+Arrays.toString(this.subalgorithms.toArray()));
		
		//Output
		CSVWriter writer;
		try{
			writer = new CSVWriter(new FileWriter(outputFile),',','\'','\\');
			//writer = new CSVWriter(new FileWriter(outputFile),',','\'','\0');
		}
		catch(Exception e){
			System.out.println("Failed to open output file.");
			return;
		}
		this.writer = writer;
		
		this.jquery = jquery;
		this.screenshot = screenshot;
		this.screenshotDir = screenshotDir;
		
		this.execute(threads);
	}
	
	public synchronized static int newRowId(){
		rowCounter++;
		return rowCounter;
	}
	
	public void execute(int threads){
		long start = System.currentTimeMillis();
		this.rowCounter = 0;
		ArrayList<Thread> threadList = new ArrayList<Thread>();
		
		for (int i = 0; i < threads; i++){
			RunTests r = new RunTests(this.queue,this.javaScriptFunctions, this.algorithms, this.subalgorithms, this.writer, this.jquery, this.secondsLimit, this.screenshot, this.screenshotDir, i);
	        Thread t = new Thread(r);
	        threadList.add(t);
	        t.start();
		}
		
		//barrier
		for (Thread thread : threadList) {
		    try {thread.join();} catch (InterruptedException e) {System.out.println("Could not join thread.");}
		}

		long stop = System.currentTimeMillis();
		String[] times = new String[1];
		times[0] = String.valueOf((stop-start)/1000);
		//this.writer.writeNext(times);
		
		//Close output writer
		try{writer.close();}catch(Exception e){System.out.println("Failed to close output file.");}			
	}
	
	public void execWrapper(String[] args){
		try {
			Process p = Runtime.getRuntime().exec(args);
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			System.out.println(e.toString().split("\n")[0]);
		}
	}
	
	public void startSession(){
		System.out.println("Starting session.");
		try {
			String[] shCommand = {"/bin/sh", "-c", "mkdir " + path_to_proxyserver + ".cache"}; 
			System.out.println(shCommand[2]);
			Process p = Runtime.getRuntime().exec(shCommand);
			p.waitFor();
			p = Runtime.getRuntime().exec(shCommand);
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println(e.toString().split("\n")[0]);
		}
		
		// start the SSL stripping
		String[] shCommand = {"screen", "-S", "sslstrip", "-X", "quit"};
		String[] shCommand2 = {"screen", "-S", "sslstrip", "-d", "-m", "python", "~/research/sslstrip-0.9/sslstrip.py".replace("~", System.getProperty("user.home")), "-l", "1235"}; 
		execWrapper(shCommand);
		execWrapper(shCommand2);
		
		// start the cache
		String[] shCommand3 = {"screen", "-S", "cacheall", "-X", "quit"};
		String[] shCommand4 = {"rm", "-rf", path_to_proxyserver + ".cache"}; // let's just make sure there's nothing where we want to build our cache (like an old cache that never got cleaned up)
		String[] shCommand5 = {"mkdir", path_to_proxyserver + ".cache"}; 
		String[] shCommand6 = {"screen", "-S", "cacheall", "-d", "-m", "python", "/scratch/schasins-cache/cacheall-proxy-server/proxyserv.py", "-d", path_to_proxyserver + ".cache"}; 
		execWrapper(shCommand3);
		execWrapper(shCommand4);
		execWrapper(shCommand5);
		execWrapper(shCommand6);
		
		// give the cache time to get set up
		try {
		    Thread.sleep(2000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}

		/*try {
			String command = "python proxyserv.py -c -i "+String.valueOf(System.currentTimeMillis());
			System.out.println(command);
			this.proxyserv = Runtime.getRuntime().exec(command,null, new File(path_to_proxyserver));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	public void endSession(){
		System.out.println("Ending session.");
		clearTmpScreenshots();
		Date date = new Date();
		String cache_dir = "arch_" + date.toString().replace(" ", "_");

		String[] shCommand = {"rm", "-rf", path_to_proxyserver + ".cache"}; // move is being crazy slow.  for now, use this
		//String[] shCommand = {"/bin/sh", "-c", "mv " + path_to_proxyserver + ".cache " + path_to_proxyserver +"caches/" + cache_dir}; 
		System.out.println("About to clear cache.");
		execWrapper(shCommand);
		System.out.println("Cache cleared.");
		System.out.println("Ended session.");
		return;
	}
	
	private class TaskQueue {
		List<String[]> rows;
		int count, timeout;
		long start;
		String currentURL;
		int rowsSinceCacheRestart;
		
		public TaskQueue(List<String[]> rows){
		    Comparator<String[]> comparator_rows = new Comparator<String[]>() {

		        @Override
		        public int compare(String[] r1, String[] r2) {
		            String i = r1[0];
		            String j = r2[0];
		            return i.compareTo(j);
		        }

		    };
		    Collections.sort(rows, comparator_rows);
			this.rows = rows;
			this.currentURL = this.rows.get(0)[0];
			this.rowsSinceCacheRestart = 0;
			
			// Comment out this line for scailability test
			/*this.count = 0;
			this.timeout = 0;
			start = System.currentTimeMillis();
			try {
				PrintWriter output = new PrintWriter(new FileWriter("time.csv"));
				output.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
		
		public synchronized String[] pop(){
			if (this.rows.size()>0){
				String[] row = this.rows.get(0);
				String newUrl = row[0];
				// restarting the cache within a session is fine, as long as we keep using the same cache dir
				// so it won't reduce the number of files we're searching, but handles cache prog slowing
				
				if (newUrl.equals(this.currentURL) || this.rowsSinceCacheRestart < 100){
					this.rowsSinceCacheRestart += 1;
				}
				else{
					// have to restart the cache
					this.currentURL = newUrl;
					this.rowsSinceCacheRestart = 0;
					
					// give the other threads a while to finish
					try {
					    Thread.sleep(5000);                 //1000 milliseconds is one second.
					} catch(InterruptedException ex) {
					    Thread.currentThread().interrupt();
					}
					
					// restart the cache
					String[] shCommand3 = {"screen", "-S", "cacheall", "-X", "quit"};
					String[] shCommand4 = {"screen", "-S", "cacheall", "-d", "-m", "python", "/scratch/schasins-cache/cacheall-proxy-server/proxyserv.py", "-d", path_to_proxyserver + ".cache"}; 
					//String[] shCommand4 = {"screen", "-S", "cacheall", "-d", "-m", "python", "/scratch/schasins-cache/cacheall-proxy-server/proxyserv.py"}; 
					execWrapper(shCommand3);
					execWrapper(shCommand4);
					
					// give the cache a while to start up
					try {
					    Thread.sleep(2000);                 //1000 milliseconds is one second.
					} catch(InterruptedException ex) {
					    Thread.currentThread().interrupt();
					}
				}
				
				rows = rows.subList(1, rows.size());
				return row;
			}
			return null;
		}
		
		// Scalability test only
		public synchronized void timeout() {
			timeout++;
		}

		// Scalability test only
		public synchronized void done() {
			count++;
			if(count%100 == 0) {
				try {
					PrintWriter output = new PrintWriter(new FileWriter("time.csv", true));
					output.write(count + "," + (System.currentTimeMillis()-start)/1000);
					output.write("," + timeout );
					output.write("," + new File(path_to_proxyserver + ".cache").list().length + "\n");
					output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println(e.toString().split("\n")[0]);
				}
			}
		}
		
		public boolean empty(){
			return this.rows.size() == 0;
		}
	}
	
	private static class RunTests implements Runnable {
		TaskQueue queue;
		String javaScriptFunction;
		int algorithms;
		List<Integer> subalgorithms;
		CSVWriter writer;
		Boolean jquery;
		Boolean verbose = true;
		int secondsLimit;
		Boolean screenshot;
		String screenshotDir;
		int index;
		
		RunTests(TaskQueue queue, String javaScriptFunction, int algorithms, List<Integer> subalgorithms, CSVWriter writer, Boolean jquery, int secondsLimit, Boolean screenshot, String screenshotDir, int i){
			this.queue = queue;
			this.javaScriptFunction = javaScriptFunction;
			this.writer = writer;
			this.algorithms = algorithms;
			this.subalgorithms = subalgorithms;
			this.jquery = jquery;
			this.secondsLimit = secondsLimit;
			this.screenshot = screenshot;
			this.screenshotDir = screenshotDir;
			this.index = i;
		}
		
		public void print(String s){
			System.out.println("["+this.index+"]"+s);
		}
		
		/*
		 public Boolean waitForPageLoaded(WebDriver driver) {
		     ExpectedCondition<Boolean> expectation = new ExpectedCondition<Boolean>() {
		        public Boolean apply(WebDriver driver) {
		          return ((JavascriptExecutor)driver).executeScript("return document.readyState").equals("complete");
		        }
		      };

		     Wait<WebDriver> wait = new WebDriverWait(driver,30);
		      try {
		              wait.until(expectation);
		      } catch(Throwable error) {
		              System.out.println("Timeout waiting for Page Load Request to complete.");
		              return false;
		      }
		      return true;
		 } 
		 */
		
		// does the actual driver creation, calls replaceDriver in case of issues
		public WebDriver newDriver(DesiredCapabilities cap){
			try{
			FirefoxProfile profile = new ProfilesIni().getProfile("default");
			//FirefoxProfile profile = new ProfilesIni().getProfile("selenium");
			//File profileDirectory = new File("/home/sarah/.mozilla/firefox/77rmg3bw.selenium");
			//File profileDirectory = new File("/home/schasins/.mozilla/firefox/ub15j19p.default");
			//FirefoxProfile profile = new FirefoxProfile(profileDirectory);
	        profile.setPreference("network.cookie.cookieBehavior", 2);
	        profile.setPreference("dom.max_script_run_time", this.secondsLimit+50);
	        profile.setPreference("dom.max_chrome_script_run_time", this.secondsLimit+50);
	            
			//WebDriver driver = new FirefoxDriver(cap);
			//WebDriver driver = new FirefoxDriver();
	        WebDriver driver = new FirefoxDriver(new FirefoxBinary(), profile, cap, cap);
			driver.manage().timeouts().implicitlyWait(this.secondsLimit+5, TimeUnit.SECONDS);
			driver.manage().timeouts().pageLoadTimeout(this.secondsLimit+5, TimeUnit.SECONDS);
			driver.manage().timeouts().setScriptTimeout(this.secondsLimit+5, TimeUnit.SECONDS);
			//print("secondsLimit: "+this.secondsLimit);
			return driver;
			}
			catch (WebDriverException exc){
				print("WebDriver exception trying to create new driver.");
				//clearTmpFiles();
				print(new SimpleDateFormat("dd-MM-yyyy-HH-mm").format(new Date()) +" - "+exc.toString().split("\n")[0]);
				return replaceDriver(null,cap);
			}
			
		}
		

		//quits the original driver, checks for defunct processes, calls newDriver to actually create new driver
		public WebDriver replaceDriver(WebDriver driver, DesiredCapabilities cap){
			if (driver != null) {driver.quit();}
			//in case anything goes wrong, let's check for defunct processes
			try {
				String result = execToString("ps -e");
				
				String[] lines = result.split(System.getProperty("line.separator"));
				String lastFirefox = "";
				for (int i = 0; i<lines.length; i++){
					String l = lines[i];
					if (l.contains("firefox") && l.contains("defunct")){
						print("Killing: "+lastFirefox);
						execToString("kill "+lastFirefox);
					}
					else if (l.contains("firefox")){
						lastFirefox = l.trim().split(" ")[0];
					}
				}
			} catch (Exception e1) {
				print("Weren't able to close defunct processes.");
				print(e1.toString().split("\n")[0]);
			}
			return newDriver(cap);
		}
		
		private void loadPage(WebDriver driver, String url) {
			driver.get(url);
			// TODO: modify DOM here.
			Object ans;
			switch(DOMChange) {
			case 1:
				/*System.out.println("MODIFY DOM 1");
				ans = ((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+
						" return getElementByXpath(\"HTML/BODY/DIV[1]/DIV[4]/SPAN[1]/CENTER[1]/DIV[1]/IMG[1]\").tagName;");
				System.out.println(ans.toString());*/
				((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+" return centerFirstLevelDiv();");
				/*ans = ((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+
						" return getElementByXpath(\"HTML/BODY/CENTER[1]/DIV[1]/DIV[4]/SPAN[1]/CENTER[1]/DIV[1]/IMG[1]\").tagName;");
				System.out.println(ans.toString());*/
				break;
			case 2:
				/*System.out.println("MODIFY DOM 2");
				ans = ((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+
						" return getElementByXpath(\"HTML/BODY/DIV[1]/DIV[1]\").id;");
				System.out.println(ans.toString());*/
				((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+" return insertNodes();");
				/*ans = ((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+
						" return getElementByXpath(\"HTML/BODY/DIV[2]/DIV[2]\").id;");
				System.out.println(ans.toString());*/
				break;
			case 3:
				/*System.out.println("MODIFY DOM 3");
				ans = ((JavascriptExecutor) driver).executeScript(
						" return document.body.getElementsByTagName(\"h2\").length;");
				System.out.println(ans.toString());*/
				((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+" return h2Toh3();");
				/*ans = ((JavascriptExecutor) driver).executeScript(
						" return document.body.getElementsByTagName(\"h2\").length;");
				System.out.println(ans.toString());*/
				break;
			case 4:
				((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+" return moveAround();");
				break;
			case 5:
				/*System.out.println("MODIFY DOM 5");
				ans = ((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+
						" return getElementByXpath(\"HTML/BODY/DIV[1]/DIV[1]/DIV[1]/SPAN[1]/SPAN[1]\").textContent;");
				System.out.println(ans.toString());*/
				((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+" return changeText();");
				/*ans = ((JavascriptExecutor) driver).executeScript(DOMModifierFunctions+
						" return getElementByXpath(\"HTML/BODY/DIV[1]/DIV[1]/DIV[1]/SPAN[1]/SPAN[1]\").textContent;");
				System.out.println(ans.toString());*/
				break;
			}
		}
		
		public WebDriver processRow(WebDriver driver, String[] row, DesiredCapabilities cap, int rowId){
			String url = row[0];
			if (!url.startsWith("http")){url = "http://"+url;}

	        //make the argString, since that will be the same across algorithms and subalgorithms
	        for(int j = 0; j < row.length; j++){
	        	String cell = row[j];
	        	cell = cell.replace("\\","\\\\"); //escape slashes
	        	cell = cell.replace("\"","\\\""); //escape double quotes
	            row[j] = "\""+cell+"\"";
	        }
	        //print("Making an argstring with rowId: "+rowId);
			String argString = Joiner.on(",").join(Arrays.copyOfRange(row, 0, row.length))+","+rowId;

	        List<String> ansList = new ArrayList<String>();
	        int failureAlg = -1;
	        boolean driverOk = true;
		    for (int j = 0; j<this.algorithms; j++){
		    	try{
		        	failureAlg = j;
			        //reload for each algorithm
		        	loadPage(driver,url);

					char letter = ((char) ((int) 'a' + j));
			        int algorithmSubalgorithms = this.subalgorithms.get(j);
			        for(int i = 0; i<algorithmSubalgorithms; i++){
				        //load jquery if we need it and if we're on a new page
				        if (this.jquery){
					        String jqueryCode;
					        try{
								jqueryCode = new Scanner(new File("resources/jquery-1.10.2.min.js")).useDelimiter("\\Z").next();
							}
							catch(Exception e){print("Failed to open jquery file."); return driver;}
					        ((JavascriptExecutor) driver).executeScript(jqueryCode);
				        }
				        
				        //System.out.println(this.javaScriptFunction+" return func"+(i+1)+"("+argString+");");
				        Object ans = ((JavascriptExecutor) driver).executeScript(this.javaScriptFunction+" return func_"+letter+(i+1)+"("+argString+");");
						
						
						if(i == (algorithmSubalgorithms-1)){
							//last subalgorithm
							if (this.screenshot){
								File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
								//copy to target location
								String inputRowId = row[1].replace("\"", "");
								inputRowId = inputRowId.replace("\'", "");
								String fname = this.screenshotDir+"/"+inputRowId+"_"+j+".png";
								try {
									FileUtils.copyFile(scrFile, new File(fname));
								} catch (IOException e) {
									System.out.println("Couldn't copy screenshot file: "+fname);
									print(e.toString().split("\n")[0]);
								}
								//row[1] should be an id col to use screenshotting
							}
							
							if (ans != null){
								ArrayList<String> ansRows = new ArrayList<String>(Arrays.asList(ans.toString().split("@#@")));
								for(int k = 0; k<ansRows.size(); k++){
									String ansRow = ansRows.get(k);
									if (ansList.size()>k){
										ansList.set(k, ansList.get(k)+"<,>"+ansRow); // our output already includes this row, so we'll just append to those rows
									}
									else{
										ansList.add(ansRow); // none of the other algorithms have tried to add an nth row, so let's add it now
									}
									//if(this.verbose){System.out.println(ansRow);}
								}
								//System.out.println(ansRows.size());
							}
						}
			        }
		        }
				catch(WebDriverException e){
					driverOk = false;
					ansList.set(0, ansList.get(0)+"<,>##TIMEOUT##"); // add a timeout cell to the first row.  this is not always appropriate
					print("-------------");
					System.out.println("Failure alg: " + failureAlg);
					System.out.println(new SimpleDateFormat("dd-MM-yyyy-HH-mm").format(new Date()));
					System.out.println(url + ": " + e.toString().split("\n")[0]);
					print("-------------");
					/*
					//this.writer.writeNext((url+"<,>"+e.toString().split("\n")[0]).split("<,>"));
					driver.quit();
					driver = newDriver(cap);
					*/
					
					try {
						print("!driverOK on row: "+row.toString());
						print("Replacing driver after !driverOK.");
						driver = replaceDriver(driver,cap);
						//print(driver.toString());
						// Comment out this line for scailability test
						//this.queue.timeout();
					    }
					catch (Exception e1) {
						print("-------------");
						print("exception on row: "+row.toString());
						print(new SimpleDateFormat("dd-MM-yyyy-HH-mm").format(new Date()));
						print(row[0] + ": " + e1.toString().split("\n")[0]);
						print("Replacing driver after exception.");
						print("-------------");
						//this.writer.writeNext((url+"<,>"+e.toString().split("\n")[0]).split("<,>"));
						driver = replaceDriver(driver,cap);
						//print(driver.toString());
						// Comment out this line for scailability test
						//this.queue.timeout();
					}
				}
	        }
	        //put anslist in the writer
	        for(int i = 0; i<ansList.size(); i++){
	        	this.writer.writeNext(ansList.get(i).split("<,>"));
	        }
	        return driver;
		}
		
		public class ProcessRow implements Callable<WebDriver>{
		    private final WebDriver driver;
		    private final String[] row;
		    private final DesiredCapabilities cap;
		    private final int rowIndex;

		    public ProcessRow(WebDriver driver, String[] row, DesiredCapabilities cap, int rowIndex) {
		        this.driver = driver;
		        this.row = row;
		        this.cap = cap;
		        this.rowIndex = rowIndex;
		    }

		    public WebDriver call() throws Exception {
		    	return processRow(driver,row,cap,rowIndex);
		    }
		}
		
	    public void run() {
			String PROXY = "localhost:1234";
			org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
			proxy.setHttpProxy(PROXY).setNoProxy("https:*");
			DesiredCapabilities cap = new DesiredCapabilities();
			cap.setCapability(CapabilityType.PROXY, proxy);
			
			WebDriver driver = newDriver(cap);

			if (driver instanceof JavascriptExecutor) {
				while (true) {
					String[] row = this.queue.pop();
					//System.out.println(Arrays.toString(row));
					if (row == null){
						break; //the queue is empty
					}
					
				   TimeLimiter limiter = new SimpleTimeLimiter();
				   
				   try {
					//print("secondsLimit: "+this.secondsLimit);
					int ind = newRowId();
					driver = limiter.callWithTimeout(new ProcessRow(driver,row,cap,ind), this.secondsLimit, TimeUnit.SECONDS, false);
				    } catch (Exception e) {
						print("-------------");
						print("exception on row: "+row.toString());
						print(new SimpleDateFormat("dd-MM-yyyy-HH-mm").format(new Date()));
						print(row[0] + ": " + e.toString().split("\n")[0]);
						print("Replacing driver after exception.");
						print("-------------");
						//this.writer.writeNext((url+"<,>"+e.toString().split("\n")[0]).split("<,>"));
						driver = replaceDriver(driver,cap);
						//print(driver.toString());
						// Comment out this line for scailability test
						//this.queue.timeout();
					}
				}
			}
			
	        //Close the browser
			print("Quiting driver at end of tasks.");
	        driver.quit();
	    }
		
		public String execToString(String command) throws Exception {
		    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		    CommandLine commandline = CommandLine.parse(command);
		    DefaultExecutor exec = new DefaultExecutor();
		    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		    exec.setStreamHandler(streamHandler);
		    exec.execute(commandline);
		    return(outputStream.toString());
		}
	}

	
	public static void main(String[] args) {
		//String input1 = "resources/tables/input-filtered-30.csv";
		String input1 = "resources/tables/input1-small.csv";
		String javaScript1 = "resources/programs/getXpaths.js";
		String output1 = "resources/tables/pldi-xpaths-2.csv";

		String input2 = "resources/tables/pldi-xpaths-2.csv";
		String javaScript2 = "resources/programs/filter.js";
		String output2 = "resources/tables/pldi-filteredXpaths-2.csv";

		String input3 = "resources/tables/pldi-filteredXpaths-2.csv";
		String javaScript3 = "resources/programs/pldi-nodeSaving.js";
		String output3 = "resources/tables/pldi-savedNodes-2.csv";
		
		String input4 = "resources/tables/pldi-savedNodes-2.csv";
		String javaScript4 = "resources/programs/pldi-nodeRetrieving.js";
		String output4 = "resources/tables/pldi-nodeRetrieval1-SameSession-2.csv";

		String output5 = "resources/tables/pldi-nodeRetrieval2-DiffSessionButTemporallyClose-2.csv";
		
		String output6 = "resources/tables/pldi-nodeRetrieval3-DiffSessionAndTemporallyFar-2.csv";
		
		
		Boolean jquery = false;
		int threads = 8;
		
		JavaScriptTestingParallelWorkStealing system = new JavaScriptTestingParallelWorkStealing();
		
		Boolean firstSession = true;
		
		if (firstSession){
			system.startSession();
			system.stage(input1,javaScript1,output1,jquery,threads,30,false,"");
			system.stage(input2,javaScript2,output2,jquery,threads,30,false,"");
			system.stage(input3,javaScript3,output3,jquery,threads,30,false,"");
			//system.stage(input4,javaScript4,output4,jquery,threads);
			//system.endSession();
			
			//system.startSession();
			system.stage(input4,javaScript4,output5,jquery,threads,30,false,"");
			system.endSession();
		}
		else{
			system.startSession();
			system.stage(input4,javaScript4,output6,jquery,threads,30,false,"");
			system.endSession();
		}
		        

	}

}