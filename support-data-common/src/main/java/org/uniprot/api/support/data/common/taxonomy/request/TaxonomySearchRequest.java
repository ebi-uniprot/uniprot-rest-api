package org.uniprot.api.support.data.common.taxonomy.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
@ParameterObject
public class TaxonomySearchRequest extends TaxonomyBasicRequest implements SearchRequest {

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = TaxonomyFacetConfig.class)
    private String facets;

    @Parameter(description = SIZE_DESCRIPTION, example = SIZE_EXAMPLE)
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
}
