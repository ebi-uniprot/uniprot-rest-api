package org.uniprot.api.configure.uniprot.domain.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.configure.uniprot.domain.SearchDataType;
import org.uniprot.api.configure.uniprot.domain.SearchItem;
import org.uniprot.api.configure.uniprot.domain.SearchItemType;
import org.uniprot.api.configure.uniprot.domain.Tuple;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UniProtSearchItem implements SearchItem {

	private String id;
	private String label;
	private SearchItemType itemType = SearchItemType.SINGLE;
	private String term;
	private SearchDataType dataType = SearchDataType.STRING;

	private List<Tuple> values;
	private String valuePrefix;
	private List<SearchItem> items;
	private Boolean hasRange;
	private Boolean hasEvidence;
	private String autoComplete;
	private String description;
	private String example;

	public Boolean isHasRange() {
		return hasRange;
	}

	public Boolean isHasEvidence() {
		return hasEvidence;
	}

	public void setItems(List<UniProtSearchItem> items) {
		this.items = new ArrayList<>();
		this.items.addAll(items);
	}
}
