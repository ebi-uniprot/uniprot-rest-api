package org.uniprot.api.configure.uniprot.domain.impl;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.configure.uniprot.domain.Field;
import org.uniprot.api.configure.uniprot.domain.FieldGroup;

@Data
public class FieldGroupImpl implements FieldGroup {
	private String groupName;
	private boolean isIsDatabase;
	private List<Field> fields = new ArrayList<>();
	
	public FieldGroupImpl() {
		
	}

	public FieldGroupImpl(String groupName, List<Field> fields) {
		this(groupName, false, fields);
	}

	public FieldGroupImpl(String groupName, boolean isDatabase, List<Field> fields) {
		super();
		this.groupName = groupName;
		this.isIsDatabase = isDatabase;
		this.fields =  new ArrayList<>(fields);;
		
	}

	
}
