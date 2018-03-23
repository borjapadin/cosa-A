package es.udc.fi.ri.mri_indexer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;


public class ThreadPool1 {

	public static final FieldType TYPE_BODY = new FieldType();
	/**indexa documentos y frecuencia*/
	static final IndexOptions options = IndexOptions.DOCS_AND_FREQS;
	/**Definimos los campos a indexar*/
	static {
		TYPE_BODY.setIndexOptions(options);
		TYPE_BODY.setTokenized(true);
		TYPE_BODY.setStored(true);
		TYPE_BODY.setStoreTermVectors(true);
		TYPE_BODY.setStoreTermVectorPositions(true);
		TYPE_BODY.freeze();
	}

	/**Añadimos los elementos al índice*/
	private static void addFields(IndexWriter writer, Path path, String hostname) {
		System.out.println(Thread.currentThread().getId() + " - Indexing " + path);
		
		try (InputStream stream = Files.newInputStream(path)) {
			Document doc = new Document();
			String field;

			String str = IOUtils.toString(stream, "UTF-8");
			StringBuffer strBuffer = new StringBuffer(str);
			
			List<List<String>> documents = Reuters21578Parser.parseString(strBuffer);
		
			for (List<String> document : documents) {
				int i = 0;
				field = document.get(i++);
				doc.add(new TextField("TITLE", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new Field("BODY", field, TYPE_BODY));
				field = document.get(i++);
				doc.add(new TextField("TOPICS", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new TextField("DATELINE", field, Field.Store.YES));
				field = document.get(i++);
				doc.add(new StringField("DATE", field, Field.Store.YES));
				doc.add(new StringField("PathSgm", path.toString(), Field.Store.YES));
				doc.add(new StringField("Hostname", hostname, Field.Store.YES));
				doc.add(new StringField("Thread", Thread.currentThread().getName(), Field.Store.YES));
				writer.addDocument(doc);
				doc = new Document();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/** Parsea los documentos que se quieren indexar, 
	 * y los envía addFields para que los añada al índice
	 * Comprobaremos que los que se nos pasan son directorios para tener que recorrerlos
	 * o enviarlos a addFiles*/
	public static void indexDocuments(IndexWriter writer, Path path, String hostName) throws IOException{
		System.out.println("Indexing documents in " + path);
		
		// si se trata de un directorio hay que recorrerlo
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					System.out.println("Checking filename: " + file);
					if (checkFileName(file)) {
						System.out.println("Path: " + file.toString());
						addFields(writer,file, hostName);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else if (checkFileName(path)) {
			addFields(writer, path, hostName);			
		}
	}
	
	/**Determinamos si la ruta que se nos pas a es un documento o un directorio
	 * Pasamos el resultado a indexDocuments para que determine si, en caso de que sea un directorio,
	 * tenga que recorrerlo o no*/
	protected static void indexDocs(final IndexWriter writer, Path path, String hostname) throws IOException {
		System.out.println("indexDocs in " + path);
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					System.out.println("Checking filename: " + file);
					if (checkFileName(file)) {
						System.out.println("Path: " + file.toString());
						indexDocuments(writer,file, hostname);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			if (checkFileName(path)) {
				indexDocuments(writer, path, hostname);
			}
		}
	}
	
	/**Comprobamos el nombre de la ruta que se nos pasa para comprobar si es
	 * uno de los documentos que queremos indexar*/
	public static boolean checkFileName(Path path) {
		final String fileNameBegin;
		final String fileNameEnd;

		if (path.getFileName().toString().length() == 13) {
			fileNameBegin = path.getFileName().toString().substring(0, 6);
			fileNameEnd = path.getFileName().toString().substring(9, 13);
			try {
				Integer.parseInt(path.getFileName().toString().substring(6, 9));
			} catch (NumberFormatException e) {
				return false;
			}
			if (fileNameBegin.equals("reut2-") && (fileNameEnd.equals(".sgm"))) {
				return true;
			} else {
				return false;
			}

		}
		return false;
	}
	
	
	/**Imprimimos la ruta de una carpeta
	 * Este método es ejecutable*/
	public static class WorkerThread implements Runnable {

		private final IndexWriter writer;
		private final Path path;
		private final String hostname;

		public WorkerThread(IndexWriter writer, Path path, String hostname) {
			this.writer = writer;
			this.path = path;
			this.hostname = hostname;
		}
		
		
		/**
		 * Esto es lo que hará el hilo cuando sea procesado por el pool de conexiones
		 * En este caso, mostrará información*/
		@Override
		public void run() {
			System.out.println(String.format("I am the thread '%s'", Thread.currentThread().getName()));
			try {
				indexDocuments(writer, path, hostname);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
}
