package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.util.Date;

public class TraningTest {
	
	private static String usage = "mri_searcher Usage: "
			+ " [-evaljm N INT1-INT2 INT3-INT4] [-indexin PATH]"
			+ " [-evaldir N INT1-INT2 INT3-INT4]\n\n";
	
	private static String indexInPath = null;
	private static String n = null;
	private static String int1_int2 = null;
	private static String int3_int4 = null;
	
	private static void validateArgs() {
		if (indexInPath == null) {
			System.err.println("Necesary parameters missing in -indexin: " + usage);
			System.exit(1);
		}
		if ((n == null) && (int1_int2 == null) && (int3_int4 == null)) {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
	}
	
	
	private static void evaldir() {
		// TODO Auto-generated method stub
	}


	private static void evaljm() {
		// TODO Auto-generated method stub		
	}


	public static void main(String args[]) throws IOException {
		String option = null;
		
		if (args.length == 0) {
			System.out.println("No hay argumentos " + usage);
			return;
		}
		
		//detectamos las opciones de indexación y capturamos sus argumentos
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-indexin":
				indexInPath = args[i + 1];
				i++;
				break;
			case "-evaljm":
				option = args[i];
				n = args[i + 1];
				int1_int2 = args[i + 2]; 
				int3_int4 = args[i + 3];
				i += 3;
				break;
			case "-evaldir":
				option = args[i];
				n = args[i + 1];
				int1_int2 = args[i + 2]; 
				int3_int4 = args[i + 3];
				i += 3;
				break;
			}
		}

		validateArgs();

		Date start = new Date();

		switch (option) {
		case "-evaljm":
			evaljm();
			break;
		case "-evaldir":
			evaldir();
			break;
		}

		Date end = new Date();

		System.out.println("Total Trainig and Test time : " + (end.getTime() - start.getTime()) + " milliseconds");
	}
}
