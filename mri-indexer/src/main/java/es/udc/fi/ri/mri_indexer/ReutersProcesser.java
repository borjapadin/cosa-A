package es.udc.fi.ri.mri_indexer;

import java.io.IOException; 
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class ReutersProcesser {

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

	//Comprobación de los argumentos que recibidos.
	//Si hay un error, se muestra un mensaje informado de la forma correcta
	private static void validateArgs() {
		if (indexFile == null) {
			System.err.println("Necesary parameters missing in -indexin: " + usage);
			System.exit(1);
		}
		if ((field == null) && (n == null) && (term == null) && (docId == null) && (ord == null)) {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
	}
	

	/**
	 * Devuelve ordenados por idf, con el número de orden y el valor de idf, los n
	 * mejores términos del campo field
	 */
	public static void bestIdfTerms(String indexFile, String field, int n) throws IOException {
		DirectoryReader indexReader = null;
		Directory dir = null;
		ArrayList<TuplaTermIdf> idfTerms = new ArrayList<>();

		try {
			// obtenemos un indexReader para poder acceder al contenido del indice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}

		//numDocs: número total de documentos de la colección
		//numDocs() devuelve el número de documentos no borrados (ni lógica, ni física)

		int numDocs = indexReader.numDocs(); 
		final Terms terms = MultiFields.getTerms(indexReader, field); //obtenemos los términos asociados al campo field
		final TermsEnum termsEnum = terms.iterator(); //termsEnum permite iterar sobre los términos de un campo

		while (termsEnum.next() != null) {
			int df_t = termsEnum.docFreq(); //df_t: número de documentos en los que aparece el término
			double idf = Math.log10((double)numDocs / (double)df_t); //idf: mide la especificidad del termino
			//double idf = (Math.log(numDocs / df_t))/Math.log(2);
			
			idfTerms.add(new TuplaTermIdf(termsEnum.term().utf8ToString(), df_t, idf));
		}

		indexReader.close();
		
		Collections.sort(idfTerms, new Comparator_Idf());
		//para devolver los peores: 
		Collections.reverse(idfTerms);
		
		for (int i = 1; i <= Math.min(n, idfTerms.size()); i++) {
			TuplaTermIdf term = idfTerms.get(i - 1);
			System.out.println("Nº " + i + "\tTERM: " + term.getTerm() + "\t\tDF: " + term.getDf() + "\t\tIDF: " + term.getIdf());
		}
	}

	
	/**
	 * Construye un listado con la siguiente información: 
	 * 	-docId de Lucene 
	 * 	-PathSmg 
	 * 	-tf (frecuencia del término en el documento) 
	 * 	-posiciones del término en el documento
	 *  -df del término
	 */
	public static void tfPos(String indexFile, String field, String term) throws IOException {
		DirectoryReader indexReader = null;
		Directory dir = null;

		ArrayList<ListTermInfo> termList = new ArrayList<>();
		
		try {
			// Creamos un indexReader para poder acceder al contenido del indice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}

		//obtenemos los términos junto con el iterador para poder recorrer el índice
		final Terms terms = MultiFields.getTerms(indexReader, field); //obtenemos los términos asociados al campo field
		final TermsEnum termsEnum = terms.iterator(); //termsEnum permite iterar sobre los términos de un campo
		
		System.out.println("recorriendo la lista de terminos");
		while (termsEnum.next() != null) {
			String t = termsEnum.term().utf8ToString();
			
			if(t.hashCode() == term.hashCode()) {
				System.out.println("JODER");
				
				int df = termsEnum.docFreq(); //dft: número de documentos en los que aparece el término
				
				PostingsEnum pe1 = null;
				pe1 = termsEnum.postings(pe1, PostingsEnum.ALL);
						
				//recorro la posting del list de cada termino
				while (pe1.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
					LinkedList<Integer> positions = new LinkedList<Integer>();
					
					int docIDpe = pe1.docID();
					Document doc = indexReader.document(docIDpe);
					int docId = pe1.docID();
					String pathSgm = doc.get("PathSgm");
					String oldId = doc.get("OldID");
					String newId = doc.get("NewID");
					int tf = pe1.freq(); //tf: frecuencia del término en el documento
				
				    for (int k = 0; k<tf; k++){
				    	positions.add(pe1.nextPosition());
				    }
				        									
					termList.add(new ListTermInfo(term, docId, pathSgm, tf, positions, df, newId, oldId, 0, "nada"));
				}			
			}
		}
		
		indexReader.close();
	}

	/**
	 * Construye un listado con la siguiente información: 
	 * 	-término
	 * 	-docId de Lucene 
	 *	-PathSgm 
	 *	-tf (frecuencia del término en el documento) 
	 *	-posiciones del término en el documento 
	 *	-df del término
	 * 
	 * El listado vendrá ordenado según el valor de ord
	 */
	public static void termsTfPos1(String indexFile, int docId, String field, int ord) throws IOException {
		DirectoryReader indexReader = null;
		Directory dir = null;

		ArrayList<ListTermInfo> termList = new ArrayList<>();

		try {
			// Creamos un indexReader para poder acceder al contenido del indice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}

		int numDocs = indexReader.numDocs(); 
		//obtenemos los términos junto con el iterador para poder recorrer el índice
		final Terms terms = MultiFields.getTerms(indexReader, field); //obtenemos los términos asociados al campo field
		final TermsEnum termsEnum = terms.iterator(); //termsEnum permite iterar sobre los términos de un campo
		
		
		System.out.println("recorriendo la lista de terminos");
		while (termsEnum.next() != null) {
			String tt = termsEnum.term().utf8ToString();
			double df = termsEnum.docFreq(); //dft: número de documentos en los que aparece el término
		
			PostingsEnum pe1 = MultiFields.getTermPositionsEnum(indexReader, field, termsEnum.term());
			//PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(indexReader, field, termsEnum.term(), PostingsEnum.ALL);
					
			while (pe1.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				int docIDpe = pe1.docID();
				
				if (docIDpe == docId) {
					LinkedList<Integer> positions = new LinkedList<Integer>();
					
					Document doc = indexReader.document(docIDpe);	
					String pathSgm = doc.get("PathSgm");
					String oldId = doc.get("OldID");
					String newId = doc.get("NewID");
					String title = doc.get("TITLE");
					int tf = pe1.freq(); //tf: frecuencia del término en el documento
					
					double idf = Math.log10((double)numDocs / (double)df);
					
					double tfidf = (double)tf * idf;
					for (int k = 0; k<tf; k++){
					    	positions.add(pe1.nextPosition());
					}
					
					termList.add(new ListTermInfo(tt, docId, pathSgm, tf, positions, df, newId, oldId, tfidf, title));
				}		
			}
		}
		
		indexReader.close();
		
		// Ordenamos la lista de términos según los parámetros indicados
		switch (ord) {
		case 0: // alfabético
			Collections.sort(termList, new Comparator_NameTerm());
			break;
		case 1: // por orden decreciente de tf
			Collections.sort(termList, new Comparator_Tf());
			break;
		case 2: // por orden decreciente de df
			Collections.sort(termList, new Comparator_Df());
			break;
		case 3: // por orden decreciente de df
			Collections.sort(termList, new Comparator_TfIdf());
			break;
		}

		System.out.println("Se van a mostrar " + termList.size() + " resultados");
		for (int i = 1; i <= Math.min(5, termList.size()); i++) {
			ListTermInfo tList = termList.get(i);
			System.out.println(
					"Term: " + tList.getTerm() + "\t\tDocID: " + tList.getDocId() + "\tPathSgm: " + tList.getPathSgm() + 
					"\tOldId: " + tList.getOldId() + "\tNewId: " + tList.getNewId() +
					"\tTF: " + tList.getTf() + "\tPos: " + tList.getPositions() + "\tDF: " + tList.getDf()
					+ "\t\tTF*IDF: " + tList.getTfIdf() + "\t\tTITLE: " + tList.getTitle());			
		}
	}

	/**
	 * Lo mismo que la opción -termstfpos pero para un documento identificado por su PathSgm y NewID
	 */
	public static void termsTfPos2(String indexFile, String pathSgm, String newId, String field, int ord) throws IOException {
		DirectoryReader indexReader = null;
		Directory dir = null;

		ArrayList<ListTermInfo> termList = new ArrayList<>();

		try {
			// Creamos un indexReader para poder acceder al contenido del indice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}

		//obtenemos los términos junto con el iterador para poder recorrer el índice
		final Terms terms = MultiFields.getTerms(indexReader, field); //obtenemos los términos asociados al campo field
		final TermsEnum termsEnum = terms.iterator(); //termsEnum permite iterar sobre los términos de un campo
		
		
		System.out.println("recorriendo la lista de terminos");
		while (termsEnum.next() != null) {
			String tt = termsEnum.term().utf8ToString();
			double df = termsEnum.docFreq(); //dft: número de documentos en los que aparece el término
		
			PostingsEnum pe1 = MultiFields.getTermPositionsEnum(indexReader, field, termsEnum.term());
			//PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(indexReader, field, termsEnum.term(), PostingsEnum.ALL);
			
			while (pe1.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				int docIDpe = pe1.docID();
				Document doc = indexReader.document(docIDpe);	
				String PathSgmpe = doc.get("PathSgm");
				String newIdPe = doc.get("NewID");
				
				if((pathSgm.hashCode() == PathSgmpe.hashCode()) && (newId.hashCode() == newIdPe.hashCode())) {
					LinkedList<Integer> positions = new LinkedList<Integer>();
					System.out.println("JODER1");
					String oldId = doc.get("OldID");
					int tf = pe1.freq(); //tf: frecuencia del término en el documento
					
					for (int k = 0; k<tf; k++){
						positions.add(pe1.nextPosition());
					}
					
					termList.add(new ListTermInfo(tt, docIDpe, pathSgm, tf, positions, df, newId, oldId, 0, "nada"));
				}
			}		
		}
		
		indexReader.close();
		
		// Ordenamos la lista de términos según los parámetros indicados
		switch (ord) {
		case 0: // alfabético
			Collections.sort(termList, new Comparator_NameTerm());
			break;
		case 1: // por orden decreciente de tf
			Collections.sort(termList, new Comparator_Tf());
			break;
		case 2: // por orden decreciente de df
			Collections.sort(termList, new Comparator_Df());
			break;
		}

		System.out.println("Se van a mostrar " + termList.size() + " resultados");
		for (int i = 0; i < termList.size(); i++) {
			ListTermInfo tList = termList.get(i);
			System.out.println(
					"Term: " + tList.getTerm() + "	\t\tDocID: " + tList.getDocId() +	"\tPathSgm: " + tList.getPathSgm() + 
					"\tOldId: " + tList.getOldId() + "\tNewId: " + tList.getNewId() +
					"\tTF: " + tList.getTf()  + "\tPos: " + tList.getPositions() + "\t\tDF: " + tList.getDf());
		}
	}
	

	public static void main(final String[] args) throws IOException {
		String option = null;
		
		if (args.length == 0) {
			System.out.println("No hay argumentos " + usage);
			return;
		}
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {

			case "-indexin":
				indexFile = args[i + 1];
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
				field = args[i + 1];
				term = args[i + 2];
				i += 2;
				break;
			case "-termstfpos1":
				option = args[i];
				docId = args[i + 1];
				field = args[i + 2];
				ord = args[i + 3];
				i += 3;
				break;
			case "-termstfpos2":
				option = args[i];
				pathSgm = args[i + 1];
				newId = args[i + 2];
				field = args[i + 3];
				ord = args[i + 4];
				i += 4;
				break;
			}
		}	

		validateArgs();

		Date start = new Date();

		switch(option){
			case "-best_idfterms":
				bestIdfTerms(indexFile, field, Integer.parseInt(n));
				break;
			case "-tfpos":
				tfPos(indexFile, field, term);
				break;
			case "-termstfpos1":
				termsTfPos1(indexFile, Integer.parseInt(docId), field, Integer.parseInt(ord));
				break;
			case"-termstfpos2":	
				termsTfPos2(indexFile, pathSgm, newId, field, Integer.parseInt(ord));
				break;
		}

		Date end = new Date();

		System.out.println("Total processing time : "+(end.getTime()-start.getTime())+" milliseconds");
	}
}
