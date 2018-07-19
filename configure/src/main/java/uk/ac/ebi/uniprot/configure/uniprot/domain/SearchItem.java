package uk.ac.ebi.uniprot.configure.uniprot.domain;

import java.util.List;
import java.util.Map;

public interface SearchItem {
	String getItem();
	String getItemType();
	String getTerm();
	String getDataType();
	String getDescription();
	Map<String, String> getEnumValues();
	List<SearchItem> getItems();
	boolean isRange();
	boolean hasEvidence();
}
