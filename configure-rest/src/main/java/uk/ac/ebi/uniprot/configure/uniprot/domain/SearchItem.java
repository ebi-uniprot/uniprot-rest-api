package uk.ac.ebi.uniprot.configure.uniprot.domain;

import java.util.List;

public interface SearchItem {
	String getLabel();
	SearchItemType getItemType();
	String getTerm();
	SearchDataType getDataType();
	String getDescription();
	List<Tuple> getValues();
	List<SearchItem> getItems();
	Boolean isHasRange();
	Boolean isHasEvidence();
	String getAutoComplete();
	String getExample();
}
