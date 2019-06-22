package uk.ac.ebi.uniprot.api.proteome.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.google.common.base.Strings;

import lombok.Data;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeFacetConfig;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.ValidFacets;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.search.field.ProteomeField;

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
	    @ValidSolrQueryFields(fieldValidatorClazz = ProteomeSolrQueryFieldValidator.class)
	    private String query;

	    @ValidSolrSortFields(sortFieldEnumClazz = ProteomeField.Sort.class)
	    private String sort;

	    private String cursor;
	    
	    @ValidReturnFields(fieldValidatorClazz = ProteomeReturnFieldsValidator.class)
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

