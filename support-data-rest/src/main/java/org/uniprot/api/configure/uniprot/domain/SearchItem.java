package org.uniprot.api.configure.uniprot.domain;

import java.util.List;

public interface SearchItem {
	String getId();
	String getLabel();
	SearchItemType getItemType();
	String getTerm();
	SearchDataType getDataType();
	String getDescription();
	String getValuePrefix();
	List<Tuple> getValues();
	List<SearchItem> getItems();
	Boolean isHasRange();
	Boolean isHasEvidence();
	String getAutoComplete();
	String getExample();
}
