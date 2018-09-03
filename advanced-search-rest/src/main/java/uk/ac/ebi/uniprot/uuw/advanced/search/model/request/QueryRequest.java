package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class QueryRequest {
	@NotNull(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.required}")
	private String query;

	private String field;
	
	private String sort;
}
