package es.udc.fi.ri.mri_indexer;

import java.util.Comparator;

public class Comparator_NameTerm implements Comparator <ListTermInfo> {

	@Override
	public int compare(ListTermInfo a, ListTermInfo b) {
		return a.getTerm().compareTo(b.getTerm());
	}
}
