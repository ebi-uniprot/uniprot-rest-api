package uk.ac.ebi.uniprot.api.proteome.request;

import com.google.common.base.Strings;
import lombok.Data;
import uk.ac.ebi.uniprot.api.configure.proteome.ProteomeResultFields;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeFacetConfig;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.*;
import uk.ac.ebi.uniprot.search.field.ProteomeField;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 *
 * @author jluo
 * @date: 26 Apr 2019
 *
*/
@Data
public class ProteomeRequest implements SearchRequest{
	  private static final int DEFAULT_RESULTS_SIZE = 25;

	    @NotNull(message = "{search.required}")
	    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
		@ValidSolrQueryFields(fieldValidatorClazz = ProteomeField.Search.class, messagePrefix = "search.proteome")
	    private String query;

	    @ValidSolrSortFields(sortFieldEnumClazz = ProteomeField.Sort.class)
	    private String sort;

	    private String cursor;

	    @ValidReturnFields(fieldValidatorClazz = ProteomeResultFields.class)
	    private String fields;

	    @ValidFacets(facetConfig = ProteomeFacetConfig.class)
	    private String facets;

	    @Positive(message = "{search.positive}")
	    private int size = DEFAULT_RESULTS_SIZE;
	    
	    public static final String DEFAULT_FIELDS="upid,organism,organism_id,protein_count";
	    @Override
	    public String getFields() {
	    	if(Strings.isNullOrEmpty(fields)) {
	    		fields =DEFAULT_FIELDS;
	    	}else if(!fields.contains(ProteomeField.Return.upid.name())) {
	    		String temp = "upid,"+fields;
	    		this.fields= temp;
	    	}
	    	return fields;
	    }
}

