package es.udc.fi.ri.mri_searcher;

import java.io.IOException;  
import java.io.InputStream;
import java.nio.file.Files;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.commons.io.IOUtils;


public class CACMEval {

	private static String usage = "mri_searcher Usage: "
			+ " [-indexin PATH] [-cut N]" 
			+ " [-top M] [-queries ALL | INT1 | INT1-INT2]\n\n";
	
	private static String indexInPath = null; 
	private static String n = null;
	private static String m = null;
	private static String query = null;
	
	// Archivo con los juicios de relevancia
	private static String qrels = "C:\\Users\\Uxia\\Desktop\\RI\\P2\\cacm\\qrels.text";
	
	private static void validateArgs() {
		if (indexInPath == null) {
			System.err.println("Necesary parameters missing in -indexin: " + usage);
			System.exit(1);
		}
		if ((n == null) && (m == null) && (query == null)) {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
	}
	
	
	public static List<Integer> relevantDocs(int num) throws IOException {
		// se abre el archivo con los juicios de relevancia
		InputStream stream = Files.newInputStream(Paths.get(qrels));
		List<Integer> rDocs = new ArrayList<>();
		String str = IOUtils.toString(stream, "UTF-8");
		StringBuffer strBuffer = new StringBuffer(str);
		String text = strBuffer.toString();
		String[] lines = text.split("\n");
		String numStr = Integer.toString(num); // pasamos el queryID a string para compararlo

		int i = 0;
		while (i < lines.length) {
			
			while ((lines[i].startsWith(numStr.concat(" "))) || (lines[i].startsWith("0".concat(numStr).concat(" ")))) {
				// para quitar el queryID 0X por X
				if (lines[i].startsWith("0")) {
					lines[i] = lines[i].substring(1, lines[i].length() - 4);
				}

				String[] numbers = lines[i++].split(" ");
				rDocs.add(Integer.parseInt(numbers[1]));
				//System.out.println("AÑADIR "+numbers[1]);
				if (i >= lines.length)
					return rDocs;
			}
			i++;
		}
		return rDocs;
	}
	
	
	public static boolean isRelevant(List<Integer> relevants, Document doc) {
		int id = Integer.parseInt(doc.getField("I").stringValue().trim());
		return relevants.contains(id);
	}

	
	private static void evaluateQuery(List<String> queriesList, int m, int queryID) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		IndexSearcher indexSearcher = null;
		Query q = null;
		TopDocs topDocs = null;
		List<IndexableField> fields = null;	
		String [] query_parse_fields = {"T" ,"W"};
		
		int docID;
		double countP10 = 0;
		double countRecall10 = 0;
		double countP20 = 0;
		double countRecall20 = 0;
		double countAp = 0;

		// para poder acceder al contenido del indice
		try {
			dir = FSDirectory.open(Paths.get(indexInPath));
			indexReader = DirectoryReader.open(dir);
		}catch (CorruptIndexException e){
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}
		
		// para poder buscar en el contido del indice
		indexSearcher = new IndexSearcher(indexReader);
		
		// Las queries se procesarán con el multifield query parser sobre los campos .T y .W del índice CACM
		MultiFieldQueryParser parser = new MultiFieldQueryParser(query_parse_fields, new StandardAnalyzer());
		
		for (String query : queriesList) { // para cada query
			try {
				q = parser.parse(QueryParser.escape(query)); // parseamos la query
			}catch (ParseException e) {
				e.printStackTrace();
			}
			
			try {
				topDocs = indexSearcher.search(q, m); // obtenemos los m mejores resultados de la consulta
			}catch (IOException e){
				System.out.println("Graceful message: exception " + e);
				e.printStackTrace();
			}
			
			//PseudoRelevanceFeedback.prf(indexReader, topDocs, query);
			
			List<Integer> relevants = relevantDocs(queryID); // obtenemos los documentos relevantes
			
			System.out.println("\n*********************************************************************************");
			System.out.print("\tBUSQUEDA Y EVALUACION DE LAS QUERIES\n"); 
			System.out.println("*********************************************************************************");
			System.out.println("QUERY " + queryID + ": \n" + query + "\n"); // se visualiza la query
			System.out.println("TOP " + Math.min(m, topDocs.scoreDocs.length) + " DE DOCUMENTOS:");
			
			for (int i = 0; i < Math.min(m, topDocs.scoreDocs.length); i++) { // para cada documento 
				docID = topDocs.scoreDocs[i].doc;
				Document tDoc = indexReader.document(docID);
				System.out.print("\n"); 
				if (isRelevant(relevants, tDoc)) {
					System.out.print("[X] "); // una marca que diga si es relevante según los juicios de relevancia
				}
				
				System.out.println("--Document "+ (i+1) + "\tScore = " + topDocs.scoreDocs[i].score); // el score del documento
				
				fields = tDoc.getFields();
				for (IndexableField field: fields) { //se visualizarán todos los campos del índice
					String fieldName = field.name();
					String fieldValue = field.stringValue();
					System.out.println(fieldName + ": " + fieldValue);  
				}
			}
			
			//acumuluacion de los valores para calcular las métricas promediadas
			countP10 += Metrics.pn(topDocs, relevants, indexReader, q, queryID, 10);
			countP20 += Metrics.pn(topDocs, relevants, indexReader, q, queryID, 20);
			countRecall10 += Metrics.recalln(topDocs, relevants, indexReader, q, queryID, 10);
			countRecall20 += Metrics.recalln(topDocs, relevants, indexReader, q, queryID, 20);		
			countAp += Metrics.ap(Integer.parseInt(n), indexReader, indexSearcher, q, queryID++);
			
			System.out.println("\nMETRICAS INDIVIDUALES");
			System.out.println("P@10 = " + countP10 + "\t" + "\tP@20 = " + countP20);
			System.out.println("Recall@10 = " + countRecall10 + "\t" + "\tRecall@20 = " + countRecall20);
			queryID++;
		}
		
		//se muestran las métricas promediadas
		double p10_p = countP10 / (double) queriesList.size();
		double recall10_p = countRecall10 / (double) queriesList.size();
		double p20_p = countP20 / (double) queriesList.size();
		double recall20_p = countRecall20 / (double) queriesList.size();
		double map = countAp / (double) queriesList.size();
		System.out.println("\n=============================================================================");
		System.out.println("METRICAS PROMEDIADAS");
		System.out.println("P@10 = " + p10_p + "\t" + "\tP@20 = " + p20_p);
		System.out.println("Recall@10 = " + recall10_p + "\t" + "\tRecall@20 = " +recall20_p);
		System.out.println("MAP = " + map);
		System.out.println("=============================================================================");
	}

	
	
	public static void main(String args[]) throws IOException {
		String option = null;
		
		if (args.length == 0) {
			System.out.println("No hay argumentos " + usage);
			return;
		}
		
	
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-indexin":
				indexInPath = args[i + 1];
				System.out.println("Opening index in ---> " + indexInPath);
				i++;
				break;
			case "-cut":				
				n = args[i + 1];
				System.out.println("++ El corte en el ranking para el computo del MAP es de " + n);
				i++;
				break;
			case "-top":
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
		case "-queries":
			List<String> queriesList = null;
			queriesList = QuerySearcher.identifyQueries(query);
			evaluateQuery(queriesList, Integer.parseInt(m), QuerySearcher.getFirstQueryID());
			break;
		}

		Date end = new Date();

		System.out.println("\nTotal searching time: " + (end.getTime() - start.getTime()) + " milliseconds");
	}	
}
