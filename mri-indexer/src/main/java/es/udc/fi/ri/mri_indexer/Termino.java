package es.udc.fi.ri.mri_indexer;

import org.apache.lucene.index.PostingsEnum;

public class Termino {
	
	private String term;
	private int docId;
	private String pathSgm;
	//private String oldId;
	//private String newId;
	private double tf;
	private PostingsEnum positions; 
	private double df;
		
	
	public Termino(String term, int docId, String pathSgm, double tf, PostingsEnum positions, double df) {
		this.term = term;
		this.docId = docId;
		this.pathSgm = pathSgm;
		//this.oldId = oldId;
		//this.newId = newId;
		this.tf = tf;
		this.positions = positions;
		this.df = df;
	}
	
	
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
	public int getDocId() {
		return docId;
	}
	public void setDocId(int docId) {
		this.docId = docId;
	}
	public String getPathSgm() {
		return pathSgm;
	}
	public void setPathSgm(String pathSgm) {
		this.pathSgm = pathSgm;
	}

	public double getTf() {
		return tf;
	}
	
	public void setTf(double tf) {
		this.tf = tf;
	}
	
	public PostingsEnum getPositions() {
		return positions;
	}
	
	public void setPositions(PostingsEnum positions) {
		this.positions = positions;
	}
	
	public double getDf() {
		return df;
	}
	
	public void setDf(double df) {
		this.df = df;
	}
	

	

}
