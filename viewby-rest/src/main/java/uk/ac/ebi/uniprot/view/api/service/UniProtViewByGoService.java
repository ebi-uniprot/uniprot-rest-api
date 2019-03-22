package uk.ac.ebi.uniprot.view.api.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;

import uk.ac.ebi.uniprot.cv.go.GoService;
import uk.ac.ebi.uniprot.cv.go.GoTerm;
import uk.ac.ebi.uniprot.view.api.model.ViewBy;

public class UniProtViewByGoService implements  UniProtViewByService {
	private final SolrClient solrClient;
	private final String uniprotCollection;
	private final  GoService goService;
	public final static String URL_PREFIX ="https://www.ebi.ac.uk/QuickGO/term/";
	private final static String GO_PREFIX= "GO:";
			
	
	public UniProtViewByGoService(SolrClient solrClient, String uniprotCollection,
			GoService goService) {
		this.solrClient = solrClient;
		this.uniprotCollection = uniprotCollection;
		this.goService = goService;
	}

	
	@Override
	public List<ViewBy> get(String queryStr, String parent) {
		
		List<String> childrens = goService.getChildrenById(parent);
		if(childrens.isEmpty())
			return Collections.emptyList();
	
		String facetIterms=
				childrens.stream().map(this::removeGoPrefix)
		.collect(Collectors.joining(","));
		SolrQuery query = new SolrQuery(queryStr);
		String facetField ="{!terms='" + facetIterms +"'}go_id";
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
				return counts.stream().map(this::convert)
						.filter(val -> val !=null)			
						.sorted(ViewBy.SORT_BY_LABEL)
						.collect(Collectors.toList());
			}
		} catch (SolrServerException | IOException e) {
			throw new UniProtViewByServiceException(e);
		}
	}
	private String removeGoPrefix(String go) {
		if((go !=null) && go.startsWith(GO_PREFIX)) {
			return go.substring(GO_PREFIX.length());
		}else
			return go;		
	}
	private String addGoPrefix(String go) {
		if((go !=null) && !go.startsWith(GO_PREFIX)) {
			return GO_PREFIX+go;
		}else
			return go;		
	}
	
	private ViewBy convert(FacetField.Count count) {
		if(count.getCount() ==0)
			return null;
		ViewBy viewBy = new ViewBy();
		String goId = addGoPrefix(count.getName());
		viewBy.setId(goId);
		viewBy.setCount(count.getCount());
		GoTerm goTerm = goService.getGoTermById(goId);
		viewBy.setLink(URL_PREFIX + goId);
		if(goTerm != null) {
			viewBy.setLabel(goTerm.getName());
		}
		viewBy.setExpand(hasChildren(goId));
		return viewBy;
	}

	private boolean hasChildren(String goId) {
		List<String> children = goService.getChildrenById(goId);
		return ((children !=null) && !children.isEmpty());
	}
}
