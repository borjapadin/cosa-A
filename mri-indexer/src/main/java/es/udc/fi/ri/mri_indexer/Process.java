package es.udc.fi.ri.mri_indexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class Process {
	//No se si el indexin al que te refieres variaas veces aqui te quieres referir al indexFiles que inicializas
	private static String usage = "mri-indexer Index Processer"
			+ " [-indexin INDEXFILE] [-best_idfterms FIELD N] [-tfpos FIELD TERM]\n"
			+ " [-termstfpos1 DOCID FIELD ORD] [-termstfpos2 PATHSGM NEWID FIELD ORD]\n\n";
	
	private static String indexFile = null;
	private static String field = null;
	private static String term = null;
	private static String n = null;
	private static String ord = null;
	private static String docId = null;
	private static String newId = null;
	private static String pathSgm = null;


	/**Comporbamos que las opciones que posibilitamos estén inicializadas
	 * De lo contrario se detalla lo que debería de acompañarlas*/
	private static void validateArgs() {
		if (indexFile == null) {
			System.err.println("At least indexin: " + usage);
			System.exit(1);
		}
		
		if ((field == null)&& (n == null)) {
			System.err.println("Necesary parameters missing in option -best_idterms: " + usage);
			System.exit(1);
		}

		if ((field == null) && (term == null)) {
			System.err.println("\"Necesary parameters missing in option -tfpos: " + usage);
			System.exit(1);
		}
		 
		if ((docId == null) && (field == null) && (ord == null)) {
			System.err.println("Necesary parameters missing in option -termstfpos1: " + usage);
			System.exit(1);
		}

		if ((pathSgm == null) && (newId == null) && (field == null) && (ord == null)) {
			System.err.println("Necesary parameters missing in option -termstfpos2: " + usage);
			System.exit(1);
		}
	}
	
	/**A continuación realizaremos:
	 * -De ser posible leemos el índice
	 * -Determinamos el número de documentos de los que cuenta la colección,
	 *	para ello usaremos numDocs, que devuelve el número de Docs no borrados (ni lógica, ni física)
	 * -Obtenemos los terminos asociados al campo field, los guardamos en un variable,
	 * para después poder iterar sobre los elementos que la conforman*/
	private static ArrayList<TuplaTermIdf> calculateIdfTerms (String indexFile, String field){
		DirectoryReader indexReader = null;
		Directory dir = null;
		ArrayList<TuplaTermIdf> idfTerms = new ArrayList<>();
		
		try{
			//leemos el índice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		}catch(CorruptIndexException e1){
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}catch (IOException e1){
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}
		
		//N: número total de documentos de la colección
		//numDocs devuelve el número de documentos no borrados (ni lógica, ni física)
		int N = indexReader.numDocs();
		
		
		try{
			//obetenemos términos asociados al campo field
			Terms terms = MultiFields.getTerms(indexReader, field);
			
			//termEnum permite iterar sobre los términos de un campo
			final TermsEnum termsEnum = terms.iterator();
			
			while(termsEnum.next() != null) {
				//df_t: número de documentos en los que aparece el término
				int df_t = termsEnum.docFreq();
				
				//idf: mide la especificidad del termino
				double idf = Math.log10(N/df_t);
				
				//asociamos el valor del idf al término
				idfTerms.add(new TuplaTermIdf(termsEnum.term().utf8ToString(),idf));
			}
			indexReader.close();	
		}catch(IOException e){
			System.out.println("Gracefull message: Exception" + e);
			e.printStackTrace();
		}
		return idfTerms;
	}
	
	
	/**Devuelve ordenados por idf, con el número de orden y el valor de idf, 
	 * los n mejores términos del campo field*/
	public static void bestIdfTerms (String indexFile, String field, int n){
		ArrayList<TuplaTermIdf> idfTerms = calculateIdfTerms(indexFile, field);
		
		Collections.sort(idfTerms, new IdfComparator());
		//para devolver los peores: Collections.reverse(idfTerms);
		
		for (int i = 1; i <= idfTerms.size(); i++) {
			TuplaTermIdf tidf = idfTerms.get(i - 1);
			System.out.println("Nº " + i + "	TERM: " + tidf.getTerm() + "	IDF: " + tidf.getIdf());
		}
	}
	
	
	
	/**Construye un listado con la siguiente información:
	 *	docId de Lucene
	 *	PathSmg
	 *	OldId
	 *	NewId del documento donde se encuentra el término
	 *	tf (frecuencia del término en el documento)
	 *	posiciones del término en el documento
	 *	df del término
	 *
	 * - El campo field debe haberse creado con las opciones de indexación de docs, 
	 * - freqs y positions. */
	public static void tfPos (String indexFile, String field, String term) {
		
	}
	
	
	
	/**Construye un listado con la siguiente información: 
	 *	término
	 *	docId de Lucene
	 *	PathSgm
	 *	OldId
	 *	NewId del documento donde se encuentra el término
	 *	tf (frecuencia del término en el documento)
	 *	posiciones del término en el documento
	 *  df del término
	 *  
  	 * - El campo debe haberse creado con las opciones de indexación de docs, freqs y positions.
	 * - El listado vendrá ordenado según el valor de ord: 
	 *  	0 alfabético
	 *  	1 por orden decreciente de tf
	 *  	2 por orden decreciente de df  */
	public static void termsTfPos1 (String indexFile, int docId, String field, int ord) {
		
	}
	
	/**Construye un listado con la siguiente información: 
	 *	término
	 *	docId de Lucene
	 *	PathSgm
	 *	OldId
	 *	NewId del documento donde se encuentra el término
	 *	tf (frecuencia del término en el documento)
	 *	posiciones del término en el documento
	 *  df del término
	 *  
  	 * - El campo debe haberse creado con las opciones de indexación de PathSgm y NewId.
	 * - El listado vendrá ordenado según el valor de ord: 
	 *  	0 alfabético
	 *  	1 por orden decreciente de tf
	 *  	2 por orden decreciente de df  */
	//lo mismo que la opción -termstfpos1, pero para un documento identificado por su PathSgm y NewId
	public static void termsTfPos2 (String indexFile, String pathSgm, int newId, String field, int ord) {
		
	}
	
	
	
	public static void main(final String[] args) throws IOException {
		String option = null; 

		if (args.length != 5)
			System.err.println("Invalid arguments: " + usage);
		
		/** Recorremos los argumentos que se nos han pasado 
		 * Determinamos la opción que se nos ha pasado y en caso de tener información asociada
		 * la obtenemos junto a dicha opción*/
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			
			case "-indexin":
				indexFile = args[i+1];
				i++;
				break;
			case "-best_idfterms":
				option = args[i];
				field = args[i + 1];
				n = args[i + 2];
				i += 2;
				break;
			case "-tfpos":
				option = args[i];
				field = args[i+1];
				term = args[i+2]; ;
				i+=2;
				break;
			case "-termstfpos1":
				option = args[i];
				docId = args[i+1];
				field = args[i+2];
				ord = args[i+3];
				i+=3;	
				break;
			case "-termstfpos2":
				option = args[i];
				pathSgm = args[i+1];
				newId = args[i+2];
				field = args[i+3];
				ord = args[i+4];
				i+=4;	
				break;
			}
		}
		
		validateArgs();
		
		Date start = new Date();
		
		switch (option) {
		case "-best_idfterms":
			bestIdfTerms(indexFile, field, Integer.parseInt(n));
			break;
		case "-tfpos":
			tfPos(indexFile, field, term);
			break;
		case "-termstfpos1":
			termsTfPos1(indexFile, Integer.parseInt(docId), field, Integer.parseInt(ord));
			break;
		case "-termstfpos2":
			termsTfPos2(indexFile, pathSgm, Integer.parseInt(newId), field, Integer.parseInt(ord));
			break;
		default:
			System.err.println("Invalid arguments (" + option + "): " + usage);
			System.exit(1);
			break;
		}
		
		Date end = new Date();
		
		//Imprime el tiempo de procesado
		System.out.println(end.getTime() - start.getTime() + " total milliseconds");	
	}	
}
