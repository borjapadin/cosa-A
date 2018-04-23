package es.udc.fi.ri.mri_indexer;

import java.util.Comparator;

public class Comparator_Idf implements Comparator<TuplaTermIdf> {
	
	 @Override
	 public int compare(TuplaTermIdf a, TuplaTermIdf b) {
		 return (int) Math.signum(b.getIdf() - a.getIdf());
	 }
}
