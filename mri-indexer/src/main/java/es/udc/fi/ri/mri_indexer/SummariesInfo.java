package es.udc.fi.ri.mri_indexer;

public class SummariesInfo {

	private String term;
	private int tf;
	private double idf;
	private double tfIdf;
	
	public SummariesInfo(String term, int tf, double idf, double tfIdf) {
		super();
		this.term = term;
		this.tf = tf;
		this.idf = idf;
		this.tfIdf = tfIdf;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public double getTf() {
		return tf;
	}

	public void setTf(int tf) {
		this.tf = tf;
	}

	public double getIdf() {
		return idf;
	}

	public void setIdf(double idf) {
		this.idf = idf;
	}

	public double getTfIdf() {
		return tfIdf;
	}

	public void setTfIdf(double tfIdf) {
		this.tfIdf = tfIdf;
	}
	
	@Override
	public String toString() {
		return "SummariesInfo [term=" + term + ", tf=" + tf + ", idf=" + idf + ", tfIdf=" + tfIdf + "]\n";
	}
}
