package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.commons.io.IOUtils;

public class CacmSearcher {

	private static String usage = "mri_searcher Usage: "
			+ " [-search JM LAMBDA | DIR MU] [-indexin PATH] [-cut N]" 
			+ " [-top M] [-queries ALL | INT1 | INT1-INT2]\n\n";
	
	
	private static String indexInPath = null; 
	private static String ir_model = null;
	private static String parameter = null;
	private static String n = null;
	private static String m = null;
	private static String query = null;
	
	private static String queriesPath = "C:\\Users\\Uxia\\Desktop\\RI\\P2\\cacm\\query.text";
	private static String queriesRel= "C:\\Users\\Uxia\\Desktop\\RI\\P2\\cacm\\qrels.text";
	private static int LAST_QUERY = 64;
	private static int firstQueryId = 0;
	
	private static void validateArgs() {
		if ((ir_model == null) && (parameter == null)) {
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
	
	private static List<String> identifyQueries(String queries) throws IOException {
		int opt = 0;
		int int1 = 0, int2 = 0;
		
		if (queries.equals("all")) {
			//todas las queries
			opt = 1;
		} else if (queries.contains("-")) {
			//queries en el rango int1-int2
			opt = 3;
			String[] numbers = queries.split("-");
			int1 = Integer.parseInt(numbers[0]);
			int2 = Integer.parseInt(numbers[1]);
		} else {
			//query int1
			opt = 2;
			int1 = Integer.parseInt(queries);
		}
		return launchQueries(opt, int1, int2);
	}
	
	private static List<String> launchQueries(int opt, int int1, int int2) throws IOException {
		List<String> queries = null;
		
		switch (opt) {
		
		case 1:
			//todas las queries
			queries = QueryParser.searchQueries(1, LAST_QUERY);
			firstQueryId = 1;
			break;
		case 2:
			//query int1
			queries = new ArrayList<>();
			queries.add(QueryParser.searchQuery(int1));
			firstQueryId = int1;
			break;
		case 3:
			//queries en el rango int1-int2
			queries = QueryParser.searchQueries(int1, int2);
			firstQueryId = Math.min(int1, int2);
			break;
		default:
			System.err.println("Error in parameter \"-queries\"");
			System.exit(1);
			break;
		}
		return queries;
	}


	private static void top(int m) {
		System.out.println("Se van a mostrar los top " + m + " de documentos del ranking");
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
				parameter = args[i + 2];
				i +=2 ;
				break;
			case "-indexin":
				indexInPath = args[i + 1];
				i++;
				break;
			case "-cut":				
				n = args[i + 1];
				System.out.println("++ El corte en el ranking para el computo del MAP es de " + n);
				i++;
				break;
			case "-top":
				option = args[i];
				m = args[i + 1];
				System.out.println("++ Se van a mostrar los top " + m + " de documentos del ranking");
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
		case "-top":
			top(Integer.parseInt(m));
			break;
		case "-queries":
			List<String> queryList = null;
			queryList = identifyQueries(query);
			break;
		}

		Date end = new Date();

		System.out.println("Total searching time: " + (end.getTime() - start.getTime()) + " milliseconds");
	}	
}
