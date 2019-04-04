package uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl;

import lombok.Data;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.FieldGroup;

import java.util.ArrayList;
import java.util.List;

@Data
public class FieldGroupImpl implements FieldGroup {
	private String groupName;
	private List<Field> fields = new ArrayList<>();
	public FieldGroupImpl() {
		
	}

	public FieldGroupImpl(String groupName, List<Field> fields) {
		super();
		this.groupName = groupName;
		this.fields =  new ArrayList<>(fields);;
	}

}
