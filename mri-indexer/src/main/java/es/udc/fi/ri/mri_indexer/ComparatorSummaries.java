package es.udc.fi.ri.mri_indexer;

import java.util.Comparator;

public class ComparatorSummaries implements Comparator<SummariesInfo> {
	 @Override
	 public int compare(SummariesInfo a, SummariesInfo b)   {
		 return (int) Math.signum(b.getTfIdf() - a.getTfIdf());
	 }
}
