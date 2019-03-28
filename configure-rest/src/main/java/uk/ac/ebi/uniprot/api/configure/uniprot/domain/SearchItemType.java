package uk.ac.ebi.uniprot.api.configure.uniprot.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchItemType {
	SINGLE("single"),
	GROUP("group"),
	GROUP_DISPLAY("groupDisplay"),
	COMMENT("comment"),
	FEATURE("feature"),
	DATABASE("database"),
	GOTERM("goterm");
	private final String name;
	SearchItemType(String name){
		this.name =name;
	}
	 @JsonValue
	public String getName() {
		return name;
	}
	
}
