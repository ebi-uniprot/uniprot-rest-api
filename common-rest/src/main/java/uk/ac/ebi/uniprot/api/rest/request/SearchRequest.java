package uk.ac.ebi.uniprot.api.rest.request;

import org.springframework.data.solr.core.query.SimpleQuery;
import uk.ac.ebi.uniprot.common.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author jluo
 * @date: 26 Apr 2019
 *
*/

public interface SearchRequest {

    static final int DEFAULT_RESULTS_SIZE = 25;

	String getQuery();

	String getFields();

	String getSort();

	String getCursor();

	String getFacets();

	int getSize();

	default boolean hasFields() {
		return Utils.notEmpty(getFields());
	}

	default boolean hasSort() {
		return Utils.notEmpty(getSort());
	}

	default boolean hasCursor() {
		return Utils.notEmpty(getCursor());
	}

	default boolean hasFacets() {
		return Utils.notEmpty(getFacets());
	}

	default SimpleQuery getSimpleQuery(){
		return new SimpleQuery(getQuery());
	}

	default List<String> getFacetList(){
		if(hasFacets()) {
			return Arrays.asList(getFacets().split(("\\s*,\\s*")));
		} else {
			return Collections.emptyList();
		}
	}

}

