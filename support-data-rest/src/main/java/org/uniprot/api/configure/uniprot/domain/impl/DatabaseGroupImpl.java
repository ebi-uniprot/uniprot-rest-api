package org.uniprot.api.configure.uniprot.domain.impl;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.configure.uniprot.domain.DatabaseGroup;
import org.uniprot.api.configure.uniprot.domain.Tuple;

@Data
public class DatabaseGroupImpl implements DatabaseGroup {
	private final String groupName;
	private final List<Tuple> items;

	public DatabaseGroupImpl(String groupName, List<Tuple> items) {
		this.groupName = groupName;
		this.items = new ArrayList<>(items);
	}
}
