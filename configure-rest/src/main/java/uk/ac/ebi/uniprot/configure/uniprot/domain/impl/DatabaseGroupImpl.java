package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import lombok.Data;
import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Tuple;

import java.util.ArrayList;
import java.util.List;

@Data
public class DatabaseGroupImpl implements DatabaseGroup {
	private final String groupName;
	private final List<Tuple> items;

	public DatabaseGroupImpl(String groupName, List<Tuple> items) {
		this.groupName = groupName;
		this.items = new ArrayList<>(items);
	}
}
