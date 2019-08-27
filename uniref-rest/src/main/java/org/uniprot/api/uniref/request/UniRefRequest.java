package org.uniprot.api.uniref.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.store.search.field.UniRefField;
import org.uniprot.store.search.field.UniRefResultFields;

import com.google.common.base.Strings;

import lombok.Data;

/**
 *
 * @author jluo
 * @date: 20 Aug 2019
 *
*/
@Data
public class UniRefRequest implements SearchRequest {

	 private static final int DEFAULT_RESULTS_SIZE = 25;

	    @NotNull(message = "{search.required}")
	    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
	    @ValidSolrQueryFields(fieldValidatorClazz = UniRefField.Search.class, messagePrefix = "search.uniref")
	    private String query;

	    @ValidSolrSortFields(sortFieldEnumClazz = UniRefField.Sort.class)
	    private String sort;

	    private String cursor;
	    
	    @ValidReturnFields(fieldValidatorClazz = UniRefResultFields.class)
	    private String fields;

	    @ValidFacets(facetConfig = UniRefFacetConfig.class)
	    private String facets;

	    @Positive(message = "{search.positive}")
	    private int size = DEFAULT_RESULTS_SIZE;

	    public static final String DEFAULT_FIELDS="id,name,common_taxon,count,created";
	    @Override
	    public String getFields() {
	    	if(Strings.isNullOrEmpty(fields)) {
	    		fields =DEFAULT_FIELDS;
	    	}else if(!fields.contains(UniRefField.Return.id.name())) {
	    		String temp = "id,"+fields;
	    		this.fields= temp;
	    	}
	    	return fields;
	    }

}

