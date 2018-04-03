package es.udc.fi.ri.mri_indexer;

import ThreadPool1;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Date;

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

	
	private static void setOpenMode(IndexWriterConfig config, String openMode) {
		switch (openMode) {
		case "append":
			config.setOpenMode(OpenMode.APPEND);
			break;
		case "create":
			// Create a new index in the directory, removing any
			// previously indexed documents:
			config.setOpenMode(OpenMode.CREATE);
			break;
		case "create_or_append":
			config.setOpenMode(OpenMode.CREATE_OR_APPEND);
			break;
		}
	}


	
	private static void index (Path docDir, List<Path> docDirList) {
		Directory dir = null;
		
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		
	
		setOpenMode(config, openMode);
		
		String hostName = getHostName();
		
		try {
			//creamos el índice
			dir = FSDirectory.open(Paths.get(indexPath));
			IndexWriter writer = new IndexWriter(dir, config);

			// if we have only one docDir, we index it
			if (docDir != null) {
				System.out.println("Indexing only " + docDir);
				ThreadPool1.indexDocuments(writer, docDir, hostName);
			} else {
				// else, we index all of them
				for (Path docPath : docDirList) {
					ThreadPool1.indexDocuments(writer, docPath, hostName);
				}
			}
			writer.close();

		} catch (IOException e) {

		}
	}
	
	
	/*
	 *  Se creará un hilo por cada subcarpeta de primer nivel de la carpeta pathname especificada en la opción -coll
	 *  
	 *  Cada hilo creará un índice de forma concurrente con todos los archivos *.sgm que cuelga de esa subcarpeta que a su vez puede
	 *    	contener subcarpetas hasta cualquier nivel.
	 *     
	 *  El número de hilos vienen dado sólo por el número de subcarpetas de primer nivel y se puede suponer que en la carpeta raíz no hay archivos *.sgm y que
	 *		se usa sólo como contenedera de las subcarpetas que contienen los archivos *.sgm.
	 */
	private static void multithread (Path docDir, List<Path> docDirList) {
		Directory dir = FSDirectory.open(Paths.get(indexes2Path));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		setOpenMode(config, openMode);

		String hostname = getHostName();

		IndexWriter writer = new IndexWriter(dir, config);

		// create n-threads, with n being the number of docDirs
		final ExecutorService executor = Executors.newFixedThreadPool(docDirList.size());
		for (Path docPath : docDirList) {
			final Runnable worker = new ThreadPool1.WorkerThread(writer, docPath, hostname);
			executor.execute(worker);
		}

		/*
		 * Close the ThreadPool; no more jobs will be accepted, but all the
		 * previously submitted jobs will be processed.
		 */
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

		System.out.println("Finished all threads");

		writer.close();		
	}

	
	public static void main (String args[]){
		//List<String> docsPaths = new ArrayList<String>(); //array con los paths donde están los documentos a indexar
		
		String option = null;
				
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
				i++;
				break;
			case "-addindexes":
				i++;
				break;
			}		
		}
		
		validateArgs();
		
		// Now we have to see which option was provided
		// if we have collPath:
		Path docDir = null;
		List<Path> docDirList = null;
		
		docDir = Paths.get(collPath);
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		
		
		Date start = new Date();
		
		//Aquí iría la llamada a los métodos
		switch(option) {
		case "-index":
			index(docDir, docDirList);
			break;
		}
			
	
		
		Date end = new Date();
		
		//Imprime el tiempo de indexación
		System.out.println("Total indexing time : " + (end.getTime() - start.getTime()) + " milliseconds");	
	}		
}
