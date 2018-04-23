package es.udc.fi.ri.mri_indexer;

import java.util.Comparator;

public class Comparator_TfIdf implements Comparator<ListTermInfo> {

	 @Override
	 public int compare(ListTermInfo a, ListTermInfo b) {
		 return (int) Math.signum(b.getTfIdf() - a.getTfIdf());
	 }
}
