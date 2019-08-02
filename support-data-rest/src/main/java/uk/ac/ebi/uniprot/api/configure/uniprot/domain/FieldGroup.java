package uk.ac.ebi.uniprot.api.configure.uniprot.domain;

import java.util.List;

public interface FieldGroup {
	String getGroupName();
	boolean isIsDatabase();
	List<Field> getFields();

}
