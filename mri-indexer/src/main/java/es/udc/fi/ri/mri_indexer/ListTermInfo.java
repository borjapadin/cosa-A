package es.udc.fi.ri.mri_indexer;

import java.util.LinkedList;

public class ListTermInfo {
	
	private String term;
	private int docId;
	private String pathSgm;
	private double tf;
	private LinkedList<Integer> positions;
	private double df;
	private String newId;
	private String oldId;
	private double tfIdf;
	private String title;
	
	public ListTermInfo(String term, int docId, String pathSgm, double tf, LinkedList<Integer> positions, double df,
			String newId, String oldId, double tfIdf, String title) {
		super();
		this.term = term;
		this.docId = docId;
		this.pathSgm = pathSgm;
		this.tf = tf;
		this.positions = positions;
		this.df = df;
		this.newId = newId;
		this.oldId = oldId;
		this.tfIdf = tfIdf;
		this.title = title;
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

	public LinkedList<Integer> getPositions() {
		return positions;
	}

	public void setPositions(LinkedList<Integer> positions) {
		this.positions = positions;
	}

	public double getDf() {
		return df;
	}

	public void setDf(double df) {
		this.df = df;
	}

	public String getNewId() {
		return newId;
	}

	public void setNewId(String newId) {
		this.newId = newId;
	}

	public String getOldId() {
		return oldId;
	}

	public void setOldId(String oldId) {
		this.oldId = oldId;
	}
	
	public double getTfIdf() {
		return tfIdf;
	}

	public void setTfIdf(double tfIdf) {
		this.tfIdf = tfIdf;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	
}
