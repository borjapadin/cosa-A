package es.udc.fi.ri.mri_indexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

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
import org.apache.lucene.util.BytesRef;

public class Process {

	private static String usage = "mri-indexer Index Processer"
			+ " [-indexin INDEXFILE] [-best_idfterms FIELD N] [-tfpos FIELD TERM]\n"
			+ " [-termstfpos1 DOCID FIELD ORD] [-termstfpos2 PATHSGM NEWID FIELD ORD]\n\n";

	private static String indexFile = null;
	private static String field = null;
	private static String term = null;
	private static String n = null;
	private static String ord = null;
	private static String docId = null;
	//private static String newId = null;
	//private static String pathSgm = null;

	private static void validateArgs() {
		if (indexFile == null) {
			System.err.println("At least indexin: " + usage);
			System.exit(1);
		}

		if ((field == null) && (n == null)) {
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

		/*if ((pathSgm == null) && (newId == null) && (field == null) && (ord == null)) {
			System.err.println("Necesary parameters missing in option -termstfpos2: " + usage);
			System.exit(1);
		}*/
	}

	private static ArrayList<TuplaTermIdf> calculateIdfTerms(String indexFile, String field) {
		DirectoryReader indexReader = null;
		Directory dir = null;
		ArrayList<TuplaTermIdf> idfTerms = new ArrayList<>();

		try {
			// leemos el índice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}

		// N: número total de documentos de la colección
		// numDocs devuelve el número de documentos no borrados (ni lógica, ni física)
		int N = indexReader.numDocs();

		try {
			// obetenemos términos asociados al campo field
			Terms terms = MultiFields.getTerms(indexReader, field);

			// termEnum permite iterar sobre los términos de un campo
			final TermsEnum termsEnum = terms.iterator();

			while (termsEnum.next() != null) {
				// df_t: número de documentos en los que aparece el término
				int df_t = termsEnum.docFreq();
				// idf: mide la especificidad del termino
				double idf = Math.log10(N / df_t);

				// asociamos el valor del idf al término
				idfTerms.add(new TuplaTermIdf(termsEnum.term().utf8ToString(), idf));
			}
			
			indexReader.close();
		} catch (IOException e) {
			System.out.println("Gracefull message: Exception" + e);
			e.printStackTrace();
		}
		return idfTerms;
	}
	
	
	// devuelve ordenados por idf, con el número de orden y el valor de idf, los n
	// mejores términos del campo field
	public static void bestIdfTerms(String indexFile, String field, int n) {
		ArrayList<TuplaTermIdf> idfTerms = calculateIdfTerms(indexFile, field);

		Collections.sort(idfTerms, new IdfComparator());
		// para devolver los peores: Collections.reverse(idfTerms);

		for (int i = 1; i <= idfTerms.size(); i++) {
			TuplaTermIdf tidf = idfTerms.get(i - 1);
			System.out.println("Nº " + i + "	TERM: " + tidf.getTerm() + "	IDF: " + tidf.getIdf());
		}
	}

	/*
	 * construye un listado con la siguiente información: docId de Lucene PathSmg
	 * OldId NewId del documento donde se encuentra el término tf (frecuencia del
	 * término en el documento) posiciones del término en el documento df del
	 * término
	 *
	 * ------ El campo field debe haberse creado con las opciones de indexación de
	 * docs, freqs y positions.
	 */
	public static void tfPos (String indexFile, String field, String term) throws IOException {
		DirectoryReader indexReader = null;
		Directory dir = null;
		
		ArrayList<Termino> termList = new ArrayList<>();
		
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
	
	
		final Terms terms = MultiFields.getTerms(indexReader, field);
		final TermsEnum termsEnum = terms.iterator();

		while (termsEnum.next() != null) {
			BytesRef br = termsEnum.term();
			
			final String tt = br.utf8ToString();
			double df = termsEnum.docFreq();
			
			PostingsEnum positions = MultiFields.getTermPositionsEnum(indexReader, field, br);
			
			while (positions.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				int docId = positions.docID();
				Document doc = indexReader.document(docId);	
				String pathSgm = doc.get("PathSgm");
				//String oldId = doc.get("OldId");
				//String newId = doc.get("NewId");		
				int tf = positions.freq();
		
				termList.add(new Termino(tt, docId, pathSgm, tf, positions, df));
			}
		}
		indexReader.close();	
	}

	
	/*
	 * construye un listado con la siguiente información: término docId de Lucene
	 * PathSgm OldId NewId del documento donde se encuentra el término tf
	 * (frecuencia del término en el documento) posiciones del término en el
	 * documento df del término
	 * 
	 * ------ El campo debe haberse creado con las opciones de indexación de docs,
	 * freqs y positions. ------ El listado vendrá ordenado según el valor de ord: 0
	 * alfabético 1 por orden decreciente de tf 2 por orden decreciente de df
	 */
	public static void termsTfPos1(String indexFile, int docId, String field, int ord) throws IOException {
		DirectoryReader indexReader = null;
		Directory dir = null;
		
		ArrayList<Termino> termList = new ArrayList<>();


		try {
			// leemos el índice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}
		
		final Terms terms = MultiFields.getTerms(indexReader, field);
		final TermsEnum termsEnum = terms.iterator();

		while (termsEnum.next() != null) {
			BytesRef br = termsEnum.term();
			
			final String tt = br.utf8ToString();
			double df = termsEnum.docFreq();
			
			PostingsEnum positions = MultiFields.getTermPositionsEnum(indexReader, field, br);
			
			while (positions.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				Document doc = indexReader.document(docId);	
				String pathSgm = doc.get("PathSgm");
				//String oldId = doc.get("OldId");
				//String newId = doc.get("NewId");		
				int tf = positions.freq();
				
				termList.add(new Termino(tt, docId, pathSgm, tf, positions, df));
			}
		}
		indexReader.close();
		
		switch(ord) {
		
		case 0:
			Collections.sort(termList, new TermComparator());
			break;
		case 1:
			Collections.sort(termList, new TfComparator());
			break;
		case 2:
			Collections.sort(termList, new DfComparator());
			break;
		
		}
		
		for (int i = 1; i <= termList.size(); i++) {
			Termino term = termList.get(i - 1);
			System.out.println("DocID" + term.getDocId() + "\tPathSgm: " + term.getPathSgm() + term.getTf() + "\tdf: " + term.getDf());
		}
		
	}

	// lo mismo que la opción -termstfpos1, pero para un documento identificado por
	// su PathSgm y NewId
	/*public static void termsTfPos2(String indexFile, String pathSgm, String newId, String field, int ord) throws IOException {
		DirectoryReader indexReader = null;
		Directory dir = null;
		
		ArrayList<Termino> termList = new ArrayList<>();


		try {
			// leemos el índice
			dir = FSDirectory.open(Paths.get(indexFile));
			indexReader = DirectoryReader.open(dir);
		} catch (CorruptIndexException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Gracefull message: Exception" + e1);
			e1.printStackTrace();
		}
		
		final Terms terms = MultiFields.getTerms(indexReader, field);
		final TermsEnum termsEnum = terms.iterator();

		while (termsEnum.next() != null) {
			BytesRef br = termsEnum.term();
			
			final String tt = br.utf8ToString();
			double df = termsEnum.docFreq();
			
			PostingsEnum positions = MultiFields.getTermPositionsEnum(indexReader, field, br);
			
			while (positions.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				int docId = positions.docID();
				Document doc = indexReader.document(docId);	
				String oldId = doc.get("OldId");	
				int tf = positions.freq();
				
				termList.add(new Termino(tt, docId, pathSgm, tf, positions, df));
			}
		}
		indexReader.close();
		
		switch(ord) {
		
		case 0:
			Collections.sort(termList, new TermComparator());
			break;
		case 1:
			Collections.sort(termList, new TfComparator());
			break;
		case 2:
			Collections.sort(termList, new DfComparator());
			break;
		
		}
		
		for (int i = 1; i <= termList.size(); i++) {
			Termino term = termList.get(i - 1);
			System.out.println("DocID" + term.getDocId() + "\tPathSgm: " + term.getPathSgm()+ 
					"\ttf " + term.getTf() + "\tdf: " + term.getDf());
		}
		
	}*/

	public static void main(final String[] args) throws IOException {
		String option = null;

		if (args.length != 5)
			System.err.println("Invalid arguments: " + usage);

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
				;
				i += 2;
				break;
			case "-termstfpos1":
				option = args[i];
				docId = args[i + 1];
				field = args[i + 2];
				ord = args[i + 3];
				i += 3;
				break;
			/*case "-termstfpos2":
				option = args[i];
				pathSgm = args[i + 1];
				newId = args[i + 2];
				field = args[i + 3];
				ord = args[i + 4];
				i += 4;
				break;*/
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
		/*case "-termstfpos2":
			termsTfPos2(indexFile, pathSgm, newId, field, Integer.parseInt(ord));
			break;*/
		default:
			System.err.println("Invalid arguments (" + option + "): " + usage);
			System.exit(1);
			break;
		}

		Date end = new Date();

		// Imprime el tiempo de procesado
		System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	}
}
