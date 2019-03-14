package uk.ac.ebi.uniprot.uniprotkb.controller.request;

import lombok.Data;
import uk.ac.ebi.uniprot.rest.validation.*;
import uk.ac.ebi.uniprot.uniprotkb.configuration.UniProtField;
import uk.ac.ebi.uniprot.uniprotkb.validation.validator.impl.UniprotReturnFieldsValidator;
import uk.ac.ebi.uniprot.uniprotkb.validation.validator.impl.UniprotSolrQueryFieldValidator;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

/**
 * Search cursor request Entity
 *
 * Important: How to query isoforms:
 *     CANONICAL ONLY: it is the default behavior, you do not need to do anything.
 *          Implementation note: in the service layer we add a filter query(fq): is_isoform:false
 *     ALL: add request parameter includeIsoform=true
 *     ISOFORMS ONLY: Add in the request query parameter: is_isoform:true and also request parameter includeIsoform=true
 *
 * @author lgonzales
 */
@Data
public class SearchRequestDTO {
    private static final int DEFAULT_RESULTS_SIZE = 25;
    
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = UniprotSolrQueryFieldValidator.class)
    private String query;

    @ValidReturnFields(fieldValidatorClazz = UniprotReturnFieldsValidator.class)
    private String fields;

    @ValidSolrSortFields(sortFieldEnumClazz = UniProtField.Sort.class)
    private String sort;

    private String cursor;

    @Pattern(regexp = "true|false", flags = {Pattern.Flag.CASE_INSENSITIVE}, message ="{search.invalid.includeIsoform}")
    private String includeIsoform;

    @ValidIncludeFacets
    private String includeFacets;

    @Positive(message = "{search.positive}")
    private Integer size = DEFAULT_RESULTS_SIZE;

    public boolean isIncludeFacets(){
        return Boolean.valueOf(includeFacets);
    }

    public boolean isIncludeIsoform(){
        return Boolean.valueOf(includeIsoform);
    }
}
