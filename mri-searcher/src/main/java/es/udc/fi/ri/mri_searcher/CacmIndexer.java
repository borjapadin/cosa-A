package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;
import java.net.UnknownHostException;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;


public class CacmIndexer {

	private static String usage = "mri_searcher Usage: "
			+ " [-openmode OPENMODE] [-index PATH] [-indexingmodel JM LAMBDA | DIR MU]" + 
			" [-coll PATH]\n\n";

	private static String indexPath = null;
	private static String openMode = null;
	private static String ir_model = null;
	private static String parameter = null;
	private static String collPath = null;

	public static final FieldType TYPE_N = new FieldType();
	static final IndexOptions options = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS;
	
	static {
		TYPE_N.setIndexOptions(options);
		TYPE_N.setTokenized(true);
		TYPE_N.setStored(true);
		TYPE_N.setStoreTermVectors(true);
		TYPE_N.setStoreTermVectorPositions(true);
		TYPE_N.freeze();
	}

	private static void validateArgs() {
		if (openMode == null
				|| (!openMode.equals("create") && !openMode.equals("append") && !openMode.equals("create_or_append"))) {
			System.err.println("openmode " + openMode + " is invalid: " + usage);
			System.exit(1);
		}	
		if (indexPath == null) {
			System.err.println("indexPath is required: " + usage);
			System.exit(1);
		}
		if (collPath == null) {
			System.err.println("collPath is required: " + usage);
			System.exit(1);
		}
		if ((ir_model == null) && (parameter == null)){
			System.err.println("ir_model and parameter are required: " + usage);
			System.exit(1);
		}
		
	}
	
	
	private static String getHostname() {
		String hostname = "Unknown";
		try {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			hostname = addr.getHostName();
		} catch (UnknownHostException ex) {
			System.out.println("Hostname can not be resolved");
		}
		return hostname;
	}

	
	
	private static void setOpenMode(IndexWriterConfig config, String openMode) {
		switch (openMode) {
		case "append":
			config.setOpenMode(OpenMode.APPEND);
			break;
		case "create":
			config.setOpenMode(OpenMode.CREATE);
			break;
		case "create_or_append":
			config.setOpenMode(OpenMode.CREATE_OR_APPEND);
			break;
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
	
	
	public static boolean checkFileName(Path path) {
		final String fileName;

		if (path.getFileName().toString().length() == 8) {
			fileName = path.getFileName().toString().substring(0, 8);

			if (fileName.equals("cacm.all") ) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	

	private static void indexDoc(IndexWriter writer, Path path, String hostname) {
		System.out.println(Thread.currentThread().getId() + " - Indexing " + path);
		
		try (InputStream stream = Files.newInputStream(path)) {
			Document doc = new Document();
			String field;
			String str = IOUtils.toString(stream, "UTF-8");
			StringBuffer strBuffer = new StringBuffer(str);
			List<List<String>> documents = CacmParser.parseString(strBuffer);
			
			for (List<String> document : documents) {
				int i = 0;
				field = document.get(i++);
				doc.add(new TextField("I", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new TextField("T", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new TextField("B", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new TextField("A", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new TextField("N", field, Field.Store.YES));
				
				writer.addDocument(doc);
				doc = new Document();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Parsea los documentos que se quieren indexar, y los envia a indexDoc para que los añada al indice
	 * */
	protected static void indexDocs(final IndexWriter writer, Path path, String hostname) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					System.out.println("Checking filename: " + file);
					if (checkFileName(file)) {
					//if (file.getFileName().toString().equals(fileName)) {
						System.out.println("Path: " + file.toString());
						indexDoc(writer,file, hostname);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			//if (checkFileName(path)) {
			//if  (path.getFileName().toString().equals(fileName)) {
				indexDoc(writer, path, hostname);
			//}
		}
	}
	
	//FUNCION PRINCIPAL
	private static void index(Path docDir) {
		Directory dir = null;
		IndexWriter indexWriter = null;
		
		// determinamos el tipo del analizador y la configuración del indexwriter
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		setSimilarity(config, ir_model, parameter);
		setOpenMode(config, openMode);

		String hostname = getHostname();
		
		try {
			//obtenemos la dir donde se creará el índice y lo creamos
			dir = FSDirectory.open(Paths.get(indexPath));
			indexWriter = new IndexWriter(dir, config);
		
			System.out.println("Indexing document in ---> " + docDir);
			indexDocs(indexWriter, docDir, hostname); //llamada para la indexación de los docs
			
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
		
		try {
			indexWriter.close();
		}catch(CorruptIndexException e) {
			 System.out.println("Error al crear el IndexWriter" + e);
			 e.printStackTrace();
		}catch (IOException e){
			 System.out.println("Error al crear el IndexWriter" + e);
			 e.printStackTrace();
		}
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
			case "-openmode":
				openMode = args[i + 1];
				i++;
				break;
			case "-index":
				option = args[i];
				indexPath = args[i + 1];
				i++;
				break;
			case "-coll":
				collPath = args[i + 1];
				i++;
				break;
			case "-indexingmodel":
				ir_model = args[i + 1];
				parameter = args[i + 2];
				i +=2 ;
				break;			
			}
		}

		validateArgs();

		//Chequeo de la ruta donde se encuentra la colección 
		Path docDir = null;
		docDir = Paths.get(collPath);
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();

		switch (option) {
		case "-index":
			index(docDir);
			break;
		}

		Date end = new Date();

		System.out.println("Total indexing time: " + (end.getTime() - start.getTime()) + " milliseconds");
	}

}
