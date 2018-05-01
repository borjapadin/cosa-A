package es.udc.fi.ri.mri_searcher;

public class TermInfo {

	private String term;
	private double idf;
	private double tf;
	private double value;
	
	public TermInfo(String term, double idf, double tf, double value) {
		super();
		this.term = term;
		this.idf = idf;
		this.tf = tf;
		this.value = value;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public double getIdf() {
		return idf;
	}

	public void setIdf(double idf) {
		this.idf = idf;
	}

	public double getTf() {
		return tf;
	}

	public void setTf(double tf) {
		this.tf = tf;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
}
