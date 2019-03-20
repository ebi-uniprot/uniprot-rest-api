package uk.ac.ebi.uniprot.view.api.model;

import java.util.List;

import lombok.Data;

@Data
public class Taxonomies {
	  private List<TaxonomyNode> taxonomies;

	    private PageInformation pageInfo;
}
