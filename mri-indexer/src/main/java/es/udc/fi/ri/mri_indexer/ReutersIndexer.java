package es.udc.fi.ri.mri_indexer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class ReutersIndexer {

	private static String usage = "mri_indexer Usage: " + " [-openmode OPENMODE] [-index PATH] [-coll PATH] "
			+ " [-multithread] [-addindexes] \n\n";

	private static String indexPath = null;
	private static String openMode = null;
	private static String collPath = null;

	
	//Comprobación de los argumentos que recibidos.Si hay un error, se muestra un mensaje informado de la forma correcta
	private static void validateArgs() {
		if (openMode == null
				|| (!openMode.equals("create") && !openMode.equals("append") && !openMode.equals("create_or_append"))) {
			System.err.println("openmode " + openMode + " is invalid: " + usage);
			System.exit(1);
		}
		if (collPath == null) {
			System.err.println("collPath is required: " + usage);
			System.exit(1);
		}
		if (indexPath == null) {
			System.err.println("indexPath is required: " + usage);
			System.exit(1);
		}
	}
	

	/**
	 * Obtenemos el hostname del equipo. En caso de no ser válido, mostramos una excepción.
	 * Así se comprueba que estamos trabajando en el equipo local
	 */
	private static String getHostName() {
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

	
	/**
	 * Determinamos los distintos modos del indexwriter
	 * Parámetros:
	 * 		IndexWriterConfig config: Le pasamos la configuración del indexador que creó el índice 
	 * 		String openmode: 
	 * 		- append: seguiremos escribiendo el índice que se nos indica, debe estar creado 
	 * 		- create: crearemos un archivo nuevo, en caso de existir uno con el mismo nombre, lo eliminamos 
	 * 		- create_or_append: si el índice ya existe, seguiremos escribiendo en el, sino, lo creamos
	 */
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

	
	private static void index(Path docDir) {
		Directory dir = null;
		IndexWriter indexWriter = null;
		
		// determinamos el tipo del analizador y la configuración del indexwriter
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		setOpenMode(config, openMode);
		String hostName = getHostName();

		try {
			//obtenemos la dir donde se creará el índice y lo creamos
			dir = FSDirectory.open(Paths.get(indexPath));
			indexWriter = new IndexWriter(dir, config);
		
			System.out.println("Indexing ---> " + docDir);
			ThreadPool1.indexDocs(indexWriter, docDir, hostName); //llamada para la indexación de los docs
			
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
	

	/**
	 * Se creará un hilo por cada subcarpeta de primer nivel de la carpeta pathname especificada en la opción -coll
	 * 
	 * Cada hilo creará un índice de forma concurrente con todos los archivos *.sgm  que cuelga de esa subcarpeta que a su vez puede contener 
	 * 		subcarpetas hasta cualquier nivel.
	 * 
	 * El número de hilos vienen dado sólo por el número de subcarpetas de primer nivel y se puede suponer que en la carpeta raíz no hay 
	 * 		archivos *.sgm y que se usa sólo como contenedera de las subcarpetas que contienen los archivos *.sgm.
	 */
	private static void multithread(Path docDir) throws IOException {
		Directory dir = null;
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		setOpenMode(config, openMode);
		String hostname = getHostName();
		
		//dir: directorio donde se almacena el índice
		dir = FSDirectory.open(Paths.get(indexPath));
		IndexWriter writer = new IndexWriter(dir, config);

		final int numCores = Runtime.getRuntime().availableProcessors();
		final ExecutorService executor = Executors.newFixedThreadPool(numCores);

		//directoryStream: lista con tolas las subcarpetas de -coll
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(docDir)) {
			for (final Path path : directoryStream) { // Procesamos cada subcarpeta en un nuevo thread
				if (Files.isDirectory(path)) {
					final Runnable worker = new ThreadPool1.WorkerThread(writer, path, hostname);
					executor.execute(worker);
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		executor.shutdown();

		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			System.exit(-2);
		}

		System.out.println("Finished all threads");
		writer.close();
	}
	

	/**
	 * Cada thread generará un índice de cada subcarptea con la identificación de la
	 * subcarpeta, para su posterior unión en un índice general que se encontrará en
	 * una subcarpeta del primer nivel, con un nombre identificativo
	 * 
	 */
	private static void addindexes(Path docDir) throws IOException {

		final int numCores = Runtime.getRuntime().availableProcessors();
		final ExecutorService executor = Executors.newFixedThreadPool(numCores);
		
		// Generamos una lista de writers y directorios que seran los que se encargaran de la indexacion de las subcarpetas
		List<IndexWriter> writerList = new LinkedList<>();
		List<Directory> dirList = new LinkedList<Directory>();

		// Generamos una lista con las subcarpetas del docDir
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(docDir)) {
			int i = 1;
			for (final Path path : directoryStream) { // Procesamos cada subcarpeta 
				// Comprobamos que la ruta es una carpeta
				if (Files.isDirectory(path)) {
					
					//se creará un índice en una subcarpeta de primer nivel de la carpeta -index pathname 		
					File baseDirectory = new File(indexPath);
					File subDirectory = new File(baseDirectory, "indiceCreado"+i);
					Path pathIndexCreado = subDirectory.toPath();	
					i++;
					System.out.println("Indice parcial creado en " + pathIndexCreado.toString());
					
					// la añadimos a la lista de carpetas
					Directory dir1 =  FSDirectory.open(pathIndexCreado);
					dirList.add(dir1);

					// generamos una configuracion de writer
					Analyzer analyzer1 = new StandardAnalyzer();
					IndexWriterConfig config1 = new IndexWriterConfig(analyzer1);
				
					setOpenMode(config1, "create");
					String hostname = getHostName();
					
					// creamos el writer y lo añadimos a la lista de writers
					IndexWriter indexWriter1 = new IndexWriter(dir1, config1);
					writerList.add(indexWriter1);
					
					final Runnable worker = new ThreadPool1.WorkerThread(indexWriter1, path, hostname);
					executor.execute(worker);
				}			
			}
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		executor.shutdown();
		
		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			System.exit(-2);
		}
		
		System.out.println("Finished all threads baby");
		
		// cerramos todos los writers
		for (IndexWriter pepe : writerList)
			pepe.close();
		
		//se coloca el índice fusionado en una subcarpeta del mismo nivel que los otros índices y con un nombre apropiado
		File baseDirectory = new File(indexPath);
		File subDirectory = new File(baseDirectory, "indiceFusionado");
		Path pathIndexFusionado = subDirectory.toPath();
		
		System.out.println("Indice fusionado se creado en " + pathIndexFusionado);
		
		// generamos la subcarpeta donde generaremos el indice común
		Directory dir2 = FSDirectory.open(pathIndexFusionado);
		Analyzer analyzer2 = new StandardAnalyzer();
		IndexWriterConfig config2 = new IndexWriterConfig(analyzer2);

		setOpenMode(config2, openMode);

		IndexWriter indexWriter2 = new IndexWriter(dir2, config2);
		//Generamos el índice adjuntando todos los indices que tenemos guardados en la lista de directorios
		for (Directory dir : dirList)
			indexWriter2.addIndexes(dir);
		
		// cerramos el writer donde estará el indice fusionado
		indexWriter2.close();
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
			case "-multithread":
				option = args[i];
				i++;
				break;
			case "-addindexes":
				option = args[i];
				i++;
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
		case "-multithread":
			multithread(docDir);
			break;
		case "-addindexes":
			addindexes(docDir);
			break;
		}

		Date end = new Date();

		System.out.println("Total indexing time : " + (end.getTime() - start.getTime()) + " milliseconds");
	}
}
