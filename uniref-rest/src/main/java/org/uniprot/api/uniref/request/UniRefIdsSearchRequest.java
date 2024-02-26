package org.uniprot.api.uniref.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 18/03/2021
 */
@Data
@ParameterObject
public class UniRefIdsSearchRequest implements IdsSearchRequest {

    @NotNull(message = "{search.required}")
    @Parameter(description = IDS_UNIREF_DESCRIPTION, example = IDS_UNIREF_EXAMPLE)
    @ValidUniqueIdList(uniProtDataType = UniProtDataType.UNIREF)
    private String ids;

    @Parameter(description = FIELDS_UNIREF_DESCRIPTION, example = FIELDS_UNIREF_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    private String fields;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniRefFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    @Parameter(description = QUERY_UNIREF_DESCRIPTION, example = QUERY_UNIREF_EXAMPLE)
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.UNIREF, messagePrefix = "search.uniref")
    private String query;

    @Parameter(description = DOWNLOAD_DESCRIPTION)
    @Pattern(
            regexp = "^(?:true|false)$",
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.uniref.invalid.download}")
    private String download;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_UNIREF_ID_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(description = SORT_UNIREF_DESCRIPTION, example = SORT_UNIREF_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIREF)
    private String sort;

    @Parameter(hidden = true)
    private String format;

    public String getCommaSeparatedIds() {
        return this.ids;
    }

    @Override
    public List<String> getIdList() {
        return List.of(getCommaSeparatedIds().split(",")).stream()
                .map(String::strip)
                .collect(Collectors.toList());
    }
}
