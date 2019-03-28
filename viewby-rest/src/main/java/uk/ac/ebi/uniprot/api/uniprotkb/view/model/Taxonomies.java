package uk.ac.ebi.uniprot.api.uniprotkb.view.model;

import java.util.List;

import lombok.Data;

@Data
public class Taxonomies {
	  private List<TaxonomyNode> taxonomies;

	    private PageInformation pageInfo;
}
