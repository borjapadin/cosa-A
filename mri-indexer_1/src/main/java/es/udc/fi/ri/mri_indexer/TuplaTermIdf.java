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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TuplaTermIdf other = (TuplaTermIdf) obj;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
	}

	@Override
	public String toString (){
        return term + "\tidf="+ idf;
    }
}
