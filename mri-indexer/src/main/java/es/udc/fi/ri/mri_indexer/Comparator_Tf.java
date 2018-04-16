package es.udc.fi.ri.mri_indexer;

import java.util.Comparator;

public class Comparator_Tf implements Comparator<ListTermInfo>{

	@Override
	public int compare(ListTermInfo a, ListTermInfo b) {
		 return (int) Math.signum(b.getTf() - a.getTf());
	}

}
