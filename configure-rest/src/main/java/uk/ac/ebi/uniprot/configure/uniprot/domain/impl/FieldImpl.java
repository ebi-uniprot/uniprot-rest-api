package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import lombok.Data;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;

@Data
public class FieldImpl implements Field {
	private String label;
	private String name;
	public FieldImpl() {
		
	}
	public FieldImpl(String label, String name) {
		this.label = label;
		this.name = name;
	}
}
