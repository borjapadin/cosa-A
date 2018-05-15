package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class TraningTest {

	private static String usage = "mri_searcher Usage: " + " [-evaljm N INT1-INT2 INT3-INT4] [-indexin PATH]"
			+ " [-evaldir N INT1-INT2 INT3-INT4]\n\n";

	private static String indexInPath = null;
	private static String n = null;
	private static String int1_int2 = null;
	private static String int3_int4 = null;
	private static int top = 10;
	private static String ir_model = null;

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

	public static void setSimilarity(IndexSearcher indexSearcher, String ir_model, float parameter) {
		switch (ir_model) {
		case "jm":
			indexSearcher.setSimilarity(new LMJelinekMercerSimilarity(parameter));
			break;
		case "dir":
			indexSearcher.setSimilarity(new LMDirichletSimilarity(parameter));
			break;
		}
	}

	private static double evalDir(List<String> queriesJm, int queryId, float parameter) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		IndexSearcher indexSearcher = null;
		Query q = null;
		TopDocs topDocs = null;
		List<IndexableField> fields = null;
		String[] query_parse_fields = { "T", "W" };

		int docID;
		double countAp = 0;

		// para poder acceder al contenido del indice
		try {
			dir = FSDirectory.open(Paths.get(indexInPath));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}

		// para poder buscar en el contido del indice
		indexSearcher = new IndexSearcher(indexReader);
		setSimilarity(indexSearcher, ir_model, parameter);
		
		MultiFieldQueryParser parser = new MultiFieldQueryParser(query_parse_fields, new StandardAnalyzer());

		for (String query : queriesJm) { // para cada query
			try {
				q = parser.parse(QueryParser.escape(query)); // parseamos la query
			} catch (ParseException e) {
				e.printStackTrace();
			}

			try {
				topDocs = indexSearcher.search(q, top); // obtenemos los 10 mejores resultados de la consulta
			} catch (IOException e) {
				System.out.println("Graceful message: exception " + e);
				e.printStackTrace();
			}

			List<Integer> relevants = CACMEval.relevantDocs(queryId); // obtenemos los documentos relevantes

			System.out.println("QUERY " + queryId + " Numero de relevantes: " + relevants.size() + "\n" + query + "");
			System.out.println("TOP " + Math.min(top, topDocs.scoreDocs.length) + " DE DOCUMENTOS:");

			for (int i = 0; i < Math.min(top, topDocs.scoreDocs.length); i++) { // para cada documento
				docID = topDocs.scoreDocs[i].doc;
				Document tDoc = indexReader.document(docID);

				if (CACMEval.isRelevant(relevants, tDoc)) {
					System.out.print("[X] "); // una marca que diga si es relevante según los juicios de relevancia
				}

				System.out.println("--Document " + (i + 1) + "\tScore = " + topDocs.scoreDocs[i].score); 
																										
				fields = tDoc.getFields();
				for (IndexableField field : fields) { // se visualizarán todos los campos del índice
					String fieldName = field.name();
					String fieldValue = field.stringValue();
					System.out.println(fieldName + ": " + fieldValue);
				}
			}

			// acumuluacion de los valores para calcular las métricas promediadas
			countAp += Metrics.ap(Integer.parseInt(n), indexReader, indexSearcher, q, queryId);
			System.out.println("\nAP == " + countAp + "\n");
			queryId++;
		}

		// se muestran las métricas promediadas
		double map = countAp / (double) queriesJm.size();
		return map;
	}

	private static double evalJm(List<String> queriesJm, int queryId, float parameter) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		IndexSearcher indexSearcher = null;
		Query q = null;
		TopDocs topDocs = null;
		List<IndexableField> fields = null;
		String[] query_parse_fields = { "T", "W" };

		int docID;
		double countAp = 0;

		// para poder acceder al contenido del indice
		try {
			dir = FSDirectory.open(Paths.get(indexInPath));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}

		// para poder buscar en el contido del indice
		indexSearcher = new IndexSearcher(indexReader);
		setSimilarity(indexSearcher, ir_model, parameter);

		// Las queries se procesarán con el multifield query parser sobre los campos .T
		// y .W del índice CACM
		MultiFieldQueryParser parser = new MultiFieldQueryParser(query_parse_fields, new StandardAnalyzer());

		for (String query : queriesJm) { // para cada query
			try {
				q = parser.parse(QueryParser.escape(query)); // parseamos la query
			} catch (ParseException e) {
				e.printStackTrace();
			}

			try {
				topDocs = indexSearcher.search(q, top); // obtenemos los 10 mejores resultados de la consulta
			} catch (IOException e) {
				System.out.println("Graceful message: exception " + e);
				e.printStackTrace();
			}

			List<Integer> relevants = CACMEval.relevantDocs(queryId); // obtenemos los documentos relevantes

			System.out.println("QUERY " + queryId + " Numero de relevantes: " + relevants.size() + "\n" + query + "");
			System.out.println("TOP " + Math.min(top, topDocs.scoreDocs.length) + " DE DOCUMENTOS:");

			for (int i = 0; i < Math.min(top, topDocs.scoreDocs.length); i++) { // para cada documento
				docID = topDocs.scoreDocs[i].doc;
				Document tDoc = indexReader.document(docID);

				if (CACMEval.isRelevant(relevants, tDoc)) {
					System.out.print("[X] "); // una marca que diga si es relevante según los juicios de relevancia
				}

				System.out.println("--Document " + (i + 1) + "\tScore = " + topDocs.scoreDocs[i].score); 

				fields = tDoc.getFields();
				for (IndexableField field : fields) { // se visualizarán todos los campos del índice
					String fieldName = field.name();
					String fieldValue = field.stringValue();
					System.out.println(fieldName + ": " + fieldValue);
				}
			}

			// acumuluacion de los valores para calcular las métricas promediadas
			countAp += Metrics.ap(Integer.parseInt(n), indexReader, indexSearcher, q, queryId);
			System.out.println("\nAP == " + countAp + "\n");
			queryId++;
		}

		// se muestran las métricas promediadas
		double map = countAp / (double) queriesJm.size();
		return map;
	}

	public static void main(String args[]) throws IOException {
		String option = null;

		if (args.length == 0) {
			System.out.println("No hay argumentos " + usage);
			return;
		}

		// detectamos las opciones de indexación y capturamos sus argumentos
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-indexin":
				indexInPath = args[i + 1];
				i++;
				break;
			case "-evaljm":
				option = args[i];
				n = args[i + 1];
				System.out.println("++ El corte en el ranking para el computo del MAP es de " + n);
				int1_int2 = args[i + 2];
				int3_int4 = args[i + 3];
				System.out.println("++ El rango de queries de test es: " + int3_int4);
				i += 3;
				break;
			case "-evaldir":
				option = args[i];
				n = args[i + 1];
				System.out.println("++ El corte en el ranking para el computo del MAP es de " + n);
				int1_int2 = args[i + 2];
				int3_int4 = args[i + 3];
				System.out.println("++ El rango de queries de test es: " + int3_int4);
				i += 3;
				break;
			}
		}

		validateArgs();

		Date start = new Date();

		switch (option) {
		case "-evaljm":
			ir_model = "jm";
			List<Double> map_list_jm = new ArrayList<Double>();
			List<String> queriesTrainingJm = null;
			List<String> queriesTestJm = null;
			queriesTrainingJm = QuerySearcher.identifyQueries(int1_int2);
			queriesTestJm = QuerySearcher.identifyQueries(int3_int4);

			System.out.println("\n*********************************************************************************");
			System.out.print("\tTRAINING\n");
			System.out.println("*********************************************************************************");

			System.out.println("\n++ El rango de queries de training es: " + int1_int2);
			System.out.println("Indexando con el modelo jm...\n");

			for (float parameter = 0.0f; parameter < 1.1f; parameter += 0.1f) {
				System.out.println("\n//////////// Search con lambda = " + String.format("%.2f", parameter));
				double map_jm = evalJm(queriesTrainingJm, QuerySearcher.getFirstQueryID(), parameter);
				map_list_jm.add(map_jm);
			}

			System.out.println("\nLamda\t\t\tMap");
			float l = 0.0f;
			for (int i = 0; i < map_list_jm.size(); i++) {
				double m = map_list_jm.get(i);
				System.out.println(String.format("%.2f", l) + "\t|\t" + m);
				l += 0.1;
			}

			System.out.println("\n*********************************************************************************");
			System.out.print("\tTEST\n");
			System.out.println("*********************************************************************************");

			float best_parameter_jm = 0.9f;
			double map_test_jm = evalJm(queriesTestJm, QuerySearcher.getFirstQueryID(), best_parameter_jm);
			System.out.println("\nEl MAP obtenido en test es " + map_test_jm);
			break;

		case "-evaldir":
			ir_model = "dir";
			List<Double> map_list_dir = new ArrayList<Double>();
			List<String> queriesTrainingDir = null;
			List<String> queriesTestDir = null;
			queriesTrainingDir = QuerySearcher.identifyQueries(int1_int2);
			queriesTestDir = QuerySearcher.identifyQueries(int3_int4);

			System.out.println("\n*********************************************************************************");
			System.out.print("\tTRAINING\n");
			System.out.println("*********************************************************************************");

			System.out.println("++ El rango de queries de training es: " + int1_int2);
			System.out.println("Indexando con el modelo dir...");

			for (float parameter = 0f; parameter < 5500f; parameter += 500f) {
				System.out.println("\n//////////// Search con mu = " + String.format("%.2f", parameter));
				double map_dir = evalDir(queriesTrainingDir,QuerySearcher.getFirstQueryID(), parameter);
				map_list_dir.add(map_dir);
			}

			System.out.println("\nMu\t\t\tMap");
			float p = 0.0f;
			for (int i = 0; i < map_list_dir.size(); i++) {
				double m = map_list_dir.get(i);
				System.out.println(String.format("%.2f", p) + "\t|\t" + m);
				p += 0.1;
			}

			System.out.println("\n*********************************************************************************");
			System.out.print("\tTEST\n");
			System.out.println("*********************************************************************************");

			float best_parameter_dir = 0.0f;
			double map_test_dir = evalDir(queriesTestDir, QuerySearcher.getFirstQueryID(), best_parameter_dir);
			System.out.println("\nEl MAP obtenido en test es " + map_test_dir);
			break;
		}

		Date end = new Date();

		System.out.println("\nTotal Trainig and Test time : " + (end.getTime() - start.getTime()) + " milliseconds");
	}
}
