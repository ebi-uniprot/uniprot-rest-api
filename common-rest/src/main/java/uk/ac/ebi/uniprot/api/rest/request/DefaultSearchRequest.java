package uk.ac.ebi.uniprot.api.rest.request;

import org.springframework.data.solr.core.query.SimpleQuery;

/**
 *
 * @author jluo
 * @date: 26 Apr 2019
 *
*/

public class DefaultSearchRequest implements SearchRequest {
	   private static final int DEFAULT_RESULTS_SIZE = 25;
	private String query;
	private String returnFields;
	private String sort;
	private String cursor;
	private int pageSize = DEFAULT_RESULTS_SIZE;
	@Override
	public String getQuery() {
		return query;
	}

	@Override
	public String getReturnFields() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSort() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCursor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPageSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SimpleQuery toQuery() {
		// TODO Auto-generated method stub
		return null;
	}

}

