package org.uniprot.api.uniprotkb.controller.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import org.springframework.http.MediaType;
import org.uniprot.api.configure.uniprot.domain.impl.UniProtResultFields;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.UniProtField;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Search cursor request Entity
 * <p>
 * Important: How to query isoforms:
 * CANONICAL ONLY: it is the default behavior, you do not need to do anything.
 * Implementation note: in the service layer we add a filter query(fq): is_isoform:false
 * ALL: add request parameter includeIsoform=true
 * ISOFORMS ONLY: Add in the request query parameter: is_isoform:true and also request parameter includeIsoform=true
 *
 * @author lgonzales
 */
@Data
public class SearchRequestDTO {
    private static final int DEFAULT_RESULTS_SIZE = 25;

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = UniProtField.Search.class, messagePrefix = "search.uniprot")
    private String query;

    @ValidReturnFields(fieldValidatorClazz = UniProtResultFields.class)
    private String fields;

    @ValidSolrSortFields(sortFieldEnumClazz = UniProtField.Sort.class)
    private String sort;

    private String cursor;

    @Pattern(regexp = "true|false", flags = {
            Pattern.Flag.CASE_INSENSITIVE}, message = "{search.invalid.includeIsoform}")
    private String includeIsoform;

    @ValidFacets(facetConfig = UniprotFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size = DEFAULT_RESULTS_SIZE;

    @Pattern(regexp = "true|false", flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "{search.invalid.matchedFields}")
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String showMatchedFields;

    public boolean isIncludeIsoform() {
        return Boolean.valueOf(includeIsoform);
    }

    public boolean isShowMatchedFields() {
        return Boolean.valueOf(showMatchedFields);
    }

    public List<String> getFacetList() {
        if (hasFacets()) {
            return Arrays.asList(facets.split(("\\s*,\\s*")));
        } else {
            return Collections.emptyList();
        }
    }

    public boolean hasFacets() {
        return Utils.notEmpty(facets);
    }

}
