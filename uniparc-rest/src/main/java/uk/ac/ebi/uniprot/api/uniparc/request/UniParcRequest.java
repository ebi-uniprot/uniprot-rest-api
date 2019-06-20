package uk.ac.ebi.uniprot.api.uniparc.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.ValidFacets;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.api.uniparc.repository.UniParcFacetConfig;
import uk.ac.ebi.uniprot.search.field.UniParcField;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
*/
@Data
public class UniParcRequest implements SearchRequest {
	 private static final int DEFAULT_RESULTS_SIZE = 25;

	    @NotNull(message = "{search.required}")
	    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
	    @ValidSolrQueryFields(fieldValidatorClazz = UniParcSolrQueryFieldValidator.class)
	    private String query;

	    @ValidSolrSortFields(sortFieldEnumClazz = UniParcField.Sort.class)
	    private String sort;

	    private String cursor;
	    
	    @ValidReturnFields(fieldValidatorClazz = UniParcReturnFieldsValidator.class)
	    private String fields;

	    @ValidFacets(facetConfig = UniParcFacetConfig.class)
	    private String facets;

	    @Positive(message = "{search.positive}")
	    private int size = DEFAULT_RESULTS_SIZE;


}

