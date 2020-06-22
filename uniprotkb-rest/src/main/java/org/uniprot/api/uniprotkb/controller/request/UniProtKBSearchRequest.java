package org.uniprot.api.uniprotkb.controller.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotKBFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Search cursor request Entity
 *
 * <p>Important: How to query isoforms: CANONICAL ONLY: it is the default behavior, you do not need
 * to do anything. Implementation note: in the service layer we add a filter query(fq):
 * is_isoform:false ALL: add request parameter includeIsoform=true ISOFORMS ONLY: Add in the request
 * query parameter: is_isoform:true and also request parameter includeIsoform=true
 *
 * @author lgonzales
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniProtKBSearchRequest extends UniProtKBBasicRequest implements SearchRequest {
    @Parameter(hidden = true)
    public static final String DEFAULT_FIELDS =
            "accession,id,reviewed,protein_name,gene_names,organism,length";

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniprotKBFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    @Parameter(hidden = true)
    @Pattern(
            regexp = "true|false",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.matchedFields}")
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String showMatchedFields;

    public boolean isShowMatchedFields() {
        return Boolean.valueOf(showMatchedFields);
    }

    @Override
    public List<String> getFacetList() {
        if (hasFacets()) {
            return Arrays.asList(facets.split(("\\s*,\\s*")));
        } else {
            return Collections.emptyList();
        }
    }
}
