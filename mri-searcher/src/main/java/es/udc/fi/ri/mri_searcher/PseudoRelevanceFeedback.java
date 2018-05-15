package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class PseudoRelevanceFeedback {

	private static String usage = "mri_searcher Usage: " + " [-indexin PATH] [-prs S] [-exp T] "
			+ " [-cut N] [-top M] [-query QUERY]\n\n";

	private static String indexInPath = null;
	private static String ir_model = null;
	private static String parameter = null;
	private static String s = null;
	private static String t = null;
	private static String n = null;
	private static String m = null;
	private static String query = null;

	private static String field = null;
	private static String docId = null;
	
	
	private static void validateArgs() {
		if (indexInPath == null) {
			System.err.println("Necesary parameters missing in -indexin: " + usage);
			System.exit(1);
		}
		if ((s == null) && (t == null) && (n == null) && (m == null) && (query == null)) {
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
	
	private static void evaluateQuery(List<String> queriesList, int n, int m, int queryID) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		IndexSearcher indexSearcher = null;
		Query q = null;
		TopDocs topDocs = null;
		List<IndexableField> fields = null;
		String[] query_parse_fields = { "T", "W" };
		
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
		} catch (CorruptIndexException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}

		// para poder buscar en el contido del indice
		indexSearcher = new IndexSearcher(indexReader);
		setSimilarity(indexSearcher, ir_model, Float.parseFloat(parameter));

		// Las queries se procesarán con el multifield query parser sobre los campos .T
		// y .W del índice CACM
		MultiFieldQueryParser parser = new MultiFieldQueryParser(query_parse_fields, new StandardAnalyzer());

		for (String query : queriesList) { // para cada query
			try {
				q = parser.parse(QueryParser.escape(query)); // parseamos la query
			} catch (ParseException e) {
				e.printStackTrace();
			}

			try {
				topDocs = indexSearcher.search(q, m); // obtenemos los m mejores resultados de la consulta
			} catch (IOException e) {
				System.out.println("Graceful message: exception " + e);
				e.printStackTrace();
			}

			List<Integer> relevants = CACMEval.relevantDocs(queryID); // obtenemos los documentos relevantes
			
			System.out.println("\n*********************************************************************************");
			System.out.print("\tBUSQUEDA Y EVALUACION DE LAS QUERIES\n");
			System.out.println("*********************************************************************************");
			System.out.println("QUERY " + queryID + ": \n" + query + "\n"); // se visualiza la query
			System.out.println("TOP " + Math.min(m, topDocs.scoreDocs.length) + " DE DOCUMENTOS:");

			for (int i = 0; i < Math.min(m, topDocs.scoreDocs.length); i++) { // para cada documento
				docID = topDocs.scoreDocs[i].doc;
				Document tDoc = indexReader.document(docID);
				System.out.print("\n");

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
			//acumuluacion de los valores para calcular las métricas promediadas
			countP10 += Metrics.pn(topDocs, relevants, indexReader, queryID, 10);
			countP20 += Metrics.pn(topDocs, relevants, indexReader, queryID, 20);
			countRecall10 += Metrics.recalln(topDocs, relevants, indexReader, queryID, 10);
			countRecall20 += Metrics.recalln(topDocs, relevants, indexReader, queryID, 20);	
			
			System.out.println("ap ranking = " + countAp);
			countAp += Metrics.ap(n, indexReader, indexSearcher, q, queryID);
			System.out.println("ap acumulado = " + countAp);
			
			System.out.println("\nMETRICAS INDIVIDUALES que son iguales que las PROMEDIADAS");
			System.out.println("P@10 = " + countP10 + "\t" + "\tP@20 = " + countP20);
			System.out.println("Recall@10 = " + countRecall10 + "\t" + "\tRecall@20 = " + countRecall20);
			System.out.println("MAP = " + countAp);
			queryID++;
		}	
	}

	public static void prf(List<String> queriesList, int m, int s, int t) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		IndexSearcher indexSearcher = null;
		Query q = null;
		String expand_query = null;
		TopDocs topDocs = null;
		String[] query_parse_fields = { "T", "W" };
		ArrayList<TermInfo> termList = new ArrayList<>();
		List<Integer> relevants = new ArrayList<Integer>();

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

		// Las queries se procesarán con el multifield query parser sobre los campos .T
		// y .W del índice CACM
		MultiFieldQueryParser parser = new MultiFieldQueryParser(query_parse_fields, new StandardAnalyzer());

		for (String query : queriesList) { // para cada query
			try {
				q = parser.parse(QueryParser.escape(query)); // parseamos la query
			} catch (ParseException e) {
				e.printStackTrace();
			}

			try {
				topDocs = indexSearcher.search(q, m); // obtenemos los m mejores resultados de la consulta
			} catch (IOException e) {
				System.out.println("Graceful message: exception " + e);
				e.printStackTrace();
			}
		}

		System.out.println("\n*********************************************************************************");
		System.out.print("\tPSEUDO-RELEVANCE-FEEDBACK\n");
		System.out.println("*********************************************************************************");

		// debe en encontrar de los doc de de s

		System.out.println("El PRS tiene " + Math.min(s, topDocs.scoreDocs.length) + " documentos");
		
		for (int i = 0; i < Math.min(s, topDocs.scoreDocs.length); i++) { // para todos los términos del PRF
			int numDocs = indexReader.numDocs();
			double score = topDocs.scoreDocs[i].score;
			int docId = topDocs.scoreDocs[i].doc;
			relevants.add(docId);
	
			for (int l = 0; l < query_parse_fields.length; l++) {
				final Terms terms = MultiFields.getTerms(indexReader, query_parse_fields[l]);
				final TermsEnum termsEnum = terms.iterator();

				while (termsEnum.next() != null) {
					String tt = termsEnum.term().utf8ToString();
					int df_t = termsEnum.docFreq(); // df_t: número de documentos en los que aparece el término
					double idf = Math.log10((double) numDocs / (double) df_t); // idf: mide la especificidad del termino
					PostingsEnum pe1 = null;
					pe1 = termsEnum.postings(pe1, PostingsEnum.ALL);

					while (pe1.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						int docIdPosting = pe1.docID();
						if (docIdPosting == docId) {
							int tf = pe1.freq(); // tf: frecuencia del término en el documento
							double value = (double) tf * idf * score;

							termList.add(new TermInfo(docId, tt, idf, tf, score, value));
						}
					}
				}			
			}

			Collections.sort(termList, new Comparator_Value());

		}

		expand_query = expand(queriesList, t, termList);
		indexReader.close();
		new_search(relevants, expand_query, m);
	}
	

	// expande la query original con los t mejores terminos encontrados en el PRS
	private static String expand(List<String> queriesList, int t, ArrayList<TermInfo> termListAux) {
		String query = "";
		String query_aux = "";
		System.out.println("** Los " + t + " mejores terminos para la expansion son: \n");
		
		for (int g = 0; g < Math.min(t, termListAux.size()); g++) {
			TermInfo term = termListAux.get(g);
			System.out.println("DocId " + term.getDocId() + "\tTERM: " + term.getTerm() + "\t\tIDF: " + term.getIdf() + "\t\tTF: " + term.getTf()
					+ "\t\tSCORE: " + term.getScore() + "\t\tVALUE: " + term.getValue());

			query_aux = query_aux + " " + term.getTerm(); // creamos la query con los m mejores terminos
			
		}
		
		//query original y mejores términos
		query = queriesList.get(0) + query_aux;
		
		System.out.println("\nLa QUERY EXPANDIDA es: " + query);
		return query;
	}
	
	
	private static void new_search(List<Integer> relevants, String expand_query, int m) throws IOException {
		Directory dir = null;
		DirectoryReader indexReader = null;
		IndexSearcher indexSearcher = null;
		Query q = null;
		TopDocs topDocs = null;
		List<IndexableField> fields = null;
		String[] query_parse_fields = { "T", "W" };
		
		int docID;

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

		indexSearcher = new IndexSearcher(indexReader); // para poder buscar en el contido del indice
		setSimilarity(indexSearcher, ir_model, Float.parseFloat(parameter));

		// Las queries se procesarán sobre los campos .T y .W del índice CACM
		MultiFieldQueryParser parser = new MultiFieldQueryParser(query_parse_fields, new StandardAnalyzer());

		try {
			q = parser.parse(QueryParser.escape(expand_query)); // parseamos la query
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		try {
			topDocs = indexSearcher.search(q, m); // obtenemos los m mejores resultados de la consulta
		} catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}
		

		System.out.println("\n*********************************************************************************");
		System.out.print("\tBUSQUEDA Y EVALUACION DE LAS QUERIES\n");
		System.out.println("*********************************************************************************");

		System.out.println("TOP " + Math.min(m, topDocs.scoreDocs.length) + " DE DOCUMENTOS:");

		for (int i = 0; i < Math.min(m, topDocs.scoreDocs.length); i++) { // para cada documento
			docID = topDocs.scoreDocs[i].doc;
			Document tDoc = indexReader.document(docID);
			System.out.print("\n");
			
			if (CACMEval.isRelevant(relevants, tDoc)) {
				System.out.print("[X] "); // una marca que diga si es relevante según los juicios de relevancia
			}
			
			System.out.println("--Document " + (i + 1) + "\tScore = " + topDocs.scoreDocs[i].score); // el score del
				
			// documento
			fields = tDoc.getFields();
			for (IndexableField field : fields) { // se visualizarán todos los campos del índice
				String fieldName = field.name();
				String fieldValue = field.stringValue();
				System.out.println(fieldName + ": " + fieldValue);
			}	
		}
		
		
	}

	
	
	public static void termsTfPos(String indexFile, int docId, String field) throws IOException {
		DirectoryReader indexReader = null;
		Directory dir = null;
		ArrayList<TermInfo> termList = new ArrayList<>();

		try {
			// Creamos un indexReader para poder acceder al contenido del indice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}

		//obtenemos los términos junto con el iterador para poder recorrer el índice
		final Terms terms = MultiFields.getTerms(indexReader, field); //obtenemos los términos asociados al campo field
		final TermsEnum termsEnum = terms.iterator(); //termsEnum permite iterar sobre los términos de un campo
		int numDocs = indexReader.numDocs();
		
		System.out.println("recorriendo la lista de terminos");
		while (termsEnum.next() != null) {
			String tt = termsEnum.term().utf8ToString();
			double df = termsEnum.docFreq(); //dft: número de documentos en los que aparece el término
			double idf = Math.log10((double) numDocs / (double) df); // idf: mide la especificidad del termino
			PostingsEnum pe1 = MultiFields.getTermPositionsEnum(indexReader, field, termsEnum.term());
			
			while (pe1.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				int docIDpe = pe1.docID();
				if (docIDpe == docId) {		
					int tf = pe1.freq(); //tf: frecuencia del término en el documento
					double value = (double) tf * idf;					
					termList.add(new TermInfo(docIDpe, tt, idf, tf, 0, value));
				}
			}		
		}
		
		indexReader.close();
		Collections.sort(termList, new Comparator_Value());
	
		System.out.println("Se van a mostrar " + termList.size() + " resultados");
		for (int i = 0; i < termList.size(); i++) {
			TermInfo tList = termList.get(i);
			System.out.println(
					"Term: " + tList.getTerm() + "	\t\tDocID: " + tList.getDocId() + 
					"\tTF: " + tList.getTf()  +  "\t\tIDF: " + tList.getIdf() + "\t\tValue: " + tList.getValue());
		}
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
			case "-prf":
				//indica el modelo de RI para la creación del indice y los valores de los parámetros de suavización
				ir_model = args[i + 1];
				parameter = args[i + 2];
				System.out.println("++ El modelo de RI para el PRF es " + ir_model + " con suavizacion " + parameter + "\n");
				i +=2 ;
				break;	
			case "-indexin":
				indexInPath = args[i + 1];
				System.out.println("Opening index in ---> " + indexInPath);
				i++;
				break;
			case "-prs":
				s = args[i + 1];
				System.out.println(
						"++ Pseudo Relevant Set PRS se construye con los primeros " + s + " documentos del ranking");
				i++;
				break;
			case "-exp":
				option = args[i];
				t = args[i + 1];
				System.out.println(
						"++ Expande la query original con los " + t + " mejores términos encontrados en el PRS");
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
			case "-query":
				option = args[i];
				query = args[i + 1];
				System.out.println("++ Se busca y hace PRF sobre la query " + query + " del archivo query.text");
				i++;
				break;			
			case "-termstfpos":
				//-termstfpos 1937 T 0
				option = args[i];
				docId = args[i + 1];
				field = args[i + 2];
				i += 2;
				break;
			}
		}

		validateArgs();

		Date start = new Date();

		switch (option) {
		case "-query":
			List<String> queriesList = null;
			queriesList = QuerySearcher.identifyQueries(query);
			evaluateQuery(queriesList, Integer.parseInt(n), Integer.parseInt(m), QuerySearcher.getFirstQueryID());
			prf(queriesList, Integer.parseInt(m), Integer.parseInt(s), Integer.parseInt(t));
			break;
		case"-termstfpos":	
			termsTfPos(indexInPath, Integer.parseInt(docId), field);
			break;
		}

		Date end = new Date();

		System.out.println(
				"\nTotal Pseudo-Relevance-Feedback time : " + (end.getTime() - start.getTime()) + " milliseconds");
	}
}
