package org.uniprot.api.uniprotkb.view.service;

import java.util.Collections;
import java.util.List;

import org.springframework.web.client.RestTemplate;
import org.uniprot.api.uniprotkb.view.Taxonomies;
import org.uniprot.api.uniprotkb.view.TaxonomyNode;

public class TaxonomyService {
	private final RestTemplate restTemplate;
	private static final String TAXONOMY_API_PREFIX="https://www.ebi.ac.uk/proteins/api/taxonomy/id/";
	
	
	public TaxonomyService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	
	public List<TaxonomyNode> getChildren(String taxId){
		
		String url = TAXONOMY_API_PREFIX + taxId +"/children";
		Taxonomies taxonomies =restTemplate.getForObject(url, Taxonomies.class);
		if(taxonomies ==null) {
			return Collections.emptyList();
		}else
			return taxonomies.getTaxonomies();
	}
}
