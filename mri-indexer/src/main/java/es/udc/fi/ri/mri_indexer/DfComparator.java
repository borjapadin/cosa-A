package es.udc.fi.ri.mri_indexer;

import java.util.Comparator;

public class DfComparator implements Comparator <Termino> {

	@Override
	public int compare(Termino a, Termino b) {
		 return (int) Math.signum(b.getDf() - a.getDf());
	}

}
