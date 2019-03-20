package uk.ac.ebi.uniprot.view.api.service;

import java.util.Collections;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.uniprot.view.api.model.Taxonomies;
import uk.ac.ebi.uniprot.view.api.model.TaxonomyNode;

public class TaxonomyRestClient {
	private final RestTemplate restTemplate;
	private static final String URL_PREFIX="https://www.ebi.ac.uk/proteins/api/taxonomy/id/";
	public TaxonomyRestClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	public List<TaxonomyNode> getChildren(long taxId){
		String url = URL_PREFIX + taxId +"/children";
		Taxonomies taxonomies =restTemplate.getForObject(url, Taxonomies.class);
		if(taxonomies ==null) {
			return Collections.emptyList();
		}else
			return taxonomies.getTaxonomies();
		
		
	}
}
