package es.udc.fi.ri.mri_indexer;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

public class ThreadPool2 {
	
	public static final FieldType TYPE_BODY = new FieldType();
	// indexa documentos y frecuencia
	static final IndexOptions options = IndexOptions.DOCS_AND_FREQS;
	static {
		//definimos las opciones de indexación
		TYPE_BODY.setIndexOptions(options);
		TYPE_BODY.setTokenized(true);
		TYPE_BODY.setStored(true);
		TYPE_BODY.setStoreTermVectors(true);
		TYPE_BODY.setStoreTermVectorPositions(true);
		TYPE_BODY.freeze();
	}
	
	/**
	 * Aquí realizamos el summaries realmente
	 * 
	 * Crea un índice con resúmenes de los documentos
	 * en esta opción es necesario especificar las rutas para -indexin y -indexout
	 * contiene los mismo campos y contenidos y un campo nuevo Resumen 
	 * campo Resumen: contiene las dos frases más similares del campo body con respecto al campo título para cada documento
	 * 
	 * 
	 * */
	public static void process(IndexWriter writer, DirectoryReader indexReader, IndexSearcher indexSearcher, int index,
			int count) throws IOException {
		
		String docTitle = null;
		QueryParser parserTitle = new QueryParser("TITLE", new StandardAnalyzer());
		QueryParser parserBody = new QueryParser("BODY", new StandardAnalyzer());
		
		for (int i = index; i < (index + count); i++) {
			if (i % 100 == 0)
				System.out.println(Thread.currentThread().getName() + "\tindex = " + i + "\t" + (index + count - i) + " to go");
			
			Document doc = indexReader.document(i);
			docTitle = doc.get("TITLE");
			Query queryTitle = null;
			Query queryBody = null;
			
			try {
				//lanzar una query con el título frente al índice formado por las frases del body
				queryTitle = parserTitle.parse(QueryParser.escape(docTitle));
				queryBody = parserBody.parse(QueryParser.escape(docTitle));
			} catch (ParseException e) {
				writer.addDocument(doc);
				continue;
			}
			
			BooleanQuery booleanQuery = new BooleanQuery.Builder()
					.add(queryTitle, BooleanClause.Occur.SHOULD)
					.add(queryBody, BooleanClause.Occur.SHOULD).build();
			
			//las dos frases más similares del campo body con respecto al campo título para cada documento
			TopDocs topDocs = indexSearcher.search(booleanQuery, 2);
			
			for (int k = 0; k < Math.min(2, topDocs.scoreDocs.length); k++) {
				Document tDoc = indexReader.document(topDocs.scoreDocs[k].doc);
				doc.add(new TextField("SimTitle", tDoc.get("TITLE"), Field.Store.YES));
				doc.add(new TextField("SimBody", tDoc.get("BODY"), Field.Store.NO));
			}
			
			doc.removeField("Thread");
			doc.add(new StringField("Thread", Thread.currentThread().getName(), Field.Store.YES));
			writer.addDocument(doc);
		}
	}
	

	
	/**
	 * This Runnable takes a folder and prints its path.
	 */
	public static class WorkerThread implements Runnable {

		private final IndexWriter writer;
		private final DirectoryReader indexReader;
		private final IndexSearcher indexSearcher;
		private final int index;
		private final int count;

		public WorkerThread(IndexWriter writer, DirectoryReader indexReader, IndexSearcher indexSearcher, int index,
				int count) {
			this.writer = writer;
			this.indexReader = indexReader;
			this.indexSearcher = indexSearcher;
			this.index = index;
			this.count = count;
		}

		/**
		 * This is the work that the current thread will do when processed by
		 * the pool. In this case, it will only print some information.
		 */
		@Override
		public void run() {
			System.out.println(String.format("I am the thread '%s'", Thread.currentThread().getName()));
			try {
				//llamamos al process para que haga la funcionalidad de summaries
				process(writer, indexReader, indexSearcher, index, count);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
