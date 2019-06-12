package uk.ac.ebi.uniprot.api.proteome.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeFacetConfig;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.ValidFacets;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.common.Utils;
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

}

