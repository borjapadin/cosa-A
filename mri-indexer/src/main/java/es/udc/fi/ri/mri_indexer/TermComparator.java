package es.udc.fi.ri.mri_indexer;

import java.util.Comparator;

public class TermComparator implements Comparator <Termino> {

	@Override
	public int compare(Termino a, Termino b) {
		return a.getTerm().compareTo(b.getTerm());
	}
}
