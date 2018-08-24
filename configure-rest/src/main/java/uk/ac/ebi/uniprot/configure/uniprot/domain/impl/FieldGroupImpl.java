package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.util.ArrayList;
import java.util.List;

import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.configure.uniprot.domain.FieldGroup;

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

	@Override
	public String getGroupName() {
		return groupName;
	}

	@Override
	public List<Field> getFields() {
		return fields;
	}

	
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public void setFields(List<FieldImpl> fields) {
		this.fields = new ArrayList<>();
		this.fields.addAll(fields);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
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
		FieldGroupImpl other = (FieldGroupImpl) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(groupName).append("\n");
		this.fields.forEach(val -> sb.append("  ").append(val.toString()).append("\n"));
		return sb.toString();
	}

}
