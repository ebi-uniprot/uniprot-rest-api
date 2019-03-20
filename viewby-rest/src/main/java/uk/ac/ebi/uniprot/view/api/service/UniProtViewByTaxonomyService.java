package uk.ac.ebi.uniprot.view.api.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.uniprot.view.api.model.Taxonomies;
import uk.ac.ebi.uniprot.view.api.model.TaxonomyNode;
import uk.ac.ebi.uniprot.view.api.model.ViewBy;

public class UniProtViewByTaxonomyService implements  UniProtViewByService {
	private final SolrClient solrClient;
	private final String uniprotCollection;
	private final RestTemplate restTemplate;
	private final static String URL_PREFIX ="https://www.uniprot.org/taxonomy/";
	private static final String TAXONOMY_API_PREFIX="https://www.ebi.ac.uk/proteins/api/taxonomy/id/";
	
	public UniProtViewByTaxonomyService(SolrClient solrClient, String uniprotCollection, RestTemplate restTemplate) {
		this.solrClient = solrClient;
		this.uniprotCollection = uniprotCollection;
		this.restTemplate =restTemplate;
	}

	@Override
	public List<ViewBy> get(String queryStr, String parent) {
		String validTaxId = parent;
		if((validTaxId ==null) ||validTaxId.isEmpty()){
			validTaxId = "1";
		}
		List<TaxonomyNode>  nodes = getChildren(validTaxId);
		if(nodes.isEmpty()) {
			return Collections.emptyList();
		}

		Map<Long, TaxonomyNode> taxIdMap = 
				nodes.stream().collect(Collectors.toMap(TaxonomyNode::getTaxonomyId, Function.identity()));
		
		String facetIterms=
		nodes.stream().map(val -> "" +val.getTaxonomyId())
		.collect(Collectors.joining(","));
		SolrQuery query = new SolrQuery(queryStr);
		String facetField ="{!terms='" + facetIterms +"'}taxonomy_id";
		query.setFacet(true);
		query.addFacetField(facetField);
	
		try {
			QueryResponse response = solrClient.query(uniprotCollection, query);
			List<FacetField> fflist = response.getFacetFields();
			if (fflist.isEmpty()) {
				return Collections.emptyList();
			} else {
				FacetField ff = fflist.get(0);
				List<FacetField.Count> counts = ff.getValues();
				return counts.stream().map(val ->convert(val, taxIdMap))
						.filter(val -> val !=null)			
						.sorted(ViewBy.SORT_BY_LABEL)
						.collect(Collectors.toList());
			}
		} catch (SolrServerException | IOException e) {
			throw new UniProtViewByServiceException(e);
		}
	}
	
	private ViewBy convert(FacetField.Count count, Map<Long, TaxonomyNode> taxIdMap) {
		if(count.getCount() ==0)
			return null;
		ViewBy viewBy = new ViewBy();
		
		viewBy.setId(count.getName());
		viewBy.setCount(count.getCount());
		TaxonomyNode node = taxIdMap.get(Long.parseLong(count.getName()));
		viewBy.setLink(URL_PREFIX + count.getName());
		if(node != null) {
			viewBy.setLabel(node.getFullName());
			viewBy.setExpand(hasChildren(node));
			
		}
		return viewBy;
	}
	
	boolean hasChildren(TaxonomyNode node) {
		return (node.getChildrenLinks() !=null) && !node.getChildrenLinks().isEmpty();
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
