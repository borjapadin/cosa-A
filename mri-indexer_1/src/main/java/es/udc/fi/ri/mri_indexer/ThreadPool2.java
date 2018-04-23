package es.udc.fi.ri.mri_indexer;

import java.io.IOException; 
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
import org.apache.lucene.queryparser.classic.QueryParser;
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
		Query queryPhrase = null;

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
				phrases = docBody.split("\\.");
				System.out.println("Se han obtenido " + phrases.length + " frases");
				phrase = "";
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
				// creamos un doc con cada frase
				
				//docBody.replaceAll("\\. ", "splitunodos");
				//docBody.replaceAll("\\.\n", "splitunodos");
				
				//String[] phrases = docBody.split("splitunodos");
				
				phrases = docBody.split("\\.\n");
			
				System.out.println(
						"Se van a crear " + (phrases.length) + " documentos en el indice que esta en memoria: ");
				
				// phrases.length-1, porque lo ultimo que pilla es Reuter &#3 y no es relevante
				for (int j = 0; j < (phrases.length); ++j) {
					Document doc3 = new Document();
					System.out.println(" - Creamos el documento " + j);
					doc3.add(new TextField("FRASE", phrases[j], Field.Store.YES));
					System.out.println(" - El campo RESUMEN tendrá este valor: \n" + phrases[j]);
					indexWriter.addDocument(doc3);
				}
				indexWriter.commit();
				indexWriter.close();

				// creacion de un indexSearcher pra el indice del ram para poder hacer busquedas
				IndexReader indexReader = DirectoryReader.open(directory);
				IndexSearcher indexSearcher = new IndexSearcher(indexReader);

				QueryParser parserPhrase = new QueryParser("FRASE", new StandardAnalyzer());

				// lanzar una query con el título frente al índice formado por las frases del
				// body
				try {
					System.out.println("Parseando la query... ");
					queryPhrase = parserPhrase.parse(QueryParser.escape(docTitle));
				} catch (org.apache.lucene.queryparser.classic.ParseException ignore) {
					phrase = "error";
				}

				System.out.println("Recordemos el valor de TTILE:  " + docTitle);
				// las dos frases más similares del campo body con respecto al campo título para
				// cada documento
				TopDocs topDocs = indexSearcher.search(queryPhrase, 2);

				// tDocs: lista con los dos mejores docs
				LinkedList<Document> tDocs = new LinkedList<Document>();

				for (int k = 0; k < Math.min(2, topDocs.scoreDocs.length); k++) {
					tDocs.add(indexReader.document(topDocs.scoreDocs[k].doc));
				}

				System.out.println("La lista tDocs tiene " + tDocs.size() + " elementos... bien");
				for (int m = 0; m < tDocs.size(); m++) {
					phrase = phrase + " " + tDocs.get(m).get("FRASE").toString().replaceAll("\\r?\\n", "");
				}

				System.out.println("Esta es la frase que será añadida al índice final: \n" + phrase);
				// creamos el indice final con los documentos que satifacen la query
				Document doc4 = new Document();
				doc4 = doc;
				doc4.add(new TextField("RESUMEN", phrase, Field.Store.YES));
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
