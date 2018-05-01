package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.TopDocs;


public class PseudoRelevanceFeedback { 

	private static String usage = "mri_searcher Usage: "
			+ " [-indexin PATH] [-prs S] [-exp T] " 
			+ " [-cut N] [-top M] [-query QUERY]\n\n";
	
	private static String indexInPath = null; 
	private static String s = null;
	private static String t = null;
	private static String n = null;
	private static String m = null; 
	private static String query = null;
	
	
	private static void validateArgs() {
		if (indexInPath == null) {
			System.err.println("Necesary parameters missing in -indexin: " + usage);
			System.exit(1);
		}
		if ((s == null) && (t == null) && (n == null) && (m == null) && (query == null)) {
			System.err.println("Necesary parameters missing: " + usage);
			System.exit(1);
		}
	}
	

	public static void prf(IndexReader indexReader, TopDocs topDocList, String query) throws IOException {
		System.out.println("\n*********************************************************************************");
		System.out.print("\tPSEUDO-RELEVANCE-FEEDBACK\n"); 
		System.out.println("*********************************************************************************");
		ArrayList<TermInfo> termList = new ArrayList<>();
		
		for (int i = 0; i < Math.min(Integer.parseInt(s), topDocList.scoreDocs.length); i++) { // para todos los términos del PRF			
			int numDocs = indexReader.numDocs(); 
			double score = topDocList.scoreDocs[i].score;
			final Terms terms = MultiFields.getTerms(indexReader, ".W");
			final TermsEnum termsEnum = terms.iterator(); 
			
			System.out.println("El PRS tiene " + Math.min(Integer.parseInt(s), topDocList.scoreDocs.length) + " documentos y " +
					terms + " terminos");
			
			while (termsEnum.next() != null) {
				String tt = termsEnum.term().utf8ToString();
				int df_t = termsEnum.docFreq(); //df_t: número de documentos en los que aparece el término
				double idf = Math.log10((double)numDocs / (double)df_t); //idf: mide la especificidad del termino
				
				PostingsEnum pe1 = null;
				pe1 = termsEnum.postings(pe1, PostingsEnum.ALL);
				
				while (pe1.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
					int tf = pe1.freq(); //tf: frecuencia del término en el documento
					double value = tf * idf * score;
					termList.add(new TermInfo(tt, idf, tf, value));
				}
			}		
			indexReader.close();
			
			System.out.println("** Los " + t + " mejores terminos para la expansion son: \n");
			for(int k = 0; k < Math.min(Integer.parseInt(t), termList.size()); k++){	
				System.out.println();	
			}
		}	
	}
	
	
	//expande la query original con los t mejores terminos encontrados en el PRS
	private static void expand(int t) {
		// TODO Auto-generated method stub	
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
			case "-indexin":
				indexInPath = args[i + 1];
				System.out.println("Opening index in ---> " + indexInPath);
				i++;
				break;
			case "-prs":
				s = args[i + 1];
				System.out.println("++ Pseudo Relevant Set PRS se construye con los primeros " + s + " documentos del ranking");
				i++;
				break;
			case "-exp":
				option = args[i];
				t = args[i + 1];
				System.out.println("++ Expande la query original con los " + t + " mejores términos encontrados en el PRS");
				i++;
				break;
			case "-cut":
				n = args[i + 1];
				System.out.println("++ El corte en el ranking para el computo del MAP es de " + n);
				i++;
				break;
			case "-top":
				m = args[i + 1];
				System.out.println("++ Se van a mostrar los top " + m + " de documentos del ranking");
				i++;
				break;
			case "-query":
				option = args[i];
				query = args[i + 1];
				System.out.println("++ Se busca y hace PRF sobre la query " + query + " del archivo query.text");
				i++;
				break;
			}
		}

		validateArgs();

		Date start = new Date();

		switch (option) {
			case "-exp":
				expand(Integer.parseInt(t));
			break;
		}

		Date end = new Date();

		System.out.println("\nTotal Pseudo-Relevance-Feedback time : " + (end.getTime() - start.getTime()) + " milliseconds");
	}
}
