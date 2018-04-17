package es.udc.fi.ri.mri_indexer;

import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;


public class ReutersProcesserAndIndexer {

	private static String usage = "mri-indexer Process and Index"
			+ " [-indexin INDEXFILE] [-indexout INDEXFILE] [-deldocsterm FIELD TERM] [-deldocsquery QUERY]\n"
			+ " [-summaries] [-multithread N]\n\n";
	
	private static String indexinFile = null;
	private static String indexoutFile = null;
	private static String field = null;
	private static String term = null;
	private static String query = null;
	private static String n = null;
	
	
	//Comprobación de los argumentos que recibidos.Si hay un error, se muestra un mensaje informado de la forma correcta
	private static void validateArgs() {
		if ((indexinFile == null) && (indexoutFile == null) && (field == null) && (term == null) && (query == null))  {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
	}


	/**
	 *	 Borra los documentos que contienen el término especificado
	 *	 Esta opción modifica el índice especificado en -indexin
	 */
	public static void delDocsTerm(String indexinFolder, String field, String term){
		Directory dir = null;
		IndexWriter indexWriter = null;

		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.APPEND);
		
		try {	
			//obtenemos la dir donde se creará el índice y lo creamos
			dir = FSDirectory.open(Paths.get(indexinFolder));
			indexWriter = new IndexWriter(dir, config);	
		}catch(CorruptIndexException e) {
			 System.out.println("Error al crear el IndexWriter" + e);
			 e.printStackTrace();
		}catch(LockObtainFailedException e) {
			 System.out.println("Error al crear el IndexWriter" + e);
			 e.printStackTrace();
		}catch (IOException e){
			 System.out.println("Error al crear el IndexWriter" + e);
			 e.printStackTrace();
		}
		
		
		try{
			System.out.println("Deleted documents that contain the TERM: " + term + " in FIELD: " + field);
			indexWriter.deleteDocuments(new Term(field, term));
			//writer.forceMergeDeletes();			
		}catch(IOException e){
			e.printStackTrace();
		}
		
		try{
			indexWriter.commit();
			indexWriter.close();
		}catch (CorruptIndexException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 	 Borra los documentos que satisfacen la query
	 *	 Esta opción modifica el índice especificado en -indexin
	 */
	public static void delDocsQuery(String indexinFolder, String query){
		DirectoryReader indexReader = null;
		Directory dir = null;
		IndexWriter indexWriter = null;
		QueryParser parser;
		Query q = null;
		
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.APPEND);
		
		//Creamos el indexReader para poder acceder a la info del indice
		try{
			//obtenemos la dir donde esta el índice y lo creamos
			dir = FSDirectory.open(Paths.get(indexinFolder));
			indexReader = DirectoryReader.open(dir);
			indexWriter = new IndexWriter(dir, config);	
		}catch(CorruptIndexException e) {
			 System.out.println("Error al crear el IndexReader" + e);
			 e.printStackTrace();
		}catch (IOException e){
			 System.out.println("Error al crear el IndexReader" + e);
			 e.printStackTrace();
		}
		
		//IDEA: SOLO PARSEAR EL CAMPO BODY
		try {
			Fields fields = MultiFields.getFields(indexReader); //accedemos a los campos del índice
		
			for (final String field : fields) {
				parser = new QueryParser(field, analyzer); //constructor el parser, con el campo y el analizador
				q = parser.parse(query); //parseamos la query dada	
				indexWriter.deleteDocuments(q);
				//writer.forceMergeDeletes();
			}
			
			System.out.println("Deleted documents that satisfy the QUERY: " + query);
			indexWriter.commit();
			indexWriter.close();

		}catch (CorruptIndexException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();	
		}catch(ParseException e){
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Crea un índice con resúmenes de los documentos. Contiene los mismo campos y contenidos y un campo nuevo Resumen.
	 * 	- campo Resumen: contiene las dos frases más similares del campo body con respecto al campo título para cada documento
	 * @throws IOException 
	 */
	public static void summaries (String indexinFolder, String indexoutFolder, int n) throws IOException {	
		
		Directory dirIn = null;
		Directory dirOut = null;
		
		IndexWriter indexWriterIn = null;
		IndexWriter indexWriterOut = null;
		
		//configuraciones para los writes
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig configOut = new IndexWriterConfig(analyzer);
		IndexWriterConfig configIn = new IndexWriterConfig(analyzer);
		
		DirectoryReader indexReaderIn = null;

		//creacion indexout
		try {
			System.out.println("Creación del índice final en " + indexoutFolder);
			dirOut = FSDirectory.open(Paths.get(indexoutFolder));
			indexWriterOut = new IndexWriter(dirOut, configOut);
		}catch(LockObtainFailedException e1) {
			 System.out.println("Error al crear el IndexWriter" + e1);
			 e1.printStackTrace();
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
		
		
		try {
			System.out.println("Lectura del índice de " + indexinFolder);
			dirIn = FSDirectory.open(Paths.get(indexinFolder));
			indexWriterIn = new IndexWriter(dirIn, configIn);;
			//para acceder al contenido del indice de indexIn
			indexReaderIn = DirectoryReader.open(dirIn);
		}catch(LockObtainFailedException e1) {
			 System.out.println("Error al crear el IndexWriter" + e1);
			 e1.printStackTrace();
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
			
		//int numDocs = indexReaderIn.numDocs();
		int numDocs = indexReaderIn.maxDoc();
		System.out.println("Hay un total de " + numDocs + " documentos" );
		
		if (n > 1) {
			System.out.println("---> Llamada a process con concurrencia ");
			double threads = n;
			double docs = numDocs;
			
			//cada hilo se ocupa de numDocs/n documentos del índice
			double docsThread = Math.ceil(docs / threads);
			System.out.println("Cada hilo se ocupa de " + docsThread + " documentos");
			
			//creamos n threads		
			final ExecutorService executor = Executors.newFixedThreadPool(n);
			System.out.println("Creating " + n + " threads");
			
			int offset = 0;
			
			Runnable worker = null;
			for (int i = 0; i < n - 1; i++) {
				worker =  new ThreadPool2.WorkerThread(indexWriterOut, indexReaderIn, offset, (int) docsThread);
				executor.execute(worker);
				offset += docsThread;
			}
			
			/*final Runnable worker = 
					new ThreadPool2.WorkerThread(indexWriterOut, indexReaderIn, offset, (numDocs - ((int) docsThread) * (n - 1)));*/
			
			executor.execute(worker);
			executor.shutdown();

			try {
				executor.awaitTermination(1, TimeUnit.HOURS);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				System.exit(-2);
			}
		} else {
			//opción sin concurrencia
			System.out.println("---> Llamada a process sin concurrencia ");
			ThreadPool2.process(indexWriterOut, indexReaderIn, 0, numDocs);
			
		}

		System.out.println("Final index created successfully");
		indexWriterOut.commit();
		indexWriterOut.close();
		indexWriterIn.close();
	}
	
	
	
	public static void main(String[] args) throws NumberFormatException, ParseException, IOException {
		String option = null;
		
		if (args.length == 0) {
			System.out.println("No hay argumentos " + usage);
			return;
		}
		
		//detectamos las opciones de indexación y capturamos sus argumentos
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-indexinFile":
				indexinFile = args[i+1];
			    i++;
				break;
			case "-indexoutFile":
				indexoutFile = args[i+1];
				i++;
				break;
			case "-deldocsterm":
				option = args[i];
			    field = args[i+1];
			    term = args[i+2];
			    i+=2;
				break;
			case "-deldocsquery":
				option = args[i];
				query = args[i+1];
				i+=2;
				break;	
			case "-multithread":
				n = args[i+1];
				i++;
				break;	
			case "-summaries":
				option = args[i];
				i++;
				break;
			}	
		}
		
		validateArgs();
		
		Date start = new Date();
		
		switch (option) {
			case "-deldocsterm":
				delDocsTerm(indexinFile, field, term);
				break;
			case "-deldocsquery":
				delDocsQuery(indexinFile, query);
				break;
			case "-summaries":
				summaries(indexinFile, indexoutFile, Integer.parseInt(n));
				break;	
		}
		
		Date end = new Date();
		
		System.out.println("Total processing and indexing time: " + (end.getTime() - start.getTime()) + " milliseconds");
	}	
}
