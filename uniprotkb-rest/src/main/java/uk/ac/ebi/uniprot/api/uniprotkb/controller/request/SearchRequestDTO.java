package uk.ac.ebi.uniprot.api.uniprotkb.controller.request;

import lombok.Data;
import uk.ac.ebi.uniprot.api.rest.validation.*;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.validation.validator.impl.UniprotReturnFieldsValidator;
import uk.ac.ebi.uniprot.api.uniprotkb.validation.validator.impl.UniprotSolrQueryFieldValidator;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.search.field.UniProtField;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    @ValidFacets(facetConfig = UniprotFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size = DEFAULT_RESULTS_SIZE;

    @Pattern(regexp = "true|false", flags = {Pattern.Flag.CASE_INSENSITIVE}, message ="{search.invalid.matchedFields}")
    private String showMatchedFields;

    public boolean isIncludeIsoform(){
        return Boolean.valueOf(includeIsoform);
    }

    public boolean isShowMatchedFields() {
        return Boolean.valueOf(showMatchedFields);
    }

    public List<String> getFacetList(){
        if(hasFacets()){
            return Arrays.asList(facets.split(("\\s*,\\s*")));
        }else{
            return Collections.emptyList();
        }
    }

    public boolean hasFacets() {
        return Utils.notEmpty(facets);
    }

}
