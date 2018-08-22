package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;

public class FieldImpl implements Field {
	private String label;
	private String name;
	public FieldImpl() {
		
	}
	public FieldImpl(String label, String name) {
		this.label = label;
		this.name = name;
	}
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public String getName() {
		return name;
	}
	
	
	
	public void setLabel(String label) {
		this.label = label;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		FieldImpl other = (FieldImpl) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return label + ": " + name;
	}

}
