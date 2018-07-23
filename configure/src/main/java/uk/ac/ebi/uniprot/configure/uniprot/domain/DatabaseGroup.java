package uk.ac.ebi.uniprot.configure.uniprot.domain;

import java.util.List;

public interface DatabaseGroup {
	String getGroupName();
	List<Tuple> getItems();
}
