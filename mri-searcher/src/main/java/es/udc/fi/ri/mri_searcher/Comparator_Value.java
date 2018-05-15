package es.udc.fi.ri.mri_searcher;

import java.util.Comparator;


public class Comparator_Value implements Comparator <TermInfo>{

	@Override
	public int compare(TermInfo a, TermInfo b) {
		 return (int) Math.signum(b.getValue() - a.getValue());
	}

}
