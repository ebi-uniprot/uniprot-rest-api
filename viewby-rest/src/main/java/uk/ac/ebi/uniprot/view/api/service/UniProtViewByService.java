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
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.view.api.model.ViewBy;

//@Service
public class UniProtViewByService {
	private SolrClient solrClient;
	private String uniprotCollection;
	
	
	public UniProtViewByService(SolrClient solrClient, String uniprotCollection) {
		this.solrClient = solrClient;
		this.uniprotCollection = uniprotCollection;
	}

	public List<ViewBy> getEC(String queryStr, String parent) {
		SolrQuery query = new SolrQuery(queryStr);
		StringBuilder regEx = new StringBuilder();
		String regExPostfix="[0-9]+";
	
		if (!Strings.isNullOrEmpty(parent)) {
			String[] tokens = parent.split("\\.");
			for (String token : tokens) {
				regEx.append(token).append("\\.");
			}	
		}
		regEx.append(regExPostfix);
		query.setFacet(true);
		query.add(FacetParams.FACET_FIELD, "ec");
		query.add(FacetParams.FACET_MATCHES, regEx.toString());
		try {
			QueryResponse response = solrClient.query(uniprotCollection, query);
			List<FacetField> fflist = response.getFacetFields();
			if (fflist.isEmpty()) {
				return Collections.emptyList();
			} else {
				FacetField ff = fflist.get(0);
				List<FacetField.Count> counts = ff.getValues();
				return counts.stream().map(this::convertEc).collect(Collectors.toList());
			}
		} catch (SolrServerException | IOException e) {
			throw new UniProtViewByServiceException(e);
		}
	}

	private ViewBy convertEc(FacetField.Count count) {
		ViewBy viewBy = new ViewBy();
		viewBy.setId(count.getName());
		viewBy.setCount(count.getCount());
		return viewBy;
	}

}
