package javaScript;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NodeAddressingComparison {

	public static void main(String[] args) {
		
		String input1 = "resources/tables/nodeAddressingComparison/urls-50.csv";
		//String input1 = "resources/tables/nodeAddressingComparison/urls-30.csv";
		//String input1 = "resources/tables/nodeAddressingComparison/urls-1.csv";
		String javaScript1 = "resources/programs/nodeAddressingComparison/getXpaths.js";
		String output1 = "resources/tables/nodeAddressingComparison/stage1-xpaths.csv";

		String javaScript2 = "resources/programs/nodeAddressingComparison/filter.js";
		String output2 = "resources/tables/nodeAddressingComparison/stage2-filteredXpaths.csv";

		String javaScript3 = "resources/programs/nodeAddressingComparison/pldi-nodeSaving.js";
		String output3 = "resources/tables/nodeAddressingComparison/stage3-savedNodes.csv";
		//String output3 = "resources/tables/nodeAddressingComparison/stage3-savedNodes-truncated.csv";
		
		String javaScript4 = "resources/programs/nodeAddressingComparison/pldi-nodeRetrieving.js";
		String output4Start = "resources/tables/nodeAddressingComparison/stage4-nodeRetrieval";
		
		Boolean jquery = true;
		int threads = 32;
		
		JavaScriptTestingParallelWorkStealing system = new JavaScriptTestingParallelWorkStealing();
		
		Boolean firstSession = false; //TODO: change back to false!
		
		File f = new File(output1);
		if(!f.exists()) { 
		    // we haven't yet run the first session.  better run the first session
			System.out.println("Going to run the first session.");
			firstSession = true;
		}
		
		if (firstSession){
			String date = new SimpleDateFormat("dd-MM-yyyy-hh-mm").format(new Date());
			String output4 = output4Start+"_"+date+".csv";
			system.startSession();
			system.stage(input1,javaScript1,output1,false,threads,200,false,"");
			system.stage(output1,javaScript2,output2,false,threads,400,false,"");
			system.stage(output2,javaScript3,output3,true,threads,400,false,"");
			system.stage(output3,javaScript4,output4,true,threads,400,false,"");
			system.endSession();
		}
		else {
			String date = new SimpleDateFormat("dd-MM-yyyy-hh-mm").format(new Date());
			String output42 = output4Start+"_"+date+".csv";
			system.startSession();
			system.stage(output3,javaScript4,output42,true,threads,400,false,"");
			system.endSession();
		}
		System.out.println("NodeAddressingComparison done.");
		System.exit(0);
	}

}
