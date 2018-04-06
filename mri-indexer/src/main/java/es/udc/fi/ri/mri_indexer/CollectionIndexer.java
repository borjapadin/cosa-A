package es.udc.fi.ri.mri_indexer;

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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CollectionIndexer {

	private static String usage = "mri_indexer Usage: "
			+ " [-openmode OPENMODE] [-index PATH] [-coll PATH] "
			+ " [-multithread] [-addindexes] \n\n";
	
	private static String indexPath = null;
	private static String openMode = null;
	private static String collPath = null;
	
	
	/**
	 * En este método, comprobaremos que los parámetros que se nos pasan son correctos en forma,
	 * en caso contrario, mostraremos un mensaje detallando la forma correcta
	 * */
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
	 * Obtenemos el hostname del equipo, en caso de no ser válido, mostramos una excepción
	 * Esto lo hacemos para comprobar que estamos trabajando en el equipo local
	 * */
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
	 * Determinamos los distintos modos del indexwriter:
	 * Parámetros:
	 * 	IndexWriterConfig config: 
	 * 		Le pasamos la configuración del indexador que se encargará de escribir el índice
	 *  String openmode: puede ser:
	 * 		append: seguiremos escribiendo el índice que se nos indica, debe estar creado
	 * 		create: crearemos un archivo nuevo, en caso de existir uno con el mismo nombre, lo eliminamos
	 * 		create_or_append: si el índice ya existe, seguiremos escribiendo en el, sino, lo creamos
	 * */
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


	/**
	 * función que comenzará con los procesos de indexación, deberemos iniciar un analizador,
	 * obtener la configuración de nuestro indexWriter, obtenemos el openMode, obtenemos el hostName,
	 * probamos a crear o abrir el índice, dependiendo del openMode, definimos nuestro IndexWriter, indexamos 
	 * la dirección que se nos pasa, y nos aseguramos de cerrar el indexWriter
	 * Parámetros:
	 * 	Path docDir:
	 * 		Se pasa la dirección del directorio a indexar
	 * */
	private static void index (Path docDir) {
		Directory dir = null;
		//determinamos el tipo del analizador y la configuración del indexwriter
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		
		//determinamos la opción de openmode que se nos ha pasado
		setOpenMode(config, openMode);
		//obtenemos el hostname
		String hostName = getHostName();
		
		try {
			//creamos el índice
			dir = FSDirectory.open(Paths.get(indexPath));
			IndexWriter writer = new IndexWriter(dir, config);

			//En caso de tener solo un docDir, lo indexamos
			if (docDir != null) {
				System.out.println("Indexing only " + docDir);
				ThreadPool1.indexDocs(writer, docDir, hostName);
			} 
			//cerramos el writer
			writer.close();

		} catch (IOException e) {

		}
	}
	
	
	/**
	 * Se creará un hilo por cada subcarpeta de primer nivel de la carpeta pathname especificada en la opción -coll
	 *  
	 * Cada hilo creará un índice de forma concurrente con todos los archivos *.sgm que cuelga de esa subcarpeta que a su vez puede
	 * 	contener subcarpetas hasta cualquier nivel.
	 *     
	 * El número de hilos vienen dado sólo por el número de subcarpetas de primer nivel y se puede suponer que en la carpeta raíz no hay archivos *.sgm y que
	 *	se usa sólo como contenedera de las subcarpetas que contienen los archivos *.sgm.
	 */
	private static void multithread (Path docDir) throws IOException {
		//abrimos el directorio
		Directory dir = FSDirectory.open(Paths.get(collPath));
		//determinamos el tipo del analizador y la configuración del indexwriter
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		//determinamos la opción de openmode que se nos ha pasado
		setOpenMode(config, openMode);
		//obtenemos el hostname
		String hostname = getHostName();
		//definimos el indexWriter mediante su configuración y la ruta
		IndexWriter writer = new IndexWriter(dir, config);
		//Creamos n-threads, siendo n el número de docDirs
		final int numCores = Runtime.getRuntime().availableProcessors();
		//Determinamos el número máximo de threads que se pueden abrir, siendo este el numero disponible de procesos
		final ExecutorService executor = Executors.newFixedThreadPool(numCores);
		
		//Generamos una lista con las subcarpetas del docDir
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(docDir)) {
			
			//Procesamos cada subcarpeta en un nuevo thread
			for (final Path path : directoryStream) {
				if (Files.isDirectory(path)) {
					//Enviamos el thread al ThreadPool. El cual lo gestionará eventualmente
					final Runnable worker = new ThreadPool1.WorkerThread(writer, path, hostname);
					executor.execute(worker);
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		/*
		 * Cerramos el ThreadPool, no se aceptarán más procesos, pero se finalizarán
		 * todos aquellos que fueran solicitados previamente
		 */
		executor.shutdown();

		//Esperaremos hasta 1 hora a que se terminen los procesos que se solicitaron
		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			System.exit(-2);
		}

		System.out.println("Finished all threads");
		//cerramos el IndexWriter
		writer.close();		
	}

	
	/**
	 * Cada thread generará un índice de cada subcarptea con la identificación de la subcarpeta,
	 * para su posterior unión en un índice general que se encontrará en una subcarpeta
	 * del primer nivel, con un nombre identificativo
	 * 
	 * */
	private static void addindexes(Path docDir) throws IOException {
		
		//Creamos n-threads, siendo n el número de docDirs
		final int numCores = Runtime.getRuntime().availableProcessors();
		//Determinamos el número máximo de threads que se pueden abrir, siendo este el numero disponible de procesos
		final ExecutorService executor = Executors.newFixedThreadPool(numCores);
		//Generamos una lista de writers y directorios que seran los que se encargaran de la indexacion de las subcarpetas
		List<IndexWriter> writerList = new LinkedList<>();
		List <Directory> dirList = new LinkedList<Directory>();
	
		//Generamos una lista con las subcarpetas del docDir
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(docDir)) {

			//Procesamos cada subcarpeta en un nuevo thread
			for (final Path path : directoryStream) {
				//Comprobamos que la ruta es una carpeta
				if (Files.isDirectory(path)) {
					
					//la añadimso a la lista de carpetas
					Directory dir = FSDirectory.open(path);
					dirList.add(dir);
					
					//generamos una configuracion de writer
					Analyzer analyzer = new StandardAnalyzer();
					IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
					//establecemos su openMode a create
					setOpenMode(iwc, "create"); 
					//obtenemos el hostname
					String hostname = getHostName();
					//creamos el writer y lo añadimos a la lista de writers
					IndexWriter writer = new IndexWriter(dir, iwc);
					writerList.add(writer);
					//Enviamos el thread al ThreadPool. El cual lo gestionará eventualmente
					final Runnable worker = new ThreadPool1.WorkerThread(writer, path, hostname);
					executor.execute(worker);
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		

		/*
		 * Cerramos el ThreadPool, no se aceptarán más procesos, pero se finalizarán
		 * todos aquellos que fueran solicitados previamente
		 */
		executor.shutdown();

		//Esperaremos hasta 1 hora a que se terminen los procesos que se solicitaron
		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			System.exit(-2);
		}
		//una vez finalizados todos los threads
		System.out.println("Finished all threads");
		//cerramos todos los writers
		for (IndexWriter writer: writerList)
			writer.close();
		//generamos la subcarpeta donde generaremos el indice conjunto
		Directory dir = FSDirectory.open(Paths.get(indexPath));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		setOpenMode(iwc, openMode);

		IndexWriter writer = new IndexWriter(dir, iwc);
		//generamos el índice adjuntando todos los indices que tenemos guardados en la lista de directorios
		for (Directory directory: dirList)
			writer.addIndexes(directory);
		//cerramos el writer
		writer.close();
			
		System.out.println("-> " + indexPath + " created");
			
	}
		
	
	public static void main (String args[]) throws IOException{
		//List<String> docsPaths = new ArrayList<String>(); //array con los paths donde están los documentos a indexar
		
		String option = null;

		/**
		 * detectamos las opciones de indexación, e indicamos
		 * la posición del argumento del que recogerán la información
		 * */
		for(int i=0; i<args.length; i++) {
			switch (args[i]) {
			case "-openmode":
				openMode = args[i+1]; 
				i++;
				break;	
			case "-index":
				option = args[i];
				indexPath = args[i+1]; 
				i++;
				break;
			case "-coll":
				collPath = args[i+1];
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
		
		//Comprobamos la opción que se nos pasa
		Path docDir = null;
		
		docDir = Paths.get(collPath);
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		
		
		Date start = new Date();
		
		//Aquí va la llamada a los métodos
		switch(option) {
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
		
		//Imprime el tiempo de indexación
		System.out.println("Total indexing time : " + (end.getTime() - start.getTime()) + " milliseconds");	
	}		
}
