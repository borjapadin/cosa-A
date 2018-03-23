package es.udc.fi.ri.mri_indexer;

import java.nio.file.Paths;
import java.util.Date;
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
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;


public class ProcessAndIndex {
	//lo mismo que en process pero ahora pasa con indexinFile
	private static String usage = "mri-indexer Process and Index"
			+ " [-indexin INDEXFILE] [-indexout INDEXFILE] [-deldocsterm TERM] [-deldocsquery QUERY]\n"
			+ " [-summaries] [-multithread N]\n\n";
	
	private static String indexinFile = null;
	private static String indexoutFile = null;
	private static String field = null;
	private static String term = null;
	private static String query = null;
	private static String n = null;
	
	
	/**Comporbamos que las opciones que posibilitamos estén inicializadas
	 * De lo contrario se detalla lo que debería de acompañarlas*/
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
		
		if (n == null) {
			System.err.println("Necesary parameters missing in option -multithread: " + usage);
			System.exit(1);
		}	
	}
	
	/**Obtendremos la driección donde crearemos el índice
	 * y de ser posible se creará*/
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
	
	/**Leemos el índice*/
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
	
	/**Borraremos los documentos que contengan el término especificado*/
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
	
	/**Borraremos los documentos que satisfagan la query, para ello:
	 * -Accederemos a los campos del índice
	 * -Construimos el parser con los datos del campo y analizador y parseamos la query*/
	public static void delDocsQuery(String indexinFolder, String query){
		QueryParser parser;
		Query q = null;
		IndexReader reader = createIndexReader(indexinFolder);
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriter writer = createIndexWriter(indexinFolder);
		//IndexSearcher searcher = new IndexSearcher(reader);
		
		//IDEA: SOLO PARSEAR EL CAMPO BODY//¿porque?
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
	
	
	/**Crea un índice con resúmenes de los documentos
	 * en esta opción es necesario especificar las rutas para -indexin y -indexout
	 * contiene los mismo campos y contenidos y un campo nuevo Resumen 
	 * campo Resumen: contiene las dos frases más similares del campo body con respecto al campo título para cada documento*/
	public static void summaries (String indexinFolder, String indexoutFolder) {
		
	}
	
	
	/**Creación de resúmenes con n hilos*/
	public static void multithread (int n) {
		
	}
	
	
	
	public static void main(String[] args) throws NumberFormatException, ParseException, IOException {
		String option = null;
		
		if (args.length <= 1) {
			System.out.println(usage);
			return;
		}
		
		/** Recorremos los argumentos que se nos han pasado 
		 * Determinamos la opción que se nos ha pasado y en caso de tener información asociada
		 * la obtenemos junto a dicha opción*/
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
			summaries(indexinFile, indexoutFile);
			break;	
		case "-multithread":
			multithread(Integer.parseInt(n));
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
