package org.uniprot.api.configure.uniprot.domain;

import java.util.List;

public interface DatabaseGroup {
	String getGroupName();
	List<Tuple> getItems();
}
