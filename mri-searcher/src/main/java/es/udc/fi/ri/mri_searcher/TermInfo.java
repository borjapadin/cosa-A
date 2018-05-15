package es.udc.fi.ri.mri_searcher;

public class TermInfo {

	private String term;
	private double idf;
	private double tf;
	private double score;
	private double value;
	private int docId;
	
	public TermInfo(int docId, String term, double idf, double tf, double score, double value) {
		super();
		this.term = term;
		this.idf = idf;
		this.tf = tf;
		this.score = score;
		this.value = value;
		this.docId = docId;
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
	
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}
}
