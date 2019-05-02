package uk.ac.ebi.uniprot.api.rest.request;

import org.springframework.data.solr.core.query.SimpleQuery;

/**
 *
 * @author jluo
 * @date: 26 Apr 2019
 *
*/

public interface SearchRequest {
	String getQuery();
	String getReturnFields();
	String getSort();
	String getCursor();
	int getPageSize();
	SimpleQuery toQuery();
}

