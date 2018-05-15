package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;


public class Metrics {   

	//número de relevantes recuperados en las primeras n posiciones del ranking / n
	public static double pn(TopDocs topDocs, List<Integer> relevants, DirectoryReader indexReader,
			int queryId, int n) throws IOException { 
		Document doc = null;
		double count = 0;
		for (int k = 0; k < Math.min(n, topDocs.scoreDocs.length); k++) {
			doc = indexReader.document(topDocs.scoreDocs[k].doc);
			if (CACMEval.isRelevant(relevants, doc))
				count++;
		}
		return count / (double) n;
	}

	//número de relevantes recuperados en las primeras n posiciones del ranking / total de relevantes para la query
	public static double recalln(TopDocs topDocs, List<Integer> relevants, DirectoryReader indexReader,
			int queryId, int n) throws IOException {
		Document doc = null;
		double count = 0;	
		for (int k = 0; k < Math.min(n, topDocs.scoreDocs.length); k++) {
			doc = indexReader.document(topDocs.scoreDocs[k].doc);
			if (CACMEval.isRelevant(relevants, doc)) 
				count++;
		}
		return count / (double) relevants.size();
	}

	
	// sumando las precisiones de cada doc relevante / número de relevantes por query
	public static double ap(int n, DirectoryReader indexReader, IndexSearcher indexSearcher, Query query, int queryId)
			throws IOException {
		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, n);
		List<Integer> relevants = CACMEval.relevantDocs(queryId);
		double relevantCount = 0;
		double precisionCount = 0;
		
		//se computa calculando la precisión cada vez que aparece documento relevante en el ranking
		for (int k = 0; k < topDocs.scoreDocs.length; k++) {
			Document doc = indexReader.document(topDocs.scoreDocs[k].doc);
			if (CACMEval.isRelevant(relevants, doc)) {
				relevantCount++;
				precisionCount += relevantCount / (k + 1);
			}
		}
		//numero de relevante por query
		return relevantCount == 0 ? 0 : (precisionCount / relevants.size());
	}
}
