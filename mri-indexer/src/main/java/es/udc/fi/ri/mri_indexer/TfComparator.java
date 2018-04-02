package es.udc.fi.ri.mri_indexer;

import java.util.Comparator;

public class TfComparator implements Comparator<Termino>{

	@Override
	public int compare(Termino a, Termino b) {
		 return (int) Math.signum(b.getTf() - a.getTf());
	}

}
