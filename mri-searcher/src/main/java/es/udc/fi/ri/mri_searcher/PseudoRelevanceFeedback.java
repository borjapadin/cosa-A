package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.util.Date;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

public class PseudoRelevanceFeedback {

	private static String usage = "mri_searcher Usage: "
			+ " [-prf JM LAMBDA | DIR MU] [-indexin PATH] [-prs S] [-exp T] " 
			+ " [-cut N] [-top M] [-query QUERY]\n\n";
	
	private static String ir_model = null;
	private static String parameter = null;
	private static String indexInPath = null;
	private static String s = null;
	private static String t = null;
	private static String n = null;
	private static String m = null;
	private static String query = null;
	
	
	private static void validateArgs() {
		if ((ir_model == null) && (parameter == null)) {
			System.err.println("ir_model is required: " + usage);
			System.exit(1);
		}
		if (indexInPath == null) {
			System.err.println("Necesary parameters missing in -indexin: " + usage);
			System.exit(1);
		}
		if ((s == null) && (t == null) && (n == null) && (m == null) && (query == null)) {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
	}
	
	
	private static void setSimilarity(IndexWriterConfig config, String ir_model, String parameter) {
		switch (ir_model) {
		case "jm":
			config.setSimilarity(new LMJelinekMercerSimilarity(Float.parseFloat(parameter)));
			break;
		case "dir":
			config.setSimilarity(new LMDirichletSimilarity(Float.parseFloat(parameter)));
			break;
		}
	}
	
	
	//expande la query original con los t mejores terminos encontrados en el PRS
	private static void expand(int t) {
		// TODO Auto-generated method stub
		
	}
	
	
	//muestra el top m de resultados del ranking
	private static void top(int m) {
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
			case "-prf":
				ir_model = args[i + 1];
				parameter = args[i + 2];
				i +=2 ;
				break;
			case "-prs":
				s = args[i + 1];
				i++;
				break;
			case "-exp":
				option = args[i];
				t = args[i + 1];
				i++;
				break;
			case "-cut":
				n = args[i + 1];
				i++;
				break;
			case "-top":
				option = args[i];
				m = args[i + 1];
				i++;
				break;
			case "-query":
				option = args[i];
				query = args[i + 1];
				i++;
				break;
			}
		}

		validateArgs();

		Date start = new Date();

		switch (option) {
			case "-top":
				top(Integer.parseInt(m));
			break;
			case "-exp":
				expand(Integer.parseInt(t));
			break;

		}

		Date end = new Date();

		System.out.println("Total Pseudo-Relevance-Feedback time : " + (end.getTime() - start.getTime()) + " milliseconds");
	}
}
