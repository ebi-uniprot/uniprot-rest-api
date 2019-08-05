package org.uniprot.api.configure.uniprot.domain.impl;

import org.uniprot.api.configure.uniprot.domain.Tuple;

import lombok.Data;

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
