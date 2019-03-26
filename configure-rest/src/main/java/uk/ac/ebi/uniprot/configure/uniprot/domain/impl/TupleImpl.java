package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import lombok.Data;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Tuple;

@Data
public class TupleImpl implements Tuple {

	private String name;
	private String value;

	public TupleImpl() {

	}

	public TupleImpl(String name, String value) {
		this.name = name;
		this.value = value;
	}

}
