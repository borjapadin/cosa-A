package es.udc.fi.ri.mri_indexer;

public class TuplaTermIdf {
	private String term;
	private double idf;
	private int df;

	public TuplaTermIdf(String term, int df, double idf) {
		this.term = term;
		this.idf = idf;
		this.df= df;
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
	
	public int getDf() {
		return df;
	}

	public void setDf(int df) {
		this.df = df;
	}

}
