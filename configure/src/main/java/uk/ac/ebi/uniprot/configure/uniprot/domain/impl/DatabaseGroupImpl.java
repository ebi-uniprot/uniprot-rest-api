package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.util.ArrayList;
import java.util.List;

import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Tuple;

public class DatabaseGroupImpl implements DatabaseGroup {
	private String groupName;
	private List<Tuple> items ;
	public DatabaseGroupImpl(String groupName, List<Tuple> items) {
		this.groupName = groupName;
		this.items =new ArrayList<>(items);
	}
	@Override
	public String getGroupName() {
		return groupName;
	}

	@Override
	public List<Tuple> getItems() {
		return items;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(groupName).append("\n");
		this.items.forEach(val -> sb.append("  ").append(val.toString()).append("\n"));
		return sb.toString();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((items == null) ? 0 : items.hashCode());
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
		DatabaseGroupImpl other = (DatabaseGroupImpl) obj;
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		if (items == null) {
			if (other.items != null)
				return false;
		} else if (!items.equals(other.items))
			return false;
		return true;
	}
	
}
