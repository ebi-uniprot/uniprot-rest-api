package uk.ac.ebi.uniprot.configure.uniprot.domain;
import com.fasterxml.jackson.annotation.JsonValue;
//@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SearchDataType {
	STRING("string"),
	INTEGER("integer"),
	DATE("date"),
	ENUM("enum"),
	UNKNOWN("");
	private  final String name;
	SearchDataType(String name){
		this.name =name;
	}
	
	 @JsonValue
	public String getName() {
		return name;
	}

}
