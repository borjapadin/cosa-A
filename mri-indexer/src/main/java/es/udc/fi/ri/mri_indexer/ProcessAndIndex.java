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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;


public class ProcessAndIndex {

	private static String usage = "mri-indexer Process and Index"
			+ " [-indexin INDEXFILE] [-indexout INDEXFILE] [-deldocsterm TERM] [-deldocsquery QUERY]\n"
			+ " [-summaries] [-multithread N]\n\n";
	
	private static String indexinFile = null;
	private static String indexoutFile = null;
	private static String field = null;
	private static String term = null;
	private static String query = null;
	private static String n = null;
	
	
	/**
	 * En este método, comprobaremos que los parámetros que se nos pasan son correctos en forma,
	 * en caso contrario, mostraremos un mensaje detallando la forma correcta
	 * */
	private static void validateArgs() {
		if (indexinFile == null) {
			System.err.println("At least indexin: " + usage);
			System.exit(1);
		}
		
		if (indexoutFile == null) {
			System.err.println("At least indexout: " + usage);
			System.exit(1);
		}
		

		if ((field == null) && (term == null)) {
			System.err.println("Necesary parameters missing in option deldocsterm: " + usage);
			System.exit(1);
		}

		if (query == null) {
			System.err.println("Necesary parameters missing in option -deldocsquery: " + usage);
			System.exit(1);
		}	
	}
	
	/**
	 * Creamos el writer junto con la dirección de donde se creará
	 * */
	private static IndexWriter createIndexWriter(String indexinFile){	
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE_OR_APPEND);
		
		Directory dir = null;
		IndexWriter writer = null;
		
		try {	
			//obtenemos la dir donde se creará el índice
			dir = FSDirectory.open(Paths.get(indexinFile));
			//creación del índice
			writer = new IndexWriter(dir, config);
			return writer;
			
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
		return null;
	}
	
	/**
	 * Creamos el reader junto con la dirección de donde tiene que leer
	 * */
	private static IndexReader createIndexReader(String indexinFolder){
		Directory dir = null;
		DirectoryReader indexReader = null;
		
		try{
			//leemos el índice en indexin
			dir = FSDirectory.open(Paths.get(indexinFolder));
			indexReader = DirectoryReader.open(dir);
			
			return indexReader;
			
		}catch(CorruptIndexException e) {
			 System.out.println("Error al crear el IndexReader" + e);
			 e.printStackTrace();
		}catch (IOException e){
			 System.out.println("Error al crear el IndexReader" + e);
			 e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Borra los documentos que contienen el término especificado
	 * */
	public static void delDocsTerm(String indexinFolder, String field, String term){
		IndexWriter writer = createIndexWriter(indexinFolder);
		
		try{
			//System.out.println("Deleted term " + term + " in field" + field);
			writer.deleteDocuments(new Term(field, term));
			//writer.forceMergeDeletes();			
		}catch(IOException e){
			e.printStackTrace();
		}
		
		try{
			writer.commit();
			writer.close();
		}catch (CorruptIndexException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}catch (IOException e) {
			System.out.println("Graceful message: exception " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * borra los documentos que satisfacen la query
	 * */
	public static void delDocsQuery(String indexinFolder, String query){
		QueryParser parser;
		Query q = null;
		IndexReader reader = createIndexReader(indexinFolder);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriter writer = createIndexWriter(indexinFolder);
		//IndexSearcher searcher = new IndexSearcher(reader);
		
		//IDEA: SOLO PARSEAR EL CAMPO BODY
		try {
			//accedemos a los campos del índice
			Fields fields = MultiFields.getFields(reader);	
		
			for (final String field : fields) {
				//constructor el parser, con el campo y el analizador
				parser = new QueryParser(field, analyzer);
				q = parser.parse(query); //parseamos la query dada	
				writer.deleteDocuments(q);
			}
			
			//writer.forceMergeDeletes();
			
			writer.commit();
			writer.close();

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
	 * Crea un índice con resúmenes de los documentos
	 * en esta opción es necesario especificar las rutas para -indexin y -indexout
	 * contiene los mismo campos y contenidos y un campo nuevo Resumen 
	 * campo Resumen: contiene las dos frases más similares del campo body con respecto al campo título para cada documento
	 * */
	public static void summaries (String indexinFolder, String indexoutFolder, int n) throws IOException {	
		
		Directory dir1 = null;
		DirectoryReader indexReader = null;

		Directory dir2 = FSDirectory.open(Paths.get(indexoutFolder));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir2, iwc);

		try {
			//comprobamos la ruta especificada en el indexin
			dir1 = FSDirectory.open(Paths.get(indexinFolder));
			indexReader = DirectoryReader.open(dir1);
			
		} catch (CorruptIndexException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Graceful message: exception " + e1);
			e1.printStackTrace();
		}
		
		
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		int numDocs = indexReader.numDocs();
		
		//si está activa la opcion multithread
		if (n > 1) {
			double threads = n;
			double docs = numDocs;
			double docsThread = Math.ceil(docs / threads);
			//creamos un nuevo thread
			final ExecutorService executor = Executors.newFixedThreadPool(n);
			System.out.println("Creating " + n + " threads");
			
			int index = 0;
			for (int i = 0; i < n - 1; i++) {
				final Runnable worker = new ThreadPool2.WorkerThread(writer, indexReader, indexSearcher, index,
						(int) docsThread);
				executor.execute(worker);
				index += docsThread;
			}
			
			final Runnable worker = new ThreadPool2.WorkerThread(writer, indexReader, indexSearcher, index,
					(numDocs - ((int) docsThread) * n - 1));
			
			executor.execute(worker);
			executor.shutdown();

			/*
			 * Wait up to 1 hour to finish all the previously submitted jobs
			 */
			try {
				executor.awaitTermination(1, TimeUnit.HOURS);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				System.exit(-2);
			}
		} else {
			//opción sin concurrencia
			ThreadPool2.process(writer, indexReader, indexSearcher, 0, numDocs);
		}

		System.out.println("Index created successfully");
		writer.close();
		
	}
	
	
	
	public static void main(String[] args) throws NumberFormatException, ParseException, IOException {
		String option = null;
		
		if (args.length <= 1) {
			System.out.println(usage);
			return;
		}
		
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
			case "-summaries":
				option = args[i];
				break;
			case "-multithread":
				option = args[i];
				n = args[i+1];
				i++;
				break;
				
			default:
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
			
		default:
			System.err.println("Invalid arguments (" + option + "): " + usage);
			System.exit(1);
			break;
		}
		
		
		Date end = new Date();
		
		//Imprimime el tiempo de procesado de un índice y a partir de este procesado construcción de un nuevo índice
		System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	}
		
}
