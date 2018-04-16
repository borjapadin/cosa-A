package es.udc.fi.ri.mri_indexer;

public class TuplaTermIdf {
	private String term;
	private double idf;
	

	public TuplaTermIdf(String term, double idf) {
		this.term = term;
		this.idf = idf;
	}
	
	public String getTerm() {
		return term;
	}

	public double getIdf() {
		return idf;
	}

	void setTerm(String term) {
		this.term = term;
	}

	public void setIdf(double idf) {
		this.idf = idf;
	}
}
