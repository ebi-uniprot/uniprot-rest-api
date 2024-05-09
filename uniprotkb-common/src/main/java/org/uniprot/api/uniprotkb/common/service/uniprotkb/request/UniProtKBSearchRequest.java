package org.uniprot.api.uniprotkb.common.service.uniprotkb.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@ParameterObject
public class UniProtKBSearchRequest extends UniProtKBBasicRequest implements SearchRequest {
    @Parameter(hidden = true)
    public static final String DEFAULT_FIELDS =
            "accession,id,reviewed,protein_name,gene_names,organism,length";

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniProtKBFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(description = SIZE_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    @Pattern(
            regexp = "true|false",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.matchedFields}")
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String showSingleTermMatchedFields;

    public boolean getShowSingleTermMatchedFields() {
        return Boolean.valueOf(showSingleTermMatchedFields);
    }

    @Override
    public List<String> getFacetList() {
        if (hasFacets()) {
            return Arrays.asList(facets.replaceAll("\\s", "").split(","));
        } else {
            return Collections.emptyList();
        }
    }
}
