package es.udc.fi.ri.mri_indexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class ThreadPool2 {

	/**
	 * Aquí realizamos el summaries realmente
	 * 
	 * Crea un índice con resúmenes de los documentos en esta opción es necesario
	 * especificar las rutas para -indexin y -indexout contiene los mismo campos y
	 * contenidos y un campo nuevo Resumen campo Resumen: contiene las dos frases
	 * más similares del campo body con respecto al campo título para cada documento
	 * 
	 * 
	 */
	public static void process(IndexWriter indexWriterOut, DirectoryReader indexReaderIn, int offset, int docsThread)
			throws IOException {

		String docTitle = null;
		String docBody = null;
		Query queryPhrase1 = null;
		Query queryPhrase2 = null;

		// para que cada thread haga su trabajo
		for (int i = offset; i < (offset + docsThread); i++) {
			// for (int i = 0; i < indexReaderIn.maxDoc(); i++) {
			if (i % 100 == 0)
				System.out.println("THREAD: " + Thread.currentThread().getName() + "\tOFFSET: " + i + "\t"
						+ (offset + docsThread - i) + " documents to go");

			// creación del índice en memoria
			System.out.println("Creación del índice " + i + " en memoria... ");
			Directory directory = new RAMDirectory();
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			IndexWriter indexWriter = new IndexWriter(directory, config);

			// doc1: documentos del indexin
			Document doc = indexReaderIn.document(i);

			// extraigo el BODY y el TITLE
			System.out.println("Extracción del los campos BODY y TTILE");
			docTitle = doc.get("TITLE");
			docBody = doc.get("BODY");
			//System.out.println("El BODY del documento es \n" + docBody);

			String[] phrases = null;
			String phrase = new String();
			String phrase1 = new String();

			// hacer un split con punto para pillar las frases del body
			if (docBody == null) {
				System.out.println("El campo BODY es nulo...");
				// si el body es nulo, el resumen será nulo
				Document doc1 = new Document();
				doc1 = doc;
				doc1.add(new TextField("RESUMEN", null, Field.Store.YES));
				indexWriterOut.addDocument(doc1);
				//indexWriter.commit();
				//indexWriter.close();

				// Si el título es nulo, el resumen contiene las dos primeras frases del body
			} else if (docTitle == null) {
				System.out.println("El campo TITLE es nulo...");
				
				//obtencion de las frases
				String body_replace = docBody.replaceAll("\\. ", "HERO");
				body_replace = body_replace.replaceAll("\\.\n", "HERO"); 
				phrases = body_replace.split("HERO");
				
				System.out.println("Se han obtenido " + phrases.length + " frases");
				phrase = "";
				phrase1 = "";
				// phrase: string que contiene las dos frases
				for (int j = 0; j < Math.min(2, phrases.length); ++j) {
					phrase = phrase + "//" + phrases[j];
				}
				System.out.println(" - El campo RESUMEN contiene las dos primeras frases del body " + phrase);
				Document doc2 = new Document();
				doc2 = doc;
				doc2.add(new TextField("RESUMEN", phrase, Field.Store.YES));
				indexWriterOut.addDocument(doc2);
			} else {
				System.out.println("Los campos BODY y TITLE no son nulos..");				
				
				//así el split de la frases se realiza correctamente
				String body_replace = docBody.replaceAll("\\. ", "HERO");
				body_replace = body_replace.replaceAll("\\.\n", "HERO"); 
				phrases = body_replace.split("HERO");
			
				
				ArrayList<SummariesInfo> summariesList = new ArrayList<>();
				int numDocs = indexReaderIn.numDocs(); 
				final Terms terms = MultiFields.getTerms(indexReaderIn, "TITLE"); //obtenemos los términos asociados al campo field
				final TermsEnum termsEnum = terms.iterator();
								
				System.out.println("recorriendo la lista de terminos");
				while (termsEnum.next() != null) {	
					PostingsEnum pe1 = MultiFields.getTermPositionsEnum(indexReaderIn, "TITLE", termsEnum.term());
					double df = termsEnum.docFreq(); //dft: número de documentos en los que aparece el término
					
					while (pe1.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						int docIDpe = pe1.docID();
						if (docIDpe == i) {
							String tt = termsEnum.term().utf8ToString();
							int tf = pe1.freq(); //tf: frecuencia del término en el documento						
							double idf = Math.log10((double)numDocs / (double)df);		
							double tfidf = (double)tf * idf;
								
							summariesList.add(new SummariesInfo(tt, tf, idf, tfidf));								
						}						
					}						
				}			

				Collections.sort(summariesList, new ComparatorSummaries());
				
				String query = "";
				for (int g = 0; g < Math.min(10, summariesList.size()); g++) {
					SummariesInfo sList = summariesList.get(g);
					System.out.println(
							"Term: " + sList.getTerm() + "\tTF: " + sList.getTf() +  
							"\tIDF: " + sList.getIdf()+ "\t\tTF*IDF: " + sList.getTfIdf());
					//creamos la query con los 3 mejores del titulo
					if (g < 3) {
						query = query + " " + sList.getTerm();
					}
				}	
				System.out.println("La query con los tres mejores términos de título es " + query);
				
				
				System.out.println(
						"Se van a crear " + (phrases.length-1) + " documentos en el indice que esta en memoria: ");
				
				// creamos un doc con cada frase
				for (int j = 0; j < (phrases.length-1); ++j) {
					Document doc3 = new Document();
					System.out.println(" - Creamos el documento " + j);
					doc3.add(new TextField("FRASE", phrases[j], Field.Store.YES));
					System.out.println(" - El campo FRASE tendrá este valor: \n" + phrases[j]);
					indexWriter.addDocument(doc3);
				}
				indexWriter.commit();
				indexWriter.close();

				// creacion de un indexSearcher pra el indice del ram para poder hacer busquedas
				IndexReader indexReader = DirectoryReader.open(directory);
				IndexSearcher indexSearcher = new IndexSearcher(indexReader);

				QueryParser parserPhrase = new QueryParser("FRASE", new StandardAnalyzer());

				// lanzar una query con el título frente al índice formado por las frases del body
				try {
					System.out.println("Parseando la query... ");
					queryPhrase1 = parserPhrase.parse(QueryParser.escape(docTitle));
					System.out.println("queryParser1 "+ queryPhrase1);
					queryPhrase2 = parserPhrase.parse(QueryParser.escape(query));
					System.out.println("queryParser2 "+ queryPhrase2);
				} catch (org.apache.lucene.queryparser.classic.ParseException ignore) {
					phrase = "error";
				}

				System.out.println("Recordemos el valor de TTILE:  " + docTitle);
				// las dos frases más similares del campo body con respecto al campo título para
				// cada documento
				TopDocs topDocs = indexSearcher.search(queryPhrase1, 2);
				TopDocs topDocs1 = indexSearcher.search(queryPhrase2, 3);

				// tDocs: lista con los dos mejores docs
				LinkedList<Document> tDocs = new LinkedList<Document>();
				LinkedList<Document> tDocs1 = new LinkedList<Document>();

				for (int k = 0; k < Math.min(2, topDocs.scoreDocs.length); k++) {
					tDocs.add(indexReader.document(topDocs.scoreDocs[k].doc));
				}

				for (int f = 0; f < Math.min(3, topDocs1.scoreDocs.length); f++) {
					tDocs1.add(indexReader.document(topDocs1.scoreDocs[f].doc));
				}
				
				System.out.println("La lista tDocs tiene " + tDocs.size() + " elementos... bien");
				for (int m = 0; m < tDocs.size(); m++) {
					phrase = phrase + " " + tDocs.get(m).get("FRASE").toString().replaceAll("\\r?\\n", "");
				}
				
				
				for (int m = 0; m < tDocs1.size(); m++) {
					phrase1 = phrase1 + " " + tDocs1.get(m).get("FRASE").toString().replaceAll("\\r?\\n", "");
				}

				System.out.println("Valores de los campos añadidos al índice final \n");
				System.out.println("RESUMEN: \n" + phrase);
				System.out.println("RESUMEN_1: \n" + phrase1);
				System.out.println("RESUMEN_2: \n" + summariesList.toString());
				
				// creamos el indice final con los documentos que satifacen la query
				Document doc4 = new Document();
				doc4 = doc;
				doc4.add(new TextField("RESUMEN", phrase, Field.Store.YES));
				doc4.add(new TextField("RESUMEN_1", phrase1, Field.Store.YES));
				doc4.add(new TextField("RESUMEN_2", summariesList.toString(), Field.Store.YES));
				System.out.println("Añadimos el documento " + i + " al índice final...");
				indexWriterOut.addDocument(doc4);
			}
		}
	}

	/**
	 * This Runnable takes a folder and prints its path.
	 */
	public static class WorkerThread implements Runnable {

		private final DirectoryReader indexReader;
		private final IndexWriter writerOut;
		private final int offset;
		private final int docsThread;

		public WorkerThread(IndexWriter writerOut, DirectoryReader indexReader, int offset, int docsThread) {
			this.writerOut = writerOut;
			this.indexReader = indexReader;
			this.offset = offset;
			this.docsThread = docsThread;
		}

		/**
		 * This is the work that the current thread will do when processed by the pool.
		 * In this case, it will only print some information.
		 */
		@Override
		public void run() {
			System.out.println(String.format("I am the thread '%s'", Thread.currentThread().getName()));
			try {
				// llamamos al process para que haga la funcionalidad de summaries
				process(writerOut, indexReader, offset, docsThread);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
