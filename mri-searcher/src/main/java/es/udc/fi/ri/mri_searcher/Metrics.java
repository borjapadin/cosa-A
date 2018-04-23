package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;


public class Metrics {
	
	private static String queriesRel= "C:\\Users\\Uxia\\Desktop\\RI\\P2\\cacm\\qrels.text";
	
	private static List<Integer> relevantDocs(int num) throws IOException {
		InputStream stream = Files.newInputStream(Paths.get(queriesRel));
		List<Integer> rDocs = new ArrayList<>();
		String str = IOUtils.toString(stream, "UTF-8");
		StringBuffer strBuffer = new StringBuffer(str);
		String text = strBuffer.toString();
		String[] lines = text.split("\n");
		String numStr = Integer.toString(num);

		int i = 0;
		while (i < lines.length) {
			while (lines[i].startsWith(numStr.concat(" "))) {
				String[] numbers = lines[i++].split(" ");
				rDocs.add(Integer.parseInt(numbers[1]));
				if (i >= lines.length)
					return rDocs;
			}
			i++;
		}
		return rDocs;
	}
	
	
	private static boolean isRelevant(List<Integer> relevants, Document doc) {
		int id = Integer.parseInt(doc.getField("I").stringValue().trim());
		return relevants.contains(id);
	}
	
	
	public static double pn(TopDocs topDocs, List<Integer> relevants, DirectoryReader indexReader, Query query,
			int queryId, int n) throws IOException {

		Document doc = null;
		double count = 0;
		for (int k = 0; k < Math.min(n, topDocs.scoreDocs.length); k++) {
			doc = indexReader.document(topDocs.scoreDocs[k].doc);
			if (isRelevant(relevants, doc)) {
				count++;
			}
		}
		return count / (double) n;

	}

	public static double recalln(TopDocs topDocs, List<Integer> relevants, DirectoryReader indexReader, Query query,
			int queryId, int n) throws IOException {

		double count = 0;
		for (int k = 0; k < Math.min(n, topDocs.scoreDocs.length); k++) {
			Document doc = indexReader.document(topDocs.scoreDocs[k].doc);

			if (isRelevant(relevants, doc)) {
				count++;
			}
		}
		return count / (double) relevants.size();

	}

	public static double ap(int n, DirectoryReader indexReader, IndexSearcher indexSearcher, Query query, int queryId)
			throws IOException {

		TopDocs topDocs = null;
		topDocs = indexSearcher.search(query, n);
		List<Integer> relevants = relevantDocs(queryId);

		double relevantCount = 0;
		double precisionCount = 0;
		for (int k = 0; k < topDocs.scoreDocs.length; k++) {
			Document doc = indexReader.document(topDocs.scoreDocs[k].doc);
			if (isRelevant(relevants, doc)) {
				relevantCount++;
				precisionCount += relevantCount / (k + 1);
			}
		}
		return relevantCount == 0 ? 0 : (precisionCount / relevants.size());

	}

}
