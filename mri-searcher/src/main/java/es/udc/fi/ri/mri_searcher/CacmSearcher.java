package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.util.Date;

public class CacmSearcher {

	private static String usage = "mri_searcher Usage: "
			+ " [-search JM LAMBDA | DIR MU] [-indexin PATH] [-cut N]" 
			+ " [-top M] [-queries ALL | INT1 | INT1-INT2]\n\n";
	
	
	private static String indexInPath = null; 
	private static String ir_model = null;
	private static String n = null;
	private static String m = null;
	private static String query = null;
	
	
	private static void validateArgs() {
		if (ir_model == null) {
			System.err.println("ir_model is required: " + usage);
			System.exit(1);
		}
		if (indexInPath == null) {
			System.err.println("Necesary parameters missing in -indexin: " + usage);
			System.exit(1);
		}
		if ((n == null) && (m == null) && (query == null)) {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
	}
	
	
	private static void queries() {
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
			case "-search":
				ir_model = args[i + 1];
				i++;
				break;
			case "-indexin":
				indexInPath = args[i + 1];
				i++;
				break;
			case "-cut":
				n = args[i + 1];
				i++;
				break;
			case "-top":
				m = args[i + 1];
				i++;
				break;
			case "-queries":
				option = args[i];
				query = args[i + 1];
				i++;
				break;
			}
		}

		validateArgs();

		Date start = new Date();

		switch (option) {
		case "-queries":
			queries();
			break;
		}

		Date end = new Date();

		System.out.println("Total searching time : " + (end.getTime() - start.getTime()) + " milliseconds");
	}
	
}
