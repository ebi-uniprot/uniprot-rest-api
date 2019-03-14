package uk.ac.ebi.uniprot.view.api.model;

import lombok.Data;

@Data
public final class ViewBy {
	private String id;
	private String label;
	private String link;
	private boolean expand;
	private long count;
	
}
