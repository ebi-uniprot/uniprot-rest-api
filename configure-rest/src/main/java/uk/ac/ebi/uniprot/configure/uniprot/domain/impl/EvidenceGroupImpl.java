package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.util.ArrayList;
import java.util.List;

import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceItem;

public class EvidenceGroupImpl implements EvidenceGroup {
	private String groupName;
	private List<EvidenceItem> items = new ArrayList<>();

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public List<EvidenceItem> getItems() {
		return items;
	}

	public void setItems(List<EvidenceItemImpl> items) {
		this.items = new ArrayList<>();
		this.items.addAll(items);
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
		EvidenceGroupImpl other = (EvidenceGroupImpl) obj;
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

	public static class EvidenceItemImpl implements EvidenceItem {
		private String name;
		private String code;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		@Override
		public String toString() {
			return name + ": " + code;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((code == null) ? 0 : code.hashCode());
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
			EvidenceItemImpl other = (EvidenceItemImpl) obj;
			if (code == null) {
				if (other.code != null)
					return false;
			} else if (!code.equals(other.code))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

	}
}
