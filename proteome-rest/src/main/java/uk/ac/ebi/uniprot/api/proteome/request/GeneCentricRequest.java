package uk.ac.ebi.uniprot.api.proteome.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import uk.ac.ebi.uniprot.api.rest.validation.ValidFacets;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.search.field.GeneCentricField;

/**
 *
 * @author jluo
 * @date: 17 May 2019
 *
*/
@Data
public class GeneCentricRequest {
	 private static final int DEFAULT_RESULTS_SIZE = 25;

	    @NotNull(message = "{search.required}")
	    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
	    @ValidSolrQueryFields(fieldValidatorClazz = GeneCentricQueryFieldsValidator.class)
	    private String query;

	    @ValidSolrSortFields(sortFieldEnumClazz = GeneCentricField.Sort.class)
	    private String sort;

	    private String cursor;
	    
	  //  @ValidReturnFields(fieldValidatorClazz = ProteomeReturnFieldsValidator.class)
	    private String fields;

	    @ValidFacets(facetConfig = GeneCentricFacetConfig.class)
	    private String facets;

	    @Positive(message = "{search.positive}")
	    private Integer size = DEFAULT_RESULTS_SIZE;

	    public List<String> getFacetList(){
	        if(hasFacets()) {
	            return Arrays.asList(facets.split(("\\s*,\\s*")));
	        } else {
	            return Collections.emptyList();
	        }
	    }

	    public boolean hasFacets() {
	        return Utils.notEmpty(facets);
	    }

}

